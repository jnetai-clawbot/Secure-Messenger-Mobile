package com.jnetaol.securemessenger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnetaol.securemessenger.crypto.CryptoManager
import com.jnetaol.securemessenger.data.model.Contact
import com.jnetaol.securemessenger.data.model.Message
import com.jnetaol.securemessenger.logger.DebugLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SecureMessengerApp
    private val contactRepo = app.contactRepository
    private val chatRepo = app.chatRepository
    private val settingsRepo = app.settingsRepository
    val identityManager = app.identityManager
    private val cryptoManager = CryptoManager()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    private val _selectedContactId = MutableStateFlow<String?>(null)
    val selectedContactId: StateFlow<String?> = _selectedContactId

    val contacts: StateFlow<List<Contact>> = contactRepo.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<Message>> = _selectedContactId
        .flatMapLatest { contactId ->
            if (contactId != null) chatRepo.getMessages(contactId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentContact: StateFlow<Contact?> = _selectedContactId
        .flatMapLatest { contactId ->
            if (contactId != null) contactRepo.getContactById(contactId)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.jnetaol.securemessenger.data.model.AppSettings())

    fun selectContact(contactId: String) {
        DebugLogger.d("MainViewModel", "selectContact", "SM-VM-001", "Selecting contact: $contactId")
        _selectedContactId.value = contactId
    }

    fun clearSelectedContact() {
        _selectedContactId.value = null
    }

    fun regenerateIdentity() {
        viewModelScope.launch {
            try {
                identityManager.regenerateIdentity()
                _toastMessage.emit("Identity regenerated")
                DebugLogger.i("MainViewModel", "regenerateIdentity", "SM-VM-002", "Identity regenerated")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "regenerateIdentity", "SM-VM-ERR-001", "Failed to regenerate identity", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun pairWithPin(pin: String) {
        viewModelScope.launch {
            try {
                DebugLogger.d("MainViewModel", "pairWithPin", "SM-VM-003", "Pairing with PIN: $pin")
                val contactId = UUID.randomUUID().toString()
                val keyPair = cryptoManager.generateKeyPair()
                val contact = Contact(
                    id = contactId,
                    displayName = "Contact $pin",
                    publicKey = keyPair.publicKey,
                    privateKey = keyPair.privateKey,
                    isBlocked = false,
                    isFriend = false,
                    createdAt = System.currentTimeMillis()
                )
                contactRepo.insertContact(contact)
                _toastMessage.emit("Pairing successful")
                DebugLogger.i("MainViewModel", "pairWithPin", "SM-VM-004", "Pairing successful: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "pairWithPin", "SM-VM-ERR-002", "Pairing failed", e)
                _toastMessage.emit("Pairing failed: ${e.message}")
            }
        }
    }

    fun pairWithQr(qrData: String) {
        viewModelScope.launch {
            try {
                DebugLogger.d("MainViewModel", "pairWithQr", "SM-VM-005", "Pairing with QR: $qrData")
                val contactId = UUID.randomUUID().toString()
                val keyPair = cryptoManager.generateKeyPair()
                val contact = Contact(
                    id = contactId,
                    displayName = "QR Contact",
                    publicKey = keyPair.publicKey,
                    privateKey = keyPair.privateKey,
                    isBlocked = false,
                    isFriend = false,
                    createdAt = System.currentTimeMillis()
                )
                contactRepo.insertContact(contact)
                _toastMessage.emit("Pairing successful")
                DebugLogger.i("MainViewModel", "pairWithQr", "SM-VM-006", "QR pairing successful: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "pairWithQr", "SM-VM-ERR-003", "QR pairing failed", e)
                _toastMessage.emit("Pairing failed: ${e.message}")
            }
        }
    }

    fun addFriend(contactId: String) {
        viewModelScope.launch {
            try {
                contactRepo.updateFriendStatus(contactId, true)
                _toastMessage.emit("Contact added as friend")
                DebugLogger.i("MainViewModel", "addFriend", "SM-VM-007", "Friend added: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "addFriend", "SM-VM-ERR-004", "Failed to add friend", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun sendMessage(contactId: String, text: String) {
        viewModelScope.launch {
            try {
                DebugLogger.d("MainViewModel", "sendMessage", "SM-VM-008", "Sending message to: $contactId")
                val contact = contactRepo.getContactByIdSync(contactId)
                if (contact == null) {
                    _toastMessage.emit("Contact not found")
                    return@launch
                }
                val encryptedText = cryptoManager.encrypt(text, contact.publicKey)
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    contactId = contactId,
                    content = encryptedText,
                    originalContent = text,
                    isEncrypted = true,
                    isFromMe = true,
                    isFile = false,
                    timestamp = System.currentTimeMillis()
                )
                chatRepo.insertMessage(message)
                DebugLogger.i("MainViewModel", "sendMessage", "SM-VM-009", "Message sent: ${message.id}")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "sendMessage", "SM-VM-ERR-005", "Failed to send message", e)
                _toastMessage.emit("Error sending message: ${e.message}")
            }
        }
    }

    fun sendFile(contactId: String, file: File) {
        viewModelScope.launch {
            try {
                DebugLogger.d("MainViewModel", "sendFile", "SM-VM-010", "Sending file: ${file.name}")
                val contact = contactRepo.getContactByIdSync(contactId)
                if (contact == null) {
                    _toastMessage.emit("Contact not found")
                    return@launch
                }
                val fileBytes = file.readBytes()
                val encryptedBytes = cryptoManager.encryptBytes(fileBytes, contact.publicKey)
                val storageDir = settingsRepo.getStorageDir(getApplication())
                val encryptedFile = File(storageDir, "enc_${file.name}")
                encryptedFile.writeBytes(encryptedBytes)
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    contactId = contactId,
                    content = encryptedFile.absolutePath,
                    originalContent = file.name,
                    isEncrypted = true,
                    isFromMe = true,
                    isFile = true,
                    fileName = file.name,
                    fileSize = file.length(),
                    timestamp = System.currentTimeMillis()
                )
                chatRepo.insertMessage(message)
                _toastMessage.emit("File sent")
                DebugLogger.i("MainViewModel", "sendFile", "SM-VM-011", "File sent: ${message.id}")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "sendFile", "SM-VM-ERR-006", "Failed to send file", e)
                _toastMessage.emit("Error sending file: ${e.message}")
            }
        }
    }

    fun nudgeContact(contactId: String) {
        viewModelScope.launch {
            try {
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    contactId = contactId,
                    content = "NUDGE",
                    originalContent = "NUDGE",
                    isEncrypted = false,
                    isFromMe = true,
                    isFile = false,
                    isNudge = true,
                    timestamp = System.currentTimeMillis()
                )
                chatRepo.insertMessage(message)
                _toastMessage.emit("Nudge sent!")
                DebugLogger.i("MainViewModel", "nudgeContact", "SM-VM-012", "Nudge sent to: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "nudgeContact", "SM-VM-ERR-007", "Failed to send nudge", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun blockContact(contactId: String) {
        viewModelScope.launch {
            try {
                contactRepo.updateBlockStatus(contactId, true)
                _toastMessage.emit("User blocked")
                DebugLogger.i("MainViewModel", "blockContact", "SM-VM-013", "Contact blocked: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "blockContact", "SM-VM-ERR-008", "Failed to block contact", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun endChat(contactId: String) {
        viewModelScope.launch {
            try {
                chatRepo.deleteMessages(contactId)
                contactRepo.deleteContact(contactId)
                if (_selectedContactId.value == contactId) {
                    _selectedContactId.value = null
                }
                _toastMessage.emit("Chat ended")
                DebugLogger.i("MainViewModel", "endChat", "SM-VM-014", "Chat ended: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "endChat", "SM-VM-ERR-009", "Failed to end chat", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun clearChatHistory(contactId: String) {
        viewModelScope.launch {
            try {
                chatRepo.deleteMessages(contactId)
                _toastMessage.emit("Chat history cleared")
                DebugLogger.i("MainViewModel", "clearChatHistory", "SM-VM-015", "Chat history cleared: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "clearChatHistory", "SM-VM-ERR-010", "Failed to clear chat", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun removeContact(contactId: String) {
        viewModelScope.launch {
            try {
                chatRepo.deleteMessages(contactId)
                contactRepo.deleteContact(contactId)
                if (_selectedContactId.value == contactId) {
                    _selectedContactId.value = null
                }
                _toastMessage.emit("Contact removed")
                DebugLogger.i("MainViewModel", "removeContact", "SM-VM-016", "Contact removed: $contactId")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "removeContact", "SM-VM-ERR-011", "Failed to remove contact", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun updateSettings(newSettings: com.jnetaol.securemessenger.data.model.AppSettings) {
        viewModelScope.launch {
            try {
                settingsRepo.updateSettings(newSettings)
                DebugLogger.i("MainViewModel", "updateSettings", "SM-VM-017", "Settings updated")
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "updateSettings", "SM-VM-ERR-012", "Failed to update settings", e)
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }
}
