package com.example.crypto

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class VaultItemPayload(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val cardHolder: String = "",
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val cardPin: String = "",
    val idFullName: String = "",
    val idNumber: String = "",
    val idExpiry: String = ""
) {
    fun toJsonString(): String = JSONObject().apply {
        put("title", title)
        put("username", username)
        put("password", password)
        put("url", url)
        put("notes", notes)
        put("cardHolder", cardHolder)
        put("cardNumber", cardNumber)
        put("cardExpiry", cardExpiry)
        put("cardCvv", cardCvv)
        put("cardPin", cardPin)
        put("idFullName", idFullName)
        put("idNumber", idNumber)
        put("idExpiry", idExpiry)
    }.toString()

    companion object {
        fun fromJsonString(json: String): VaultItemPayload {
            val obj = JSONObject(json)
            return VaultItemPayload(
                title = obj.optString("title", ""),
                username = obj.optString("username", ""),
                password = obj.optString("password", ""),
                url = obj.optString("url", ""),
                notes = obj.optString("notes", ""),
                cardHolder = obj.optString("cardHolder", ""),
                cardNumber = obj.optString("cardNumber", ""),
                cardExpiry = obj.optString("cardExpiry", ""),
                cardCvv = obj.optString("cardCvv", ""),
                cardPin = obj.optString("cardPin", ""),
                idFullName = obj.optString("idFullName", ""),
                idNumber = obj.optString("idNumber", ""),
                idExpiry = obj.optString("idExpiry", "")
            )
        }
    }
}

data class PasswordStrengthResult(
    val score: Int, // 0..4
    val label: String,
    val entropyBits: Int
)

object VaultCryptoManager {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val AES_ALGORITHM = "AES"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val VERIFIER_PLAINTEXT = "VAULT_SHIELD_VERIFIER_TOKEN_V1"

    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun deriveKey(masterPassword: CharArray, saltBase64: String): SecretKey {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, AES_ALGORITHM)
        } finally {
            spec.clearPassword()
        }
    }

    fun createVerifier(secretKey: SecretKey): Pair<String, String> {
        return encryptString(VERIFIER_PLAINTEXT, secretKey)
    }

    fun verifyMasterPassword(
        masterPassword: CharArray,
        saltBase64: String,
        verifierCiphertextBase64: String,
        verifierIvBase64: String
    ): SecretKey? {
        return try {
            val candidateKey = deriveKey(masterPassword, saltBase64)
            val decrypted = decryptString(verifierCiphertextBase64, verifierIvBase64, candidateKey)
            if (decrypted == VERIFIER_PLAINTEXT) candidateKey else null
        } catch (_: Exception) {
            null
        }
    }

    fun encryptPayload(payload: VaultItemPayload, secretKey: SecretKey): Pair<String, String> {
        return encryptString(payload.toJsonString(), secretKey)
    }

    fun decryptPayload(
        ciphertextBase64: String,
        ivBase64: String,
        secretKey: SecretKey
    ): VaultItemPayload? {
        return try {
            val json = decryptString(ciphertextBase64, ivBase64, secretKey) ?: return null
            VaultItemPayload.fromJsonString(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun encryptString(plaintext: String, secretKey: SecretKey): Pair<String, String> {
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP) to
            Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    private fun decryptString(
        ciphertextBase64: String,
        ivBase64: String,
        secretKey: SecretKey
    ): String? {
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#\$%^&*()-_=+[]{};:,.?"

        val pools = mutableListOf<String>()
        if (includeUpper) pools.add(upper)
        if (includeLower) pools.add(lower)
        if (includeDigits) pools.add(digits)
        if (includeSymbols) pools.add(symbols)
        if (pools.isEmpty()) pools.add(lower + digits)

        val allChars = pools.joinToString("")
        val targetLength = length.coerceIn(6, 64)
        val result = CharArray(targetLength)

        // Guarantee at least one character from each selected pool
        for (i in pools.indices) {
            if (i < targetLength) {
                val pool = pools[i]
                result[i] = pool[secureRandom.nextInt(pool.length)]
            }
        }
        for (i in pools.size until targetLength) {
            result[i] = allChars[secureRandom.nextInt(allChars.length)]
        }

        // Fisher-Yates shuffle
        for (i in result.lastIndex downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }
        return String(result)
    }

    fun evaluateStrength(password: String): PasswordStrengthResult {
        if (password.isEmpty()) {
            return PasswordStrengthResult(score = 0, label = "未设置", entropyBits = 0)
        }
        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32
        if (poolSize == 0) poolSize = 26

        val entropy = (password.length * kotlin.math.log2(poolSize.toDouble())).toInt()
        val uniqueRatio = password.toSet().size.toDouble() / password.length.toDouble()

        val score = when {
            password.length < 6 || entropy < 28 -> 1
            password.length < 10 || entropy < 48 || uniqueRatio < 0.45 -> 2
            entropy < 72 -> 3
            else -> 4
        }
        val label = when (score) {
            1 -> "较弱"
            2 -> "一般"
            3 -> "较强"
            4 -> "极强"
            else -> "未设置"
        }
        return PasswordStrengthResult(score = score, label = label, entropyBits = entropy)
    }
}
