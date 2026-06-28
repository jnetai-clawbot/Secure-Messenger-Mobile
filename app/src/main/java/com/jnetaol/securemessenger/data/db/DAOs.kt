package com.jnetaol.securemessenger.data.db

import androidx.room.*
import com.jnetaol.securemessenger.data.model.Contact
import com.jnetaol.securemessenger.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun getContactById(contactId: String): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactByIdSync(contactId: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Query("UPDATE contacts SET isFriend = :isFriend WHERE id = :contactId")
    suspend fun updateFriendStatus(contactId: String, isFriend: Boolean)

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE id = :contactId")
    suspend fun updateBlockStatus(contactId: String, isBlocked: Boolean)

    @Query("UPDATE contacts SET displayName = :name WHERE id = :contactId")
    suspend fun updateDisplayName(contactId: String, name: String)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getMessages(contactId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteMessages(contactId: String)

    @Query("DELETE FROM messages WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldMessages(beforeTimestamp: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE contactId = :contactId AND isFromMe = 0 AND isRead = 0")
    fun getUnreadCount(contactId: String): Flow<Int>

    @Query("UPDATE messages SET isRead = 1 WHERE contactId = :contactId AND isFromMe = 0 AND isRead = 0")
    suspend fun markAllRead(contactId: String)

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageSync(contactId: String): Message?
}

@Dao
interface ChatDao {
    @Query("SELECT DISTINCT contactId FROM messages ORDER BY (SELECT MAX(timestamp) FROM messages m2 WHERE m2.contactId = messages.contactId) DESC")
    fun getChatContactIds(): Flow<List<String>>

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT 1")
    fun getLastMessage(contactId: String): Flow<Message?>
}
