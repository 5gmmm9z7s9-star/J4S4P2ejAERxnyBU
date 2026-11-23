package com.example.data

import androidx.annotation.Keep
import com.example.crypto.VaultItemPayload
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class VaultCategory(val displayName: String) {
    LOGIN("账号密码"),
    CARD("银行卡"),
    SECURE_NOTE("机密笔记"),
    IDENTITY("重要证件");

    companion object {
        fun fromRaw(raw: String?): VaultCategory {
            return entries.firstOrNull { it.name == raw } ?: SECURE_NOTE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class VaultConfigDoc(
    val userId: String = "",
    val salt: String = "",
    val verifierCiphertext: String = "",
    val verifierIv: String = "",
    val passwordHint: String = "",
    val biometricEnabled: Boolean = true,
    val autoLockSeconds: Long = 60L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VaultItemDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = VaultCategory.LOGIN.name,
    val encryptedPayload: String = "",
    val iv: String = "",
    val favorite: Boolean = false,
    val strengthScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DecryptedVaultItem(
    val id: String,
    val category: VaultCategory,
    val payload: VaultItemPayload,
    val favorite: Boolean,
    val strengthScore: Int,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val decryptFailed: Boolean = false
)
