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
    val timestamp: Long = System.currentTimeMillis()
)

data class AppSettings(
    val serverAddress: String = "0.0.0.0",
    val serverPort: Int = 8080,
    val useLocalNetwork: Boolean = true,
    val encryptionEnabled: Boolean = true,
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
