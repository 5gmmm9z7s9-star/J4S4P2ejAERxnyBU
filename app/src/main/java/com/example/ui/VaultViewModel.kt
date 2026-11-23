package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.VaultCryptoManager
import com.example.crypto.VaultItemPayload
import com.example.data.DecryptedVaultItem
import com.example.data.VaultCategory
import com.example.data.VaultConfigDoc
import com.example.data.VaultItemDoc
import com.example.data.VaultRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class VaultFilter(val label: String) {
    ALL("全部"),
    FAVORITES("收藏"),
    LOGIN("账号密码"),
    CARD("银行卡"),
    SECURE_NOTE("机密笔记"),
    IDENTITY("重要证件")
}

data class VaultHealthSummary(
    val totalLogins: Int = 0,
    val strongCount: Int = 0,
    val weakItems: List<DecryptedVaultItem> = emptyList(),
    val reusedItems: List<DecryptedVaultItem> = emptyList(),
    val overallScore: Int = 100
)

class VaultViewModel(
    private val repository: VaultRepository,
    private val currentUserId: String
) : ViewModel() {

    val configState: StateFlow<UiState<VaultConfigDoc?>> =
        repository.observeVaultConfig(currentUserId)
            .map<VaultConfigDoc?, UiState<VaultConfigDoc?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing vault config", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "无法读取保险箱配置"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val rawItemsState: StateFlow<UiState<List<VaultItemDoc>>> =
        repository.observeVaultItems(currentUserId)
            .map<List<VaultItemDoc>, UiState<List<VaultItemDoc>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing vault items", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "无法读取加密条目"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _sessionKey = MutableStateFlow<SecretKey?>(null)
    private val _biometricPassed = MutableStateFlow(false)
    val biometricPassed: StateFlow<Boolean> = _biometricPassed.asStateFlow()

    val isVaultUnlocked: StateFlow<Boolean> = combine(
        _sessionKey,
        _biometricPassed,
        configState
    ) { key, bioPassed, cfgState ->
        val config = (cfgState as? UiState.Success)?.data
        if (config == null || key == null) {
            false
        } else {
            !config.biometricEnabled || bioPassed
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false
    )

    private val _decryptedItems = MutableStateFlow<List<DecryptedVaultItem>>(emptyList())
    val decryptedItems: StateFlow<List<DecryptedVaultItem>> = _decryptedItems.asStateFlow()

    val itemsError: StateFlow<String?> = rawItemsState.map { state ->
        (state as? UiState.Error)?.message
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(VaultFilter.ALL)
    val selectedFilter: StateFlow<VaultFilter> = _selectedFilter.asStateFlow()

    val filteredItems: StateFlow<List<DecryptedVaultItem>> = combine(
        _decryptedItems,
        _searchQuery,
        _selectedFilter
    ) { items, query, filter ->
        val trimmed = query.trim().lowercase()
        items
            .filter { item ->
                when (filter) {
                    VaultFilter.ALL -> true
                    VaultFilter.FAVORITES -> item.favorite
                    VaultFilter.LOGIN -> item.category == VaultCategory.LOGIN
                    VaultFilter.CARD -> item.category == VaultCategory.CARD
                    VaultFilter.SECURE_NOTE -> item.category == VaultCategory.SECURE_NOTE
                    VaultFilter.IDENTITY -> item.category == VaultCategory.IDENTITY
                }
            }
            .filter { item ->
                if (trimmed.isEmpty()) true
                else {
                    item.payload.title.lowercase().contains(trimmed) ||
                        item.payload.username.lowercase().contains(trimmed) ||
                        item.payload.url.lowercase().contains(trimmed) ||
                        item.payload.notes.lowercase().contains(trimmed) ||
                        item.payload.cardHolder.lowercase().contains(trimmed) ||
                        item.payload.idFullName.lowercase().contains(trimmed)
                }
            }
            .sortedWith(
                compareByDescending<DecryptedVaultItem> { it.favorite }
                    .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val vaultHealth: StateFlow<VaultHealthSummary> = _decryptedItems.map { items ->
        val logins = items.filter { it.category == VaultCategory.LOGIN && it.payload.password.isNotEmpty() }
        if (logins.isEmpty()) {
            VaultHealthSummary()
        } else {
            val weak = logins.filter { it.strengthScore <= 2 }
            val passwordGroups = logins.groupBy { it.payload.password }
            val reused = logins.filter { (passwordGroups[it.payload.password]?.size ?: 0) > 1 }
            val strongCount = logins.count { it.strengthScore >= 3 }
            val penaltyWeak = weak.size * 18
            val penaltyReused = reused.size * 15
            val score = (100 - penaltyWeak - penaltyReused).coerceIn(10, 100)
            VaultHealthSummary(
                totalLogins = logins.size,
                strongCount = strongCount,
                weakItems = weak,
                reusedItems = reused,
                overallScore = score
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VaultHealthSummary()
    )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    private val _cooldownRemainingSec = MutableStateFlow(0)
    val cooldownRemainingSec: StateFlow<Int> = _cooldownRemainingSec.asStateFlow()

    private var cooldownJob: Job? = null
    private var backgroundTimestampMs: Long? = null

    init {
        viewModelScope.launch {
            combine(rawItemsState, _sessionKey) { state, key ->
                state to key
            }.collect { (state, key) ->
                if (key == null || state !is UiState.Success) {
                    _decryptedItems.value = emptyList()
                } else {
                    val docs = state.data
                    val decrypted = withContext(Dispatchers.Default) {
                        docs.map { doc ->
                            val category = VaultCategory.fromRaw(doc.category)
                            val payload = VaultCryptoManager.decryptPayload(
                                ciphertextBase64 = doc.encryptedPayload,
                                ivBase64 = doc.iv,
                                secretKey = key
                            )
                            if (payload != null) {
                                DecryptedVaultItem(
                                    id = doc.id,
                                    category = category,
                                    payload = payload,
                                    favorite = doc.favorite,
                                    strengthScore = doc.strengthScore.toInt(),
                                    createdAt = doc.createdAt,
                                    updatedAt = doc.updatedAt,
                                    decryptFailed = false
                                )
                            } else {
                                DecryptedVaultItem(
                                    id = doc.id,
                                    category = category,
                                    payload = VaultItemPayload(title = "无法解密的条目"),
                                    favorite = doc.favorite,
                                    strengthScore = 0,
                                    createdAt = doc.createdAt,
                                    updatedAt = doc.updatedAt,
                                    decryptFailed = true
                                )
                            }
                        }
                    }
                    _decryptedItems.value = decrypted
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: VaultFilter) {
        _selectedFilter.value = filter
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun postMessage(msg: String) {
        _actionMessage.value = msg
    }

    fun markBiometricPassed() {
        _biometricPassed.value = true
        _authError.value = null
    }

    fun lockVault() {
        _sessionKey.value = null
        _biometricPassed.value = false
        _decryptedItems.value = emptyList()
        _authError.value = null
    }

    fun onAppBackgrounded() {
        if (_sessionKey.value != null && backgroundTimestampMs == null) {
            backgroundTimestampMs = System.currentTimeMillis()
        }
    }

    fun onAppForegrounded() {
        val bgTime = backgroundTimestampMs ?: return
        backgroundTimestampMs = null
        val config = (configState.value as? UiState.Success)?.data ?: return
        val timeoutMs = config.autoLockSeconds * 1000L
        if (System.currentTimeMillis() - bgTime >= timeoutMs) {
            lockVault()
        }
    }

    fun createVault(
        masterPassword: String,
        hint: String,
        biometricEnabled: Boolean
    ) {
        if (_isBusy.value) return
        if (masterPassword.length < 6) {
            _authError.value = "主密码长度至少需要 6 位字符"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            try {
                val (salt, key, verifier) = withContext(Dispatchers.Default) {
                    val generatedSalt = VaultCryptoManager.generateSalt()
                    val derivedKey = VaultCryptoManager.deriveKey(masterPassword.toCharArray(), generatedSalt)
                    val verifierPair = VaultCryptoManager.createVerifier(derivedKey)
                    Triple(generatedSalt, derivedKey, verifierPair)
                }
                val result = repository.createMasterConfig(
                    userId = currentUserId,
                    salt = salt,
                    verifierCiphertext = verifier.first,
                    verifierIv = verifier.second,
                    passwordHint = hint,
                    biometricEnabled = biometricEnabled,
                    autoLockSeconds = 60
                )
                result.onSuccess {
                    _biometricPassed.value = true
                    _sessionKey.value = key
                    _actionMessage.value = "保险箱创建成功，已开启双重安全保护"
                }.onFailure { err ->
                    _authError.value = err.localizedMessage ?: "创建保险箱失败，请检查网络连接"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun verifyMasterPassword(masterPassword: String) {
        if (_isBusy.value) return
        if (_cooldownRemainingSec.value > 0) {
            _authError.value = "尝试次数过多，请等待 ${_cooldownRemainingSec.value} 秒后重试"
            return
        }
        val config = (configState.value as? UiState.Success)?.data
        if (config == null) {
            _authError.value = "正在加载保险箱配置，请稍候"
            return
        }
        if (config.biometricEnabled && !_biometricPassed.value) {
            _authError.value = "请先完成第一道保险（指纹 / 生物识别）验证"
            return
        }
        if (masterPassword.isEmpty()) {
            _authError.value = "请输入主密码"
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            try {
                val verifiedKey = withContext(Dispatchers.Default) {
                    VaultCryptoManager.verifyMasterPassword(
                        masterPassword = masterPassword.toCharArray(),
                        saltBase64 = config.salt,
                        verifierCiphertextBase64 = config.verifierCiphertext,
                        verifierIvBase64 = config.verifierIv
                    )
                }
                if (verifiedKey != null) {
                    _failedAttempts.value = 0
                    _sessionKey.value = verifiedKey
                } else {
                    val nextFails = _failedAttempts.value + 1
                    _failedAttempts.value = nextFails
                    if (nextFails >= 5) {
                        startCooldown(15)
                        _authError.value = "连续 5 次主密码错误，已触发 15 秒安全冷却"
                    } else {
                        _authError.value = "主密码不正确（剩余 ${5 - nextFails} 次尝试）"
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (sec in seconds downTo 1) {
                _cooldownRemainingSec.value = sec
                delay(1000L)
            }
            _cooldownRemainingSec.value = 0
            _failedAttempts.value = 0
        }
    }

    fun saveVaultItem(
        editingItemId: String?,
        category: VaultCategory,
        payload: VaultItemPayload,
        favorite: Boolean,
        onSaved: () -> Unit
    ) {
        val key = _sessionKey.value
        if (key == null) {
            _actionMessage.value = "保险箱已锁定，请重新解锁"
            return
        }
        if (payload.title.isBlank()) {
            _actionMessage.value = "请填写名称或标题"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val (cipherText, iv, score) = withContext(Dispatchers.Default) {
                    val (enc, ivStr) = VaultCryptoManager.encryptPayload(payload, key)
                    val strength = if (category == VaultCategory.LOGIN && payload.password.isNotEmpty()) {
                        VaultCryptoManager.evaluateStrength(payload.password).score
                    } else {
                        0
                    }
                    Triple(enc, ivStr, strength)
                }
                val result = if (editingItemId == null) {
                    repository.addVaultItem(
                        userId = currentUserId,
                        category = category,
                        encryptedPayload = cipherText,
                        iv = iv,
                        favorite = favorite,
                        strengthScore = score
                    )
                } else {
                    repository.updateVaultItem(
                        userId = currentUserId,
                        itemId = editingItemId,
                        category = category,
                        encryptedPayload = cipherText,
                        iv = iv,
                        favorite = favorite,
                        strengthScore = score
                    )
                }
                result.onSuccess {
                    _actionMessage.value = if (editingItemId == null) "已加密存入密库" else "已更新加密记录"
                    onSaved()
                }.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "保存失败，请重试"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun toggleFavorite(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.toggleFavorite(
                userId = currentUserId,
                itemId = item.id,
                favorite = !item.favorite
            ).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "更新收藏状态失败"
            }
        }
    }

    fun deleteItem(itemId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.deleteVaultItem(currentUserId, itemId)
                    .onSuccess {
                        _actionMessage.value = "已从保险箱永久删除"
                        onDeleted()
                    }
                    .onFailure { err ->
                        _actionMessage.value = err.localizedMessage ?: "删除失败"
                    }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateLockSettings(
        biometricEnabled: Boolean,
        autoLockSeconds: Int,
        hint: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.updateLockSettings(
                    userId = currentUserId,
                    biometricEnabled = biometricEnabled,
                    autoLockSeconds = autoLockSeconds,
                    passwordHint = hint
                ).onSuccess {
                    _actionMessage.value = "安全设置已更新"
                }.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "更新安全设置失败"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun changeMasterPassword(
        currentPassword: String,
        newPassword: String,
        newHint: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val config = (configState.value as? UiState.Success)?.data
        if (config == null) {
            onComplete(false, "配置未加载")
            return
        }
        if (newPassword.length < 6) {
            onComplete(false, "新主密码至少需要 6 位字符")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val oldKey = withContext(Dispatchers.Default) {
                    VaultCryptoManager.verifyMasterPassword(
                        masterPassword = currentPassword.toCharArray(),
                        saltBase64 = config.salt,
                        verifierCiphertextBase64 = config.verifierCiphertext,
                        verifierIvBase64 = config.verifierIv
                    )
                }
                if (oldKey == null) {
                    onComplete(false, "当前主密码验证失败")
                    return@launch
                }

                val currentItems = _decryptedItems.value.filter { !it.decryptFailed }
                val (newSalt, newKey, newVerifier, reencrypted) = withContext(Dispatchers.Default) {
                    val salt = VaultCryptoManager.generateSalt()
                    val key = VaultCryptoManager.deriveKey(newPassword.toCharArray(), salt)
                    val verifier = VaultCryptoManager.createVerifier(key)
                    val itemsReenc = currentItems.map { item ->
                        val (cipher, iv) = VaultCryptoManager.encryptPayload(item.payload, key)
                        Triple(item.id, cipher, iv)
                    }
                    Quadruple(salt, key, verifier, itemsReenc)
                }

                val result = repository.rotateMasterPassword(
                    userId = currentUserId,
                    newSalt = newSalt,
                    newVerifierCiphertext = newVerifier.first,
                    newVerifierIv = newVerifier.second,
                    newHint = newHint,
                    reencryptedItems = reencrypted
                )
                result.onSuccess {
                    _sessionKey.value = newKey
                    _actionMessage.value = "主密码已更换，全部密文已重新加密"
                    onComplete(true, null)
                }.onFailure { err ->
                    onComplete(false, err.localizedMessage ?: "更换主密码失败")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private companion object {
        const val TAG = "VaultVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
