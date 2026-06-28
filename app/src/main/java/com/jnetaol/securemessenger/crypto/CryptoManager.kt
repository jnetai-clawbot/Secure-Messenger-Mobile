package com.jnetaol.securemessenger.crypto

import com.jnetaol.securemessenger.logger.DebugLogger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class CryptoKeyPair(
    val publicKey: String,
    val privateKey: String
)

class CryptoManager {
    private val secureRandom = SecureRandom()
    private val keyGenerator = KeyGenerator.getInstance("AES").apply { init(256) }

    init {
        DebugLogger.i("CryptoManager", "init", "SM-CR-001", "CryptoManager initialized")
    }

    fun generateKeyPair(): CryptoKeyPair {
        return try {
            DebugLogger.d("CryptoManager", "generateKeyPair", "SM-CR-002")
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048, secureRandom)
            val keyPair = keyGen.generateKeyPair()
            val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
            val privateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)
            DebugLogger.i("CryptoManager", "generateKeyPair", "SM-CR-003", "Key pair generated")
            CryptoKeyPair(publicKey, privateKey)
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "generateKeyPair", "SM-CR-ERR-001", "Failed to generate key pair", e)
            throw SecurityException("Key generation failed: ${e.message}", e)
        }
    }

    fun generateAESKey(): SecretKey {
        return try {
            DebugLogger.d("CryptoManager", "generateAESKey", "SM-CR-004")
            keyGenerator.generateKey()
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "generateAESKey", "SM-CR-ERR-002", "Failed to generate AES key", e)
            throw SecurityException("AES key generation failed: ${e.message}", e)
        }
    }

    fun encrypt(plainText: String, publicKeyStr: String): String {
        if (publicKeyStr.isBlank() || publicKeyStr.length < 20) {
            DebugLogger.w("CryptoManager", "encrypt", "SM-CR-WARN-001", "Invalid public key, returning plaintext")
            return plainText
        }
        return try {
            DebugLogger.d("CryptoManager", "encrypt", "SM-CR-005", "Encrypting text")
            val aesKey = generateAESKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(
                java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyStr))
            )
            rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey)
            val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)

            val combined = ByteArray(4 + iv.size + 2 + encryptedAesKey.size + encryptedData.size)
            System.arraycopy(intToBytes(iv.size), 0, combined, 0, 4)
            System.arraycopy(iv, 0, combined, 4, iv.size)
            System.arraycopy(shortToBytes(encryptedAesKey.size.toShort()), 0, combined, 4 + iv.size, 2)
            System.arraycopy(encryptedAesKey, 0, combined, 4 + iv.size + 2, encryptedAesKey.size)
            System.arraycopy(encryptedData, 0, combined, 4 + iv.size + 2 + encryptedAesKey.size, encryptedData.size)

            Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "encrypt", "SM-CR-ERR-003", "Encryption failed, falling back to plaintext", e)
            plainText
        }
    }

    fun decrypt(encryptedText: String, privateKeyStr: String): String {
        return try {
            DebugLogger.d("CryptoManager", "decrypt", "SM-CR-006", "Decrypting text")
            val combined = Base64.getDecoder().decode(encryptedText)
            var offset = 0

            val ivLen = bytesToInt(combined, offset); offset += 4
            val iv = combined.copyOfRange(offset, offset + ivLen); offset += ivLen
            val aesKeyLen = bytesToShort(combined, offset).toInt(); offset += 2
            val encryptedAesKey = combined.copyOfRange(offset, offset + aesKeyLen); offset += aesKeyLen
            val encryptedData = combined.copyOfRange(offset, combined.size)

            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val privKey = keyFactory.generatePrivate(
                java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyStr))
            )
            val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            rsaCipher.init(Cipher.DECRYPT_MODE, privKey)
            val aesKeyBytes = rsaCipher.doFinal(encryptedAesKey)
            val aesKey = SecretKeySpec(aesKeyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encryptedData), Charsets.UTF_8)
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "decrypt", "SM-CR-ERR-004", "Decryption failed", e)
            throw SecurityException("Decryption failed: ${e.message}", e)
        }
    }

    fun encryptBytes(data: ByteArray, publicKeyStr: String): ByteArray {
        if (publicKeyStr.isBlank() || publicKeyStr.length < 20) {
            DebugLogger.w("CryptoManager", "encryptBytes", "SM-CR-WARN-002", "Invalid public key, returning raw bytes")
            return data
        }
        return try {
            DebugLogger.d("CryptoManager", "encryptBytes", "SM-CR-007", "Encrypting ${data.size} bytes")
            val aesKey = generateAESKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            val encryptedData = cipher.doFinal(data)

            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(
                java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyStr))
            )
            val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey)
            val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)

            val combined = ByteArray(4 + iv.size + 2 + encryptedAesKey.size + encryptedData.size)
            System.arraycopy(intToBytes(iv.size), 0, combined, 0, 4)
            System.arraycopy(iv, 0, combined, 4, iv.size)
            System.arraycopy(shortToBytes(encryptedAesKey.size.toShort()), 0, combined, 4 + iv.size, 2)
            System.arraycopy(encryptedAesKey, 0, combined, 4 + iv.size + 2, encryptedAesKey.size)
            System.arraycopy(encryptedData, 0, combined, 4 + iv.size + 2 + encryptedAesKey.size, encryptedData.size)
            combined
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "encryptBytes", "SM-CR-ERR-005", "Byte encryption failed, falling back to raw", e)
            data
        }
    }

    fun decryptBytes(encryptedData: ByteArray, privateKeyStr: String): ByteArray {
        return try {
            DebugLogger.d("CryptoManager", "decryptBytes", "SM-CR-008", "Decrypting ${encryptedData.size} bytes")
            var offset = 0
            val ivLen = bytesToInt(encryptedData, offset); offset += 4
            val iv = encryptedData.copyOfRange(offset, offset + ivLen); offset += ivLen
            val aesKeyLen = bytesToShort(encryptedData, offset).toInt(); offset += 2
            val encryptedAesKey = encryptedData.copyOfRange(offset, offset + aesKeyLen); offset += aesKeyLen
            val encryptedPayload = encryptedData.copyOfRange(offset, encryptedData.size)

            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val privKey = keyFactory.generatePrivate(
                java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyStr))
            )
            val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            rsaCipher.init(Cipher.DECRYPT_MODE, privKey)
            val aesKeyBytes = rsaCipher.doFinal(encryptedAesKey)
            val aesKey = SecretKeySpec(aesKeyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            cipher.doFinal(encryptedPayload)
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "decryptBytes", "SM-CR-ERR-006", "Byte decryption failed", e)
            throw SecurityException("Byte decryption failed: ${e.message}", e)
        }
    }

    fun hash(data: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(hash)
        } catch (e: Exception) {
            DebugLogger.e("CryptoManager", "hash", "SM-CR-ERR-007", "Hash failed", e)
            throw SecurityException("Hash failed: ${e.message}", e)
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun bytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf((value.toInt() shr 8).toByte(), value.toByte())
    }

    private fun bytesToShort(bytes: ByteArray, offset: Int): Short {
        return (((bytes[offset].toInt() and 0xFF) shl 8) or
                (bytes[offset + 1].toInt() and 0xFF)).toShort()
    }
}
