package com.jnetaol.securemessenger.data.repository

import android.content.Context
import android.os.Environment
import com.jnetaol.securemessenger.data.model.AppSettings
import com.jnetaol.securemessenger.logger.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()

    init {
        DebugLogger.i("SettingsRepository", "init", "SM-SR-001", "SettingsRepository initialized")
    }

    private fun loadSettings(): AppSettings {
        return try {
            AppSettings(
                stunServer1 = prefs.getString("stun_server1", "stun:stun.l.google.com:19302") ?: "stun:stun.l.google.com:19302",
                stunServer2 = prefs.getString("stun_server2", "stun:stun1.l.google.com:19302") ?: "stun:stun1.l.google.com:19302",
                turnServer = prefs.getString("turn_server", "turn:openrelay.metered.ca:80") ?: "turn:openrelay.metered.ca:80",
                turnUsername = prefs.getString("turn_username", "openrelayproject") ?: "openrelayproject",
                turnPassword = prefs.getString("turn_password", "openrelayproject") ?: "openrelayproject",
                useCustomTurn = prefs.getBoolean("use_custom_turn", false),
                customTurnServer = prefs.getString("custom_turn_server", "") ?: "",
                customTurnUsername = prefs.getString("custom_turn_username", "") ?: "",
                customTurnPassword = prefs.getString("custom_turn_password", "") ?: "",
                enableIPv6 = prefs.getBoolean("enable_ipv6", true),
                preferTCP = prefs.getBoolean("prefer_tcp", false),
                useUDP = prefs.getBoolean("use_udp", true),
                localPort = prefs.getInt("local_port", 0),
                encryptionEnabled = prefs.getBoolean("encryption_enabled", true),
                blockUnencrypted = prefs.getBoolean("block_unencrypted", false),
                autoAcceptFiles = prefs.getBoolean("auto_accept_files", false),
                notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
                soundEnabled = prefs.getBoolean("sound_enabled", true),
                vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
                keepHistoryDays = prefs.getInt("keep_history_days", 30),
                storageLocation = prefs.getString("storage_location", "") ?: "",
                isDarkMode = prefs.getBoolean("is_dark_mode", true),
                primaryColor = prefs.getLong("primary_color", 0xFF1A73E8),
                backgroundColor = prefs.getLong("background_color", 0xFF121212),
                textColor = prefs.getLong("text_color", 0xFFFFFFFF),
                accentColor = prefs.getLong("accent_color", 0xFF4CAF50)
            )
        } catch (e: Exception) {
            DebugLogger.e("SettingsRepository", "loadSettings", "SM-SR-ERR-001", "Failed to load settings", e)
            AppSettings()
        }
    }

    suspend fun updateSettings(newSettings: AppSettings) {
        try {
            prefs.edit()
                .putString("stun_server1", newSettings.stunServer1)
                .putString("stun_server2", newSettings.stunServer2)
                .putString("turn_server", newSettings.turnServer)
                .putString("turn_username", newSettings.turnUsername)
                .putString("turn_password", newSettings.turnPassword)
                .putBoolean("use_custom_turn", newSettings.useCustomTurn)
                .putString("custom_turn_server", newSettings.customTurnServer)
                .putString("custom_turn_username", newSettings.customTurnUsername)
                .putString("custom_turn_password", newSettings.customTurnPassword)
                .putBoolean("enable_ipv6", newSettings.enableIPv6)
                .putBoolean("prefer_tcp", newSettings.preferTCP)
                .putBoolean("use_udp", newSettings.useUDP)
                .putInt("local_port", newSettings.localPort)
                .putBoolean("encryption_enabled", newSettings.encryptionEnabled)
                .putBoolean("block_unencrypted", newSettings.blockUnencrypted)
                .putBoolean("auto_accept_files", newSettings.autoAcceptFiles)
                .putBoolean("notifications_enabled", newSettings.notificationsEnabled)
                .putBoolean("sound_enabled", newSettings.soundEnabled)
                .putBoolean("vibration_enabled", newSettings.vibrationEnabled)
                .putInt("keep_history_days", newSettings.keepHistoryDays)
                .putString("storage_location", newSettings.storageLocation)
                .putBoolean("is_dark_mode", newSettings.isDarkMode)
                .putLong("primary_color", newSettings.primaryColor)
                .putLong("background_color", newSettings.backgroundColor)
                .putLong("text_color", newSettings.textColor)
                .putLong("accent_color", newSettings.accentColor)
                .apply()
            _settings.value = newSettings
            DebugLogger.i("SettingsRepository", "updateSettings", "SM-SR-002", "Settings updated")
        } catch (e: Exception) {
            DebugLogger.e("SettingsRepository", "updateSettings", "SM-SR-ERR-002", "Failed to update settings", e)
            throw e
        }
    }

    fun getStorageDir(context: Context): File {
        val location = _settings.value.storageLocation
        return if (location.isNotEmpty()) {
            val dir = File(location)
            if (!dir.exists()) dir.mkdirs()
            dir
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SecureMessenger")
            if (!dir.exists()) dir.mkdirs()
            dir
        }
    }
}
