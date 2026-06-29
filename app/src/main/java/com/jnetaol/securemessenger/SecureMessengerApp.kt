package com.jnetaol.securemessenger

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jnetaol.securemessenger.data.db.AppDatabase
import com.jnetaol.securemessenger.data.repository.ChatRepository
import com.jnetaol.securemessenger.data.repository.ContactRepository
import com.jnetaol.securemessenger.data.repository.SettingsRepository
import com.jnetaol.securemessenger.logger.DebugLogger
import com.jnetaol.securemessenger.pairing.IdentityManager
import com.jnetaol.securemessenger.network.P2PManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecureMessengerApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var contactRepository: ContactRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var identityManager: IdentityManager
        private set
    var p2pManager: P2PManager? = null
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DebugLogger.i("SecureMessengerApp", "onCreate", "SM-APP-001", "App initializing")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            DebugLogger.e("SecureMessengerApp", "uncaughtException", "SM-APP-CRASH-001",
                "Thread: ${thread.name}, Error: ${throwable.message}", throwable)
            throwable.printStackTrace()
        }

        try {
            database = AppDatabase.getInstance(this)
            contactRepository = ContactRepository(database.contactDao())
            chatRepository = ChatRepository(database.chatDao(), database.messageDao())
            settingsRepository = SettingsRepository(this)
            identityManager = IdentityManager(this)
            DebugLogger.i("SecureMessengerApp", "onCreate", "SM-APP-002", "Core services initialized")
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "onCreate", "SM-APP-ERR-001", "Core init failed", e)
            throw e
        }

        try {
            p2pManager = P2PManager(
                settings = com.jnetaol.securemessenger.data.model.AppSettings(),
                onMessageReceived = { peerId, data ->
                    DebugLogger.d("SecureMessengerApp", "onMessageReceived", "SM-APP-P2P-001", "Message from $peerId")
                    handleP2PMessage(peerId, data)
                },
                onConnectionEstablished = { peerId ->
                    DebugLogger.i("SecureMessengerApp", "onConnectionEstablished", "SM-APP-P2P-002", "Connected to $peerId")
                },
                onConnectionFailed = { peerId, reason ->
                    DebugLogger.e("SecureMessengerApp", "onConnectionFailed", "SM-APP-P2P-ERR-001", "Failed $peerId: $reason")
                }
            )
            p2pManager?.start()
            DebugLogger.i("SecureMessengerApp", "onCreate", "SM-APP-003", "P2P manager initialized and started")
            applicationScope.launch {
                delay(2000)
                autoConnectToContacts()
            }
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "onCreate", "SM-APP-ERR-002", "P2P init failed (non-fatal)", e)
            p2pManager = null
        }

        DebugLogger.i("SecureMessengerApp", "onCreate", "SM-APP-004", "App initialized successfully")
    }

    override fun onTerminate() {
        DebugLogger.i("SecureMessengerApp", "onTerminate", "SM-APP-005", "App terminating")
        try {
            p2pManager?.destroy()
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "onTerminate", "SM-APP-ERR-003", "P2P destroy failed", e)
        }
        super.onTerminate()
    }

    private fun handleP2PMessage(peerId: String, data: ByteArray) {
        try {
            val msg = String(data, Charsets.UTF_8)
            if (msg.startsWith("SM_PAIR|")) {
                val parts = msg.split("|")
                if (parts.size >= 4) {
                    val remoteId = parts[1]
                    val remotePin = parts[2]
                    val remoteKey = parts[3]
                    applicationScope.launch {
                        try {
                            val existing = contactRepository.getContactByIdSync(remoteId)
                            if (existing == null) {
                                val keyPair = com.jnetaol.securemessenger.crypto.CryptoManager().generateKeyPair()
                                val contact = com.jnetaol.securemessenger.data.model.Contact(
                                    id = remoteId,
                                    displayName = "PIN: $remotePin",
                                    publicKey = remoteKey,
                                    privateKey = keyPair.privateKey,
                                    isBlocked = false,
                                    isFriend = false,
                                    createdAt = System.currentTimeMillis()
                                )
                                contactRepository.insertContact(contact)
                                val myId = identityManager.identityId
                                val myPin = identityManager.pinCode
                                val myKey = keyPair.publicKey
                                val reply = "SM_PAIR|$myId|$myPin|$myKey"
                                p2pManager?.sendMessage(peerId, reply.toByteArray(Charsets.UTF_8))
                                DebugLogger.i("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-003",
                                    "Auto-paired with $remoteId (PIN: $remotePin)")
                            } else {
                                contactRepository.updatePublicKey(remoteId, remoteKey)
                                DebugLogger.i("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-005",
                                    "Updated key for existing contact $remoteId")
                            }
                        } catch (e: Exception) {
                            DebugLogger.e("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-ERR-002",
                                "Failed to create contact from pairing", e)
                        }
                    }
                }
            } else if (msg.startsWith("SM_ACK|")) {
                val messageId = msg.removePrefix("SM_ACK|")
                applicationScope.launch {
                    try {
                        chatRepository.markMessageDelivered(messageId)
                        DebugLogger.d("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-006",
                            "ACK received for message $messageId")
                    } catch (e: Exception) {
                        DebugLogger.e("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-ERR-005",
                            "Failed to mark delivered", e)
                    }
                }
            } else {
                val payload = msg
                applicationScope.launch {
                    try {
                        val contact = contactRepository.getContactByIdSync(peerId)
                        if (contact != null) {
                            val isNudge = payload == "NUDGE"
                            val isFile = payload.startsWith("FILE|")
                            val decrypted: String
                            val isEnc: Boolean
                            val fileName: String?
                            val fileSize: Long?

                            if (isNudge) {
                                decrypted = "NUDGE"
                                isEnc = false
                                fileName = null
                                fileSize = null
                            } else if (isFile) {
                                val fileParts = payload.split("|")
                                fileName = fileParts.getOrNull(1) ?: "file"
                                fileSize = fileParts.getOrNull(2)?.toLongOrNull() ?: 0
                                val fileDataB64 = fileParts.getOrNull(3) ?: ""
                                decrypted = fileName
                                isEnc = fileDataB64.isNotEmpty()
                                if (fileDataB64.isNotEmpty()) {
                                    try {
                                        val fileBytes = java.util.Base64.getDecoder().decode(fileDataB64)
                                        val decryptedBytes = if (contact.publicKey.isNotEmpty()) {
                                            try { com.jnetaol.securemessenger.crypto.CryptoManager().decryptBytes(fileBytes, contact.privateKey) }
                                            catch (_: Exception) { fileBytes }
                                        } else { fileBytes }
                                        val storageDir = settingsRepository.getStorageDir(this@SecureMessengerApp)
                                        val savedFile = java.io.File(storageDir, fileName)
                                        savedFile.writeBytes(decryptedBytes)
                                        DebugLogger.i("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-008",
                                            "File saved: ${savedFile.absolutePath}")
                                    } catch (e: Exception) {
                                        DebugLogger.e("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-ERR-007",
                                            "Failed to save file", e)
                                    }
                                }
                            } else {
                                val dec = try {
                                    com.jnetaol.securemessenger.crypto.CryptoManager().decrypt(payload, contact.privateKey)
                                } catch (e: Exception) {
                                    DebugLogger.w("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-WARN-001",
                                        "Decrypt failed, storing raw: ${e.message}")
                                    payload
                                }
                                decrypted = dec
                                isEnc = dec != payload
                                fileName = null
                                fileSize = null
                            }

                            val message = com.jnetaol.securemessenger.data.model.Message(
                                id = java.util.UUID.randomUUID().toString(),
                                contactId = peerId,
                                content = payload,
                                originalContent = decrypted,
                                isEncrypted = isEnc,
                                isFromMe = false,
                                isFile = isFile,
                                isNudge = isNudge,
                                fileName = fileName,
                                fileSize = fileSize,
                                isRead = false,
                                isDelivered = true,
                                timestamp = System.currentTimeMillis()
                            )
                            chatRepository.insertMessage(message)
                            p2pManager?.sendMessage(peerId, "SM_ACK|${message.id}".toByteArray(Charsets.UTF_8))
                            DebugLogger.d("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-004",
                                "Message received from $peerId, ACK sent")

                            val appSettings = kotlinx.coroutines.runBlocking { settingsRepository.settings.first() }
                            if (isNudge && appSettings.vibrationEnabled) {
                                triggerVibration()
                            }
                            if (appSettings.soundEnabled) {
                                playNotificationSound()
                            }
                        }
                    } catch (e: Exception) {
                        DebugLogger.e("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-ERR-003",
                            "Failed to save incoming message", e)
                    }
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-ERR-004",
                "Message handling failed", e)
        }
    }

    private suspend fun autoConnectToContacts() {
        try {
            val contacts = contactRepository.getAllContactsSync()
            val myId = identityManager.identityId
            for (contact in contacts) {
                if (contact.isBlocked) continue
                val addr = getContactAddress(contact.id)
                if (addr != null) {
                    p2pManager?.connectToPeer(contact.id, myId, addr.first, addr.second)
                    DebugLogger.d("SecureMessengerApp", "autoConnect", "SM-APP-P2P-007",
                        "Auto-connecting to ${contact.id}")
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "autoConnect", "SM-APP-P2P-ERR-006",
                "Auto-connect failed", e)
        }
    }

    private fun getContactAddress(contactId: String): Pair<String, Int>? {
        return try {
            val p2pInfo = p2pManager?.getConnectionInfo()
            if (p2pInfo != null && p2pInfo.publicPort > 0) {
                Pair(p2pInfo.publicAddress, p2pInfo.publicPort)
            } else {
                val localPort = p2pManager?.getLocalPort() ?: 0
                if (localPort > 0) {
                    val ip = getGlobalIPv6() ?: getLocalIPv4()
                    Pair(ip, localPort)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getGlobalIPv6(): String? {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet6Address && !addr.isLoopbackAddress
                        && !addr.isLinkLocalAddress && !addr.isSiteLocalAddress) {
                        return addr.hostAddress?.split("%")?.firstOrNull()
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalIPv4(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "triggerVibration", "SM-APP-VIB-ERR-001", "Vibration failed", e)
        }
    }

    private fun playNotificationSound() {
        try {
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone.play()
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "playNotificationSound", "SM-APP-SND-ERR-001", "Sound failed", e)
        }
    }
}
