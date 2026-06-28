package com.jnetaol.securemessenger.pairing

import android.content.Context
import android.content.SharedPreferences
import com.jnetaol.securemessenger.crypto.CryptoManager
import com.jnetaol.securemessenger.logger.DebugLogger
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class IdentityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("identity_prefs", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()
    private val crypto = CryptoManager()

    private var cachedId: String? = null
    private var cachedPin: String? = null
    private var cachedPublicKey: String? = null
    private var cachedPrivateKey: String? = null

    init {
        DebugLogger.i("IdentityManager", "init", "SM-IM-001", "IdentityManager initialized")
        cachedId = prefs.getString("identity_id", null)
        cachedPin = prefs.getString("pin_code", null)
        cachedPublicKey = prefs.getString("public_key", null)
        cachedPrivateKey = prefs.getString("private_key", null)
        if (cachedId == null || cachedPin == null || cachedPublicKey == null) {
            generateNewIdentity()
        }
    }

    val identityId: String get() = cachedId ?: ""
    val pinCode: String get() = cachedPin ?: ""
    val publicKey: String get() = cachedPublicKey ?: ""
    val privateKey: String get() = cachedPrivateKey ?: ""

    val qrCodeData: String
        get() = "SM|${cachedId ?: ""}|${cachedPin ?: ""}|${cachedPublicKey ?: ""}"

    fun getQRDataWithP2P(ip: String, port: Int): String {
        return "SM|${cachedId ?: ""}|${cachedPin ?: ""}|${cachedPublicKey ?: ""}|$ip:$port"
    }

    fun regenerateIdentity() {
        DebugLogger.i("IdentityManager", "regenerateIdentity", "SM-IM-002", "Regenerating identity")
        generateNewIdentity()
    }

    private fun generateNewIdentity(): String {
        return try {
            val newId = UUID.randomUUID().toString()
            val newPin = generatePin()
            val keyPair = crypto.generateKeyPair()
            cachedId = newId
            cachedPin = newPin
            cachedPublicKey = keyPair.publicKey
            cachedPrivateKey = keyPair.privateKey
            prefs.edit()
                .putString("identity_id", newId)
                .putString("pin_code", newPin)
                .putString("public_key", keyPair.publicKey)
                .putString("private_key", keyPair.privateKey)
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
        for (i in 0 until 6) pin.append(secureRandom.nextInt(10))
        return pin.toString()
    }

    fun validatePin(input: String): Boolean = input.length == 6 && input.all { it.isDigit() }
}
