package com.jnetaol.securemessenger.data.repository

import com.jnetaol.securemessenger.data.db.ChatDao
import com.jnetaol.securemessenger.data.db.MessageDao
import com.jnetaol.securemessenger.data.model.Message
import com.jnetaol.securemessenger.logger.DebugLogger
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    init {
        DebugLogger.i("ChatRepository", "init", "SM-CH-001", "ChatRepository initialized")
    }

    fun getMessages(contactId: String): Flow<List<Message>> {
        return try {
            messageDao.getMessages(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "getMessages", "SM-CH-ERR-001", "Failed to get messages", e)
            throw e
        }
    }

    suspend fun insertMessage(message: Message) {
        try {
            messageDao.insertMessage(message)
            DebugLogger.i("ChatRepository", "insertMessage", "SM-CH-002", "Message inserted: ${message.id}")
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "insertMessage", "SM-CH-ERR-002", "Failed to insert message", e)
            throw e
        }
    }

    suspend fun deleteMessages(contactId: String) {
        try {
            messageDao.deleteMessages(contactId)
            DebugLogger.i("ChatRepository", "deleteMessages", "SM-CH-003", "Messages deleted for: $contactId")
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "deleteMessages", "SM-CH-ERR-003", "Failed to delete messages", e)
            throw e
        }
    }

    fun getLastMessage(contactId: String): Flow<Message?> {
        return try {
            chatDao.getLastMessage(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "getLastMessage", "SM-CH-ERR-004", "Failed to get last message", e)
            throw e
        }
    }

    fun getUnreadCount(contactId: String): Flow<Int> {
        return try {
            messageDao.getUnreadCount(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "getUnreadCount", "SM-CH-ERR-005", "Failed to get unread count", e)
            throw e
        }
    }

    suspend fun markAllRead(contactId: String) {
        try {
            messageDao.markAllRead(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "markAllRead", "SM-CH-ERR-006", "Failed to mark read", e)
        }
    }

    suspend fun getLastMessageSync(contactId: String): Message? {
        return try {
            messageDao.getLastMessageSync(contactId)
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "getLastMessageSync", "SM-CH-ERR-007", "Failed to get last message sync", e)
            null
        }
    }

    suspend fun markMessageDelivered(messageId: String) {
        try {
            messageDao.markMessageDelivered(messageId)
        } catch (e: Exception) {
            DebugLogger.e("ChatRepository", "markMessageDelivered", "SM-CH-ERR-008", "Failed to mark delivered", e)
        }
    }
}
