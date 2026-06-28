package com.jnetaol.securemessenger

import android.app.Application
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
            } else {
                val payload = msg
                applicationScope.launch {
                    try {
                        val contact = contactRepository.getContactByIdSync(peerId)
                        if (contact != null) {
                            val message = com.jnetaol.securemessenger.data.model.Message(
                                id = java.util.UUID.randomUUID().toString(),
                                contactId = peerId,
                                content = payload,
                                originalContent = payload,
                                isEncrypted = false,
                                isFromMe = false,
                                isFile = false,
                                isRead = false,
                                timestamp = System.currentTimeMillis()
                            )
                            chatRepository.insertMessage(message)
                            DebugLogger.d("SecureMessengerApp", "handleP2PMessage", "SM-APP-P2P-004",
                                "Message received from $peerId")
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
}
