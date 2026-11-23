package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VaultRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("请先使用 Google 账号登录。")
    }

    private fun configDocRef(userId: String) =
        db.collection("users").document(userId)
            .collection("vault_config").document(MASTER_CONFIG_DOC_ID)

    private fun itemsCollectionRef(userId: String) =
        db.collection("users").document(userId)
            .collection("vault_items")

    fun observeVaultConfig(userId: String): Flow<VaultConfigDoc?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "登录账号不匹配。" }
        val docRef = configDocRef(userId)
        val path = docRef.path
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        VaultConfigDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeVaultItems(userId: String): Flow<List<VaultItemDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "登录账号不匹配。" }
        val colRef = itemsCollectionRef(userId)
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VaultItemDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createMasterConfig(
        userId: String,
        salt: String,
        verifierCiphertext: String,
        verifierIv: String,
        passwordHint: String,
        biometricEnabled: Boolean,
        autoLockSeconds: Int
    ): Result<Unit> {
        val docRef = configDocRef(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            val cleanHint = passwordHint.trim().take(100)
            val clampedTimeout = autoLockSeconds.coerceIn(0, 3600)
            require(salt.length in 16..64) { "盐值格式无效" }
            require(verifierCiphertext.length in 16..512) { "校验令牌格式无效" }
            require(verifierIv.length in 12..64) { "IV 格式无效" }

            val payload = mapOf(
                "userId" to uid,
                "salt" to salt,
                "verifierCiphertext" to verifierCiphertext,
                "verifierIv" to verifierIv,
                "passwordHint" to cleanHint,
                "biometricEnabled" to biometricEnabled,
                "autoLockSeconds" to clampedTimeout,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLockSettings(
        userId: String,
        biometricEnabled: Boolean,
        autoLockSeconds: Int,
        passwordHint: String
    ): Result<Unit> {
        val docRef = configDocRef(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            val cleanHint = passwordHint.trim().take(100)
            val clampedTimeout = autoLockSeconds.coerceIn(0, 3600)

            docRef.update(
                mapOf(
                    "biometricEnabled" to biometricEnabled,
                    "autoLockSeconds" to clampedTimeout,
                    "passwordHint" to cleanHint,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun rotateMasterPassword(
        userId: String,
        newSalt: String,
        newVerifierCiphertext: String,
        newVerifierIv: String,
        newHint: String,
        reencryptedItems: List<Triple<String, String, String>> // (itemId, newCiphertext, newIv)
    ): Result<Unit> {
        val configRef = configDocRef(userId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            val cleanHint = newHint.trim().take(100)
            require(newSalt.length in 16..64) { "盐值格式无效" }
            require(newVerifierCiphertext.length in 16..512) { "校验令牌格式无效" }
            require(newVerifierIv.length in 12..64) { "IV 格式无效" }

            val firstBatchItems = reencryptedItems.take(440)
            val remainingChunks = reencryptedItems.drop(440).chunked(450)

            db.runBatch { batch ->
                batch.update(
                    configRef,
                    mapOf(
                        "salt" to newSalt,
                        "verifierCiphertext" to newVerifierCiphertext,
                        "verifierIv" to newVerifierIv,
                        "passwordHint" to cleanHint,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                firstBatchItems.forEach { (itemId, cipher, iv) ->
                    val itemRef = itemsCollectionRef(userId).document(itemId)
                    batch.update(
                        itemRef,
                        mapOf(
                            "encryptedPayload" to cipher,
                            "iv" to iv,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            remainingChunks.forEach { chunk ->
                db.runBatch { batch ->
                    chunk.forEach { (itemId, cipher, iv) ->
                        val itemRef = itemsCollectionRef(userId).document(itemId)
                        batch.update(
                            itemRef,
                            mapOf(
                                "encryptedPayload" to cipher,
                                "iv" to iv,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, configRef.path)
            Result.failure(e)
        }
    }

    suspend fun addVaultItem(
        userId: String,
        category: VaultCategory,
        encryptedPayload: String,
        iv: String,
        favorite: Boolean,
        strengthScore: Int
    ): Result<Unit> {
        val colRef = itemsCollectionRef(userId)
        var docPath = colRef.path
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            require(encryptedPayload.length in 16..16384) { "加密数据超出大小限制" }
            require(iv.length in 12..64) { "IV 格式无效" }

            val docRef = colRef.document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "category" to category.name,
                "encryptedPayload" to encryptedPayload,
                "iv" to iv,
                "favorite" to favorite,
                "strengthScore" to strengthScore.coerceIn(0, 4),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateVaultItem(
        userId: String,
        itemId: String,
        category: VaultCategory,
        encryptedPayload: String,
        iv: String,
        favorite: Boolean,
        strengthScore: Int
    ): Result<Unit> {
        val docRef = itemsCollectionRef(userId).document(itemId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            require(itemId.isNotBlank()) { "条目 ID 不能为空" }
            require(encryptedPayload.length in 16..16384) { "加密数据超出大小限制" }
            require(iv.length in 12..64) { "IV 格式无效" }

            docRef.update(
                mapOf(
                    "category" to category.name,
                    "encryptedPayload" to encryptedPayload,
                    "iv" to iv,
                    "favorite" to favorite,
                    "strengthScore" to strengthScore.coerceIn(0, 4),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(
        userId: String,
        itemId: String,
        favorite: Boolean
    ): Result<Unit> {
        val docRef = itemsCollectionRef(userId).document(itemId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            require(itemId.isNotBlank()) { "条目 ID 不能为空" }
            docRef.update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(
        userId: String,
        itemId: String
    ): Result<Unit> {
        val docRef = itemsCollectionRef(userId).document(itemId)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "登录状态已变更，已拒绝跨账号写入。" }
            require(itemId.isNotBlank()) { "条目 ID 不能为空" }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val MASTER_CONFIG_DOC_ID = "master"
    }
}
