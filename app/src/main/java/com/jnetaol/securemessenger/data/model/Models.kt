package com.jnetaol.securemessenger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val displayName: String,
    val publicKey: String,
    val privateKey: String,
    val isBlocked: Boolean = false,
    val isFriend: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val contactId: String,
    val content: String,
    val originalContent: String = "",
    val isEncrypted: Boolean = true,
    val isFromMe: Boolean = true,
    val isFile: Boolean = false,
    val isNudge: Boolean = false,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val encryptError: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ContactWithMeta(
    val contact: Contact,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0
)

data class AppSettings(
    val stunServer1: String = "stun:stun.l.google.com:19302",
    val stunServer2: String = "stun:stun1.l.google.com:19302",
    val turnServer: String = "turn:openrelay.metered.ca:80",
    val turnUsername: String = "openrelayproject",
    val turnPassword: String = "openrelayproject",
    val useCustomTurn: Boolean = false,
    val customTurnServer: String = "",
    val customTurnUsername: String = "",
    val customTurnPassword: String = "",
    val enableIPv6: Boolean = true,
    val preferTCP: Boolean = false,
    val useUDP: Boolean = true,
    val localPort: Int = 0,
    val encryptionEnabled: Boolean = true,
    val blockUnencrypted: Boolean = false,
    val autoAcceptFiles: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val keepHistoryDays: Int = 30,
    val storageLocation: String = "",
    val isDarkMode: Boolean = true,
    val primaryColor: Long = 0xFF1A73E8,
    val backgroundColor: Long = 0xFF121212,
    val textColor: Long = 0xFFFFFFFF,
    val accentColor: Long = 0xFF4CAF50
)
