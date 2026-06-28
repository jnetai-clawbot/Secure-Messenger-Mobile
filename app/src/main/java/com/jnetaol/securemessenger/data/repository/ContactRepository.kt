package com.jnetaol.securemessenger.data.repository

import com.jnetaol.securemessenger.data.db.ContactDao
import com.jnetaol.securemessenger.data.model.Contact
import com.jnetaol.securemessenger.logger.DebugLogger
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {
    init {
        DebugLogger.i("ContactRepository", "init", "SM-CR-001", "ContactRepository initialized")
    }

    fun getAllContacts(): Flow<List<Contact>> {
        return try {
            contactDao.getAllContacts()
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "getAllContacts", "SM-CR-ERR-001", "Failed to get contacts", e)
            throw e
        }
    }

    fun getContactById(contactId: String): Flow<Contact?> {
        return try {
            contactDao.getContactById(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "getContactById", "SM-CR-ERR-002", "Failed to get contact", e)
            throw e
        }
    }

    suspend fun getContactByIdSync(contactId: String): Contact? {
        return try {
            contactDao.getContactByIdSync(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "getContactByIdSync", "SM-CR-ERR-003", "Failed to get contact sync", e)
            throw e
        }
    }

    suspend fun insertContact(contact: Contact) {
        try {
            contactDao.insertContact(contact)
            DebugLogger.i("ContactRepository", "insertContact", "SM-CR-002", "Contact inserted: ${contact.id}")
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "insertContact", "SM-CR-ERR-004", "Failed to insert contact", e)
            throw e
        }
    }

    suspend fun updateFriendStatus(contactId: String, isFriend: Boolean) {
        try {
            contactDao.updateFriendStatus(contactId, isFriend)
            DebugLogger.i("ContactRepository", "updateFriendStatus", "SM-CR-003", "Friend status: $isFriend for $contactId")
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "updateFriendStatus", "SM-CR-ERR-005", "Failed to update friend status", e)
            throw e
        }
    }

    suspend fun updateBlockStatus(contactId: String, isBlocked: Boolean) {
        try {
            contactDao.updateBlockStatus(contactId, isBlocked)
            DebugLogger.i("ContactRepository", "updateBlockStatus", "SM-CR-004", "Block status: $isBlocked for $contactId")
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "updateBlockStatus", "SM-CR-ERR-006", "Failed to update block status", e)
            throw e
        }
    }

    suspend fun deleteContact(contactId: String) {
        try {
            contactDao.deleteContactById(contactId)
            DebugLogger.i("ContactRepository", "deleteContact", "SM-CR-005", "Contact deleted: $contactId")
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "deleteContact", "SM-CR-ERR-007", "Failed to delete contact", e)
            throw e
        }
    }

    suspend fun updateDisplayName(contactId: String, name: String) {
        try {
            contactDao.updateDisplayName(contactId, name)
            DebugLogger.i("ContactRepository", "updateDisplayName", "SM-CR-006", "Display name updated: $contactId -> $name")
        } catch (e: Exception) {
            DebugLogger.e("ContactRepository", "updateDisplayName", "SM-CR-ERR-008", "Failed to update display name", e)
            throw e
        }
    }
}
