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
                serverAddress = prefs.getString("server_address", "0.0.0.0") ?: "0.0.0.0",
                serverPort = prefs.getInt("server_port", 8080),
                useLocalNetwork = prefs.getBoolean("use_local_network", true),
                encryptionEnabled = prefs.getBoolean("encryption_enabled", true),
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
                .putString("server_address", newSettings.serverAddress)
                .putInt("server_port", newSettings.serverPort)
                .putBoolean("use_local_network", newSettings.useLocalNetwork)
                .putBoolean("encryption_enabled", newSettings.encryptionEnabled)
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
