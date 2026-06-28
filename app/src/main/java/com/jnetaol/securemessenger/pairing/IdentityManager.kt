package com.jnetaol.securemessenger.pairing

import android.content.Context
import android.content.SharedPreferences
import com.jnetaol.securemessenger.logger.DebugLogger
import java.security.SecureRandom
import java.util.UUID

class IdentityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("identity_prefs", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    private var cachedId: String? = null
    private var cachedPin: String? = null

    init {
        DebugLogger.i("IdentityManager", "init", "SM-IM-001", "IdentityManager initialized")
        cachedId = prefs.getString("identity_id", null)
        cachedPin = prefs.getString("pin_code", null)
        if (cachedId == null || cachedPin == null) {
            generateNewIdentity()
        }
    }

    val identityId: String
        get() = cachedId ?: ""

    val qrCodeData: String
        get() = "SM|${cachedId ?: ""}|${cachedPin ?: ""}"

    val pinCode: String
        get() = cachedPin ?: ""

    fun regenerateIdentity() {
        DebugLogger.i("IdentityManager", "regenerateIdentity", "SM-IM-002", "Regenerating identity")
        generateNewIdentity()
    }

    private fun generateNewIdentity(): String {
        return try {
            val newId = UUID.randomUUID().toString()
            val newPin = generatePin()
            cachedId = newId
            cachedPin = newPin
            prefs.edit()
                .putString("identity_id", newId)
                .putString("pin_code", newPin)
                .apply()
            DebugLogger.i("IdentityManager", "generateNewIdentity", "SM-IM-003", "New identity: $newId, PIN: $newPin")
            newId
        } catch (e: Exception) {
            DebugLogger.e("IdentityManager", "generateNewIdentity", "SM-IM-ERR-001", "Failed to generate identity", e)
            throw e
        }
    }

    private fun generatePin(): String {
        val pin = StringBuilder()
        for (i in 0 until 6) {
            pin.append(secureRandom.nextInt(10))
        }
        return pin.toString()
    }

    fun validatePin(input: String): Boolean {
        return input.length == 6 && input.all { it.isDigit() }
    }
}
