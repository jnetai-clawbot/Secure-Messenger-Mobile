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
import kotlinx.coroutines.flow.first
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
    lateinit var p2pManager: P2PManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DebugLogger.i("SecureMessengerApp", "onCreate", "SM-APP-001", "App initializing")
        try {
            database = AppDatabase.getInstance(this)
            contactRepository = ContactRepository(database.contactDao())
            chatRepository = ChatRepository(database.chatDao(), database.messageDao())
            settingsRepository = SettingsRepository(this)
            identityManager = IdentityManager(this)
            p2pManager = P2PManager(
                settings = com.jnetaol.securemessenger.data.model.AppSettings(),
                onMessageReceived = { peerId, data ->
                    DebugLogger.d("SecureMessengerApp", "onMessageReceived", "SM-APP-P2P-001", "Message from $peerId")
                },
                onConnectionEstablished = { peerId ->
                    DebugLogger.i("SecureMessengerApp", "onConnectionEstablished", "SM-APP-P2P-002", "Connected to $peerId")
                },
                onConnectionFailed = { peerId, reason ->
                    DebugLogger.e("SecureMessengerApp", "onConnectionFailed", "SM-APP-P2P-ERR-001", "Failed $peerId: $reason")
                }
            )
            DebugLogger.i("SecureMessengerApp", "onCreate", "SM-APP-002", "App initialized successfully")
        } catch (e: Exception) {
            DebugLogger.e("SecureMessengerApp", "onCreate", "SM-APP-ERR-001", "App initialization failed", e)
            throw e
        }
    }

    override fun onTerminate() {
        DebugLogger.i("SecureMessengerApp", "onTerminate", "SM-APP-003", "App terminating")
        p2pManager.destroy()
        super.onTerminate()
    }
}
