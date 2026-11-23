package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.crypto.VaultCryptoManager
import com.example.data.VaultConfigDoc
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun VaultSetupScreen(
    userEmail: String?,
    isBusy: Boolean,
    authError: String?,
    onCreateVault: (masterPassword: String, hint: String, biometricEnabled: Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val biometricStatus = remember { BiometricAuthHelper.checkBiometricStatus(context) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var localError by remember { mutableStateOf<String?>(null) }

    val strength = remember(password) { VaultCryptoManager.evaluateStrength(password) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "初始化您的专属密库",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = "退出账号",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "设置主密码（最高权限密钥）",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "所有密码、银行卡与机密笔记都会在本地使用您的主密码加密后才同步云端。请务必牢记，系统无法为您找回。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("设置主密码（至少 6 位）") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_master_password_input")
            )

            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                StrengthMeterBar(score = strength.score, label = strength.label, entropyBits = strength.entropyBits)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = null
                },
                label = { Text("再次确认主密码") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = hint,
                onValueChange = { if (it.length <= 100) hint = it },
                label = { Text("密码提示语（选填，切勿包含密码本身）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_hint_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "开启指纹 + 主密码双重保险",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (biometricStatus) {
                                    BiometricStatus.BIOMETRIC_READY -> "设备支持生物识别，每次打开先验指纹再验主密码"
                                    BiometricStatus.DEVICE_CREDENTIAL_ONLY -> "将使用设备锁屏密码作为第一道生物/系统防线"
                                    else -> "当前设备未录入指纹，开启后将在录入时生效"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        modifier = Modifier.testTag("setup_biometric_switch")
                    )
                }
            }

            val displayError = localError ?: authError
            if (displayError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        password.length < 6 -> localError = "主密码长度至少需要 6 位"
                        password != confirmPassword -> localError = "两次输入的主密码不一致"
                        else -> onCreateVault(password, hint, biometricEnabled)
                    }
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_vault_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("正在生成密钥并创建...")
                } else {
                    Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("创建并加密进入保险箱", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DualLockScreen(
    config: VaultConfigDoc,
    userEmail: String?,
    biometricPassed: Boolean,
    isBusy: Boolean,
    authError: String?,
    cooldownRemainingSec: Int,
    onBiometricSuccess: () -> Unit,
    onVerifyMasterPassword: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricStatus = remember { BiometricAuthHelper.checkBiometricStatus(context) }

    var masterPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var biometricFeedback by remember { mutableStateOf<String?>(null) }

    val layer1Passed = !config.biometricEnabled || biometricPassed

    fun triggerBiometricPrompt() {
        if (activity == null) return
        biometricFeedback = null
        when (biometricStatus) {
            BiometricStatus.BIOMETRIC_READY -> {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = context.getString(R.string.biometric_prompt_title),
                    subtitle = context.getString(R.string.biometric_prompt_subtitle),
                    negativeButtonText = context.getString(R.string.biometric_prompt_negative),
                    useDeviceCredentialFallback = false,
                    onSuccess = {
                        biometricFeedback = null
                        onBiometricSuccess()
                    },
                    onError = { msg ->
                        biometricFeedback = "生物识别失败：$msg"
                    },
                    onCancelled = {
                        biometricFeedback = "已取消指纹验证，点击下方按钮可重试"
                    }
                )
            }
            BiometricStatus.DEVICE_CREDENTIAL_ONLY -> {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = "密盾 · 第一道系统锁屏验证",
                    subtitle = "设备未录入指纹，请使用锁屏 PIN / 图案验证身份",
                    negativeButtonText = "",
                    useDeviceCredentialFallback = true,
                    onSuccess = {
                        biometricFeedback = null
                        onBiometricSuccess()
                    },
                    onError = { msg ->
                        biometricFeedback = "系统验证失败：$msg"
                    },
                    onCancelled = {
                        biometricFeedback = "已取消身份验证"
                    }
                )
            }
            BiometricStatus.NONE_ENROLLED,
            BiometricStatus.HARDWARE_UNAVAILABLE -> {
                biometricFeedback = "当前设备尚未录入指纹或锁屏密码，请前往系统设置录入"
            }
        }
    }

    // Automatically launch biometric prompt when entering lock screen if ready
    LaunchedEffect(config.biometricEnabled, biometricPassed, biometricStatus) {
        if (config.biometricEnabled && !biometricPassed &&
            (biometricStatus == BiometricStatus.BIOMETRIC_READY ||
                biometricStatus == BiometricStatus.DEVICE_CREDENTIAL_ONLY)
        ) {
            triggerBiometricPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vault_hero),
                    contentDescription = "密盾双重解锁横幅",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (config.biometricEnabled) "双重保险锁定中" else "主密码锁定中",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Layer 1 Card: Biometric / Fingerprint
            val layer1Color by animateColorAsState(
                targetValue = if (layer1Passed) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "layer1Color"
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = layer1Color),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (layer1Passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (layer1Passed) Icons.Rounded.CheckCircle else Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                tint = if (layer1Passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "第 1 道保险 · 指纹 / 生物识别",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        !config.biometricEnabled -> "已在安全中心关闭，直接进入第 2 步"
                                        biometricPassed -> "身份验证已通过"
                                        else -> "请点击验证指纹以开启主密码输入"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (layer1Passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = config.biometricEnabled && !biometricPassed) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            if (biometricFeedback != null) {
                                Text(
                                    text = biometricFeedback ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            if (biometricStatus == BiometricStatus.BIOMETRIC_READY ||
                                biometricStatus == BiometricStatus.DEVICE_CREDENTIAL_ONLY
                            ) {
                                Button(
                                    onClick = { triggerBiometricPrompt() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("biometric_unlock_button")
                                ) {
                                    Icon(imageVector = Icons.Rounded.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (biometricStatus == BiometricStatus.BIOMETRIC_READY) {
                                            "验证指纹解锁第一道防线"
                                        } else {
                                            "验证系统锁屏密码"
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                // Emulator / device without any fingerprint or lock screen enrolled
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AmberWarning.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "检测到当前设备尚未录入指纹或锁屏密码，无法调用系统生物识别。请先前往系统安全设置录入指纹，或使用主密码继续。",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("录入指纹")
                                    }
                                    Button(
                                        onClick = { onBiometricSuccess() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("biometric_hardware_fallback_button")
                                    ) {
                                        Text("前往主密码")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Layer 2 Card: Master Password Verification
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (layer1Passed) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (layer1Passed) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = if (layer1Passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "第 2 道保险 · 主密码解密验证",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (layer1Passed) {
                                    "输入主密码以在本地派生密钥并解密保险箱"
                                } else {
                                    "请先完成第 1 步指纹验证"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = masterPassword,
                        onValueChange = { masterPassword = it },
                        enabled = layer1Passed && !isBusy && cooldownRemainingSec == 0,
                        label = { Text("输入主密码") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (layer1Passed && masterPassword.isNotEmpty() && cooldownRemainingSec == 0) {
                                    onVerifyMasterPassword(masterPassword)
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "隐藏主密码" else "显示主密码"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_password_input")
                    )

                    if (config.passwordHint.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showHint = !showHint }) {
                                Text(if (showHint) "隐藏密码提示" else "忘记了？查看密码提示")
                            }
                            if (showHint) {
                                Text(
                                    text = config.passwordHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (cooldownRemainingSec > 0) {
                                    "请等待 $cooldownRemainingSec 秒后重试"
                                } else {
                                    authError
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onVerifyMasterPassword(masterPassword) },
                        enabled = layer1Passed && masterPassword.isNotEmpty() && !isBusy && cooldownRemainingSec == 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("unlock_vault_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("正在校验并解密...")
                        } else {
                            Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("验证主密码并打开保险箱", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onSignOut) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("切换或退出 Google 账号")
            }
        }
    }
}

@Composable
fun StrengthMeterBar(
    score: Int,
    label: String,
    entropyBits: Int,
    modifier: Modifier = Modifier
) {
    val progress = (score / 4f).coerceIn(0f, 1f)
    val barColor = when (score) {
        1 -> MaterialTheme.colorScheme.error
        2 -> AmberWarning
        3 -> MaterialTheme.colorScheme.secondary
        4 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "密码强度：$label",
                style = MaterialTheme.typography.labelSmall,
                color = barColor
            )
            Text(
                text = "$entropyBits bits 熵值",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
