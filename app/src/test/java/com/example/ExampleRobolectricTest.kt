package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.VaultCryptoManager
import com.example.crypto.VaultItemPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("密盾", appName)
    }

    @Test
    fun `master password verification and AES-GCM payload roundtrip`() {
        val salt = VaultCryptoManager.generateSalt()
        val key = VaultCryptoManager.deriveKey("MySuperSecret#2026".toCharArray(), salt)
        val (verifierCipher, verifierIv) = VaultCryptoManager.createVerifier(key)

        val verifiedKey = VaultCryptoManager.verifyMasterPassword(
            masterPassword = "MySuperSecret#2026".toCharArray(),
            saltBase64 = salt,
            verifierCiphertextBase64 = verifierCipher,
            verifierIvBase64 = verifierIv
        )
        assertNotNull(verifiedKey)

        val wrongKey = VaultCryptoManager.verifyMasterPassword(
            masterPassword = "WrongPassword!".toCharArray(),
            saltBase64 = salt,
            verifierCiphertextBase64 = verifierCipher,
            verifierIvBase64 = verifierIv
        )
        assertNull(wrongKey)

        val original = VaultItemPayload(
            title = "招商银行",
            cardHolder = "张三",
            cardNumber = "6225880112345678",
            cardExpiry = "09/29",
            cardCvv = "888",
            notes = "工资卡"
        )
        val (cipher, iv) = VaultCryptoManager.encryptPayload(original, verifiedKey!!)
        val decrypted = VaultCryptoManager.decryptPayload(cipher, iv, verifiedKey)
        assertEquals(original, decrypted)
    }

    @Test
    fun `password generator creates high strength passwords`() {
        val pwd = VaultCryptoManager.generatePassword(
            length = 20,
            includeUpper = true,
            includeLower = true,
            includeDigits = true,
            includeSymbols = true
        )
        assertEquals(20, pwd.length)
        val strength = VaultCryptoManager.evaluateStrength(pwd)
        assertTrue(strength.score >= 3)
    }
}
