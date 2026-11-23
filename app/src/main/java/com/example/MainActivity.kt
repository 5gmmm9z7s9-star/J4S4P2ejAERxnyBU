package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.GoogleAuthHelper
import com.example.data.DecryptedVaultItem
import com.example.data.VaultCategory
import com.example.data.VaultRepository
import com.example.ui.DualLockScreen
import com.example.ui.PasswordGeneratorScreen
import com.example.ui.SecurityCenterScreen
import com.example.ui.UiState
import com.example.ui.VaultHomeScreen
import com.example.ui.VaultItemDetailScreen
import com.example.ui.VaultItemEditorScreen
import com.example.ui.VaultSetupScreen
import com.example.ui.VaultViewModel
import com.example.ui.theme.VaultShieldTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent screenshots & recent-apps thumbnail leaks for sensitive vault data
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            VaultShieldTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            VaultAuthenticatedRoot(user = user)
        }
    }
}

private enum class VaultTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    VAULT("保险箱", Icons.Rounded.Lock, Icons.Outlined.Lock),
    GENERATOR("生成器", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    SECURITY("安全中心", Icons.Rounded.Shield, Icons.Outlined.Shield)
}

@Composable
fun VaultAuthenticatedRoot(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: VaultViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                VaultViewModel(VaultRepository(db), user.uid)
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val configState by viewModel.configState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val biometricPassed by viewModel.biometricPassed.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val cooldownRemainingSec by viewModel.cooldownRemainingSec.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val decryptedItems by viewModel.decryptedItems.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val itemsError by viewModel.itemsError.collectAsStateWithLifecycle()
    val vaultHealth by viewModel.vaultHealth.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var clipboardClearJob by remember { mutableStateOf<Job?>(null) }

    fun copySensitiveWithAutoClear(label: String, text: String) {
        if (text.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.clipboard_sensitive_label), text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        viewModel.postMessage("已复制「$label」，将在 30 秒后自动清空剪贴板")
        clipboardClearJob?.cancel()
        clipboardClearJob = scope.launch {
            delay(30_000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearActionMessage()
    }

    fun performSignOut() {
        viewModel.lockVault()
        GoogleAuthHelper.signOut(credentialManager = credentialManager, scope = scope)
    }

    when (val cfgState = configState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "加载保险箱配置失败：${cfgState.message}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is UiState.Success -> {
            val config = cfgState.data
            if (config == null) {
                VaultSetupScreen(
                    userEmail = user.email,
                    isBusy = isBusy,
                    authError = authError,
                    onCreateVault = { pwd, hint, bio -> viewModel.createVault(pwd, hint, bio) },
                    onSignOut = ::performSignOut
                )
            } else if (!isUnlocked) {
                DualLockScreen(
                    config = config,
                    userEmail = user.email,
                    biometricPassed = biometricPassed,
                    isBusy = isBusy,
                    authError = authError,
                    cooldownRemainingSec = cooldownRemainingSec,
                    onBiometricSuccess = { viewModel.markBiometricPassed() },
                    onVerifyMasterPassword = { pwd -> viewModel.verifyMasterPassword(pwd) },
                    onSignOut = ::performSignOut
                )
            } else {
                var selectedTab by remember { mutableStateOf(VaultTab.VAULT) }
                var selectedDetailItemId by remember { mutableStateOf<String?>(null) }
                var isCreatingNew by remember { mutableStateOf(false) }
                var editingItem by remember { mutableStateOf<DecryptedVaultItem?>(null) }

                val detailItem = remember(selectedDetailItemId, decryptedItems) {
                    selectedDetailItemId?.let { id -> decryptedItems.firstOrNull { it.id == id } }
                }

                if (isCreatingNew || editingItem != null) {
                    VaultItemEditorScreen(
                        editingItem = editingItem,
                        isBusy = isBusy,
                        onBack = {
                            isCreatingNew = false
                            editingItem = null
                        },
                        onSave = { category, payload, favorite ->
                            viewModel.saveVaultItem(
                                editingItemId = editingItem?.id,
                                category = category,
                                payload = payload,
                                favorite = favorite,
                                onSaved = {
                                    isCreatingNew = false
                                    editingItem = null
                                }
                            )
                        }
                    )
                } else if (detailItem != null) {
                    VaultItemDetailScreen(
                        item = detailItem,
                        isBusy = isBusy,
                        onBack = { selectedDetailItemId = null },
                        onEdit = { editingItem = detailItem },
                        onToggleFavorite = { viewModel.toggleFavorite(detailItem) },
                        onDelete = {
                            viewModel.deleteItem(detailItem.id) {
                                selectedDetailItemId = null
                            }
                        },
                        onCopySensitive = ::copySensitiveWithAutoClear
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 600.dp
                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            floatingActionButton = {
                                if (selectedTab == VaultTab.VAULT) {
                                    FloatingActionButton(
                                        onClick = { isCreatingNew = true },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.testTag("add_vault_item_fab")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "新增机密条目"
                                        )
                                    }
                                }
                            },
                            bottomBar = {
                                if (!useRail) {
                                    NavigationBar(
                                        windowInsets = WindowInsets.navigationBars,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        VaultTab.entries.forEach { tab ->
                                            val selected = tab == selectedTab
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { selectedTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label) },
                                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (useRail) {
                                    NavigationRail(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        VaultTab.entries.forEach { tab ->
                                            val selected = tab == selectedTab
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { selectedTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label) }
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Box(modifier = Modifier.widthIn(max = 720.dp)) {
                                        when (selectedTab) {
                                            VaultTab.VAULT -> VaultHomeScreen(
                                                items = filteredItems,
                                                totalCount = decryptedItems.size,
                                                searchQuery = searchQuery,
                                                selectedFilter = selectedFilter,
                                                healthScore = vaultHealth.overallScore,
                                                biometricEnabled = config.biometricEnabled,
                                                itemsError = itemsError,
                                                onSearchChange = viewModel::updateSearchQuery,
                                                onFilterChange = viewModel::updateFilter,
                                                onItemClick = { selectedDetailItemId = it.id },
                                                onQuickCopy = { item ->
                                                    val (label, secret) = when (item.category) {
                                                        VaultCategory.LOGIN -> "密码" to item.payload.password
                                                        VaultCategory.CARD -> "卡号" to item.payload.cardNumber
                                                        VaultCategory.SECURE_NOTE -> "机密内容" to item.payload.notes
                                                        VaultCategory.IDENTITY -> "证件号" to item.payload.idNumber
                                                    }
                                                    if (secret.isNotBlank()) {
                                                        copySensitiveWithAutoClear(label, secret)
                                                    } else {
                                                        selectedDetailItemId = item.id
                                                    }
                                                },
                                                onToggleFavorite = viewModel::toggleFavorite,
                                                onAddClick = { isCreatingNew = true },
                                                onLockNow = viewModel::lockVault
                                            )
                                            VaultTab.GENERATOR -> PasswordGeneratorScreen(
                                                onCopyPassword = { pwd ->
                                                    copySensitiveWithAutoClear("生成的密码", pwd)
                                                }
                                            )
                                            VaultTab.SECURITY -> SecurityCenterScreen(
                                                config = config,
                                                userEmail = user.email,
                                                health = vaultHealth,
                                                isBusy = isBusy,
                                                onUpdateSettings = viewModel::updateLockSettings,
                                                onChangeMasterPassword = viewModel::changeMasterPassword,
                                                onOpenItem = { item ->
                                                    selectedDetailItemId = item.id
                                                },
                                                onLockNow = viewModel::lockVault,
                                                onSignOut = ::performSignOut
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
