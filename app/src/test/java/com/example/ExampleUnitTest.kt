package com.example

import com.example.crypto.VaultCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun passwordGenerator_producesRequestedLengthAndHighStrength() {
        val pwd = VaultCryptoManager.generatePassword(
            length = 20,
            includeUpper = true,
            includeLower = true,
            includeDigits = true,
            includeSymbols = true
        )
        assertEquals(20, pwd.length)
        assertTrue(pwd.any { it.isUpperCase() })
        assertTrue(pwd.any { it.isLowerCase() })
        assertTrue(pwd.any { it.isDigit() })
        assertTrue(pwd.any { !it.isLetterOrDigit() })

        val strength = VaultCryptoManager.evaluateStrength(pwd)
        assertTrue(strength.score >= 3)
        assertTrue(strength.entropyBits >= 72)
    }

    @Test
    fun evaluateStrength_distinguishesWeakAndStrongPasswords() {
        val weak = VaultCryptoManager.evaluateStrength("12345")
        val strong = VaultCryptoManager.evaluateStrength("X9#mK2!pQ7@vL4&z")
        assertTrue(weak.score < strong.score)
        assertEquals(4, strong.score)
    }
}
