package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.crypto.VaultCryptoManager
import com.example.data.DecryptedVaultItem
import com.example.data.VaultConfigDoc
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

@Composable
fun PasswordGeneratorScreen(
    onCopyPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var lengthSlider by remember { mutableFloatStateOf(16f) }
    var includeUpper by remember { mutableStateOf(true) }
    var includeLower by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }

    var generatedPassword by remember {
        mutableStateOf(VaultCryptoManager.generatePassword(16, true, true, true, true))
    }

    fun regenerate() {
        generatedPassword = VaultCryptoManager.generatePassword(
            length = lengthSlider.roundToInt(),
            includeUpper = includeUpper,
            includeLower = includeLower,
            includeDigits = includeDigits,
            includeSymbols = includeSymbols
        )
    }

    val strength = remember(generatedPassword) {
        VaultCryptoManager.evaluateStrength(generatedPassword)
    }

    val letterColor = MaterialTheme.colorScheme.onSurface
    val digitColor = MaterialTheme.colorScheme.secondary
    val symbolColor = MaterialTheme.colorScheme.primary

    val highlightedPassword = remember(generatedPassword, letterColor, digitColor, symbolColor) {
        buildAnnotatedString {
            generatedPassword.forEach { ch ->
                val color = when {
                    ch.isDigit() -> digitColor
                    !ch.isLetterOrDigit() -> symbolColor
                    else -> letterColor
                }
                withStyle(SpanStyle(color = color)) {
                    append(ch)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "高强度密码生成器",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "基于加密级随机数生成，避免规律与重复",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = highlightedPassword,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.testTag("generated_password_text")
                )
                Spacer(modifier = Modifier.height(14.dp))
                StrengthMeterBar(
                    score = strength.score,
                    label = strength.label,
                    entropyBits = strength.entropyBits
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { regenerate() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("regenerate_password_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("换一个")
                    }
                    Button(
                        onClick = { onCopyPassword(generatedPassword) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_generated_password_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("安全复制")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("密码长度", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${lengthSlider.roundToInt()} 位",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = lengthSlider,
                    onValueChange = {
                        lengthSlider = it
                        regenerate()
                    },
                    valueRange = 8f..32f,
                    steps = 23
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                GeneratorSwitchRow("大写字母 (A-Z)", includeUpper) {
                    if (it || includeLower || includeDigits || includeSymbols) {
                        includeUpper = it
                        regenerate()
                    }
                }
                GeneratorSwitchRow("小写字母 (a-z)", includeLower) {
                    if (it || includeUpper || includeDigits || includeSymbols) {
                        includeLower = it
                        regenerate()
                    }
                }
                GeneratorSwitchRow("数字 (0-9)", includeDigits) {
                    if (it || includeUpper || includeLower || includeSymbols) {
                        includeDigits = it
                        regenerate()
                    }
                }
                GeneratorSwitchRow("特殊符号 (!@#\$...)", includeSymbols) {
                    if (it || includeUpper || includeLower || includeDigits) {
                        includeSymbols = it
                        regenerate()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun GeneratorSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SecurityCenterScreen(
    config: VaultConfigDoc,
    userEmail: String?,
    health: VaultHealthSummary,
    isBusy: Boolean,
    onUpdateSettings: (biometricEnabled: Boolean, autoLockSeconds: Int, hint: String) -> Unit,
    onChangeMasterPassword: (current: String, newPass: String, newHint: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onOpenItem: (DecryptedVaultItem) -> Unit,
    onLockNow: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    if (showChangePasswordDialog) {
        ChangeMasterPasswordDialog(
            initialHint = config.passwordHint,
            isBusy = isBusy,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, newPass, newHint, onError ->
                onChangeMasterPassword(current, newPass, newHint) { success, errMsg ->
                    if (success) {
                        showChangePasswordDialog = false
                    } else {
                        onError(errMsg ?: "修改失败")
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "安全中心与双重保险",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Password Health Audit Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("密码安全体检", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (health.totalLogins == 0) {
                                "暂无账号密码条目需要体检"
                            } else {
                                "共检测 ${health.totalLogins} 个账号，${health.strongCount} 个达到强密码"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (health.overallScore >= 80) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            AmberWarning.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = "${health.overallScore}分",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = if (health.overallScore >= 80) MaterialTheme.colorScheme.primary else AmberWarning,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (health.weakItems.isNotEmpty() || health.reusedItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    health.weakItems.take(3).forEach { item ->
                        AuditAlertRow(
                            title = item.payload.title,
                            issue = "弱密码建议更换",
                            onClick = { onOpenItem(item) }
                        )
                    }
                    health.reusedItems.take(3).forEach { item ->
                        AuditAlertRow(
                            title = item.payload.title,
                            issue = "与其他账号重复使用密码",
                            onClick = { onOpenItem(item) }
                        )
                    }
                } else if (health.totalLogins > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "全部账号均使用独立强密码",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Dual Insurance Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("双重保险策略", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("指纹 / 生物识别第一道锁", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "建议保持开启，实现指纹 + 主密码双重保护",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = config.biometricEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(
                                enabled,
                                config.autoLockSeconds.toInt(),
                                config.passwordHint
                            )
                        },
                        modifier = Modifier.testTag("settings_biometric_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("切到后台自动锁定时间", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val timeoutOptions = listOf(
                    0 to "立即",
                    30 to "30 秒",
                    60 to "1 分钟",
                    300 to "5 分钟"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeoutOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = config.autoLockSeconds.toInt() == sec,
                            onClick = {
                                onUpdateSettings(
                                    config.biometricEnabled,
                                    sec,
                                    config.passwordHint
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showChangePasswordDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("change_master_password_button")
                ) {
                    Icon(imageVector = Icons.Rounded.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("修改主密码（重新加密全部条目）")
                }
            }
        }

        // Account & Lock Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("当前同步账号", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail ?: "已认证 Google 用户",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLockNow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即锁定")
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("退出登录")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun AuditAlertRow(
    title: String,
    issue: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AmberWarning.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = AmberWarning,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(text = issue, style = MaterialTheme.typography.bodySmall, color = AmberWarning)
            }
            Text(
                text = "去修改",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ChangeMasterPasswordDialog(
    initialHint: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (current: String, newPass: String, newHint: String, onError: (String) -> Unit) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmNewPass by remember { mutableStateOf("") }
    var newHint by remember { mutableStateOf(initialHint) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("修改主密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "修改后将使用新主密码自动重新加密保险箱中的全部条目。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it; errorMsg = null },
                    label = { Text("当前主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; errorMsg = null },
                    label = { Text("新主密码（至少 6 位）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmNewPass,
                    onValueChange = { confirmNewPass = it; errorMsg = null },
                    label = { Text("确认新主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newHint,
                    onValueChange = { if (it.length <= 100) newHint = it },
                    label = { Text("密码提示语（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    when {
                        currentPass.isEmpty() -> errorMsg = "请输入当前主密码"
                        newPass.length < 6 -> errorMsg = "新主密码至少需要 6 位"
                        newPass != confirmNewPass -> errorMsg = "两次输入的新主密码不一致"
                        else -> onConfirm(currentPass, newPass, newHint) { err -> errorMsg = err }
                    }
                }
            ) {
                Text(if (isBusy) "重加密中..." else "确认更换")
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
