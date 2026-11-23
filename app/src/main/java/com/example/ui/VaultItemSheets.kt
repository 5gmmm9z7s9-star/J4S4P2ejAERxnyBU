package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.crypto.VaultCryptoManager
import com.example.crypto.VaultItemPayload
import com.example.data.DecryptedVaultItem
import com.example.data.VaultCategory
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VaultItemDetailScreen(
    item: DecryptedVaultItem,
    isBusy: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopySensitive: (label: String, value: String) -> Unit
) {
    BackHandler(onBack = onBack)

    var showDeleteDialog by remember { mutableStateOf(false) }
    val (icon, tint, containerColor) = categoryVisuals(item.category)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认永久删除？") },
            text = { Text("删除「${item.payload.title}」后无法恢复，确定要从保险箱中移除吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回"
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (item.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "收藏",
                            tint = if (item.favorite) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_item_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier.testTag("delete_item_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = containerColor,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.category.displayName,
                                    tint = tint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.payload.title.ifBlank { "未命名条目" },
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = tint
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category-specific detail fields
                val p = item.payload
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (item.category) {
                            VaultCategory.LOGIN -> {
                                DetailFieldRow(
                                    label = "账号 / 用户名",
                                    value = p.username,
                                    isSensitive = false,
                                    useMonospace = false,
                                    onCopy = { onCopySensitive("账号", p.username) }
                                )
                                DetailFieldRow(
                                    label = "登录密码",
                                    value = p.password,
                                    isSensitive = true,
                                    useMonospace = true,
                                    onCopy = { onCopySensitive("密码", p.password) }
                                )
                                if (p.password.isNotEmpty()) {
                                    val strength = remember(p.password) {
                                        VaultCryptoManager.evaluateStrength(p.password)
                                    }
                                    StrengthMeterBar(
                                        score = strength.score,
                                        label = strength.label,
                                        entropyBits = strength.entropyBits,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                DetailFieldRow(
                                    label = "网站 / 应用地址",
                                    value = p.url,
                                    isSensitive = false,
                                    useMonospace = false,
                                    onCopy = { onCopySensitive("网址", p.url) }
                                )
                            }
                            VaultCategory.CARD -> {
                                DetailFieldRow(
                                    label = "持卡人姓名",
                                    value = p.cardHolder,
                                    isSensitive = false,
                                    useMonospace = false,
                                    onCopy = { onCopySensitive("持卡人", p.cardHolder) }
                                )
                                DetailFieldRow(
                                    label = "银行卡号",
                                    value = p.cardNumber,
                                    isSensitive = true,
                                    useMonospace = true,
                                    onCopy = { onCopySensitive("卡号", p.cardNumber) }
                                )
                                DetailFieldRow(
                                    label = "有效期 (MM/YY)",
                                    value = p.cardExpiry,
                                    isSensitive = false,
                                    useMonospace = true,
                                    onCopy = { onCopySensitive("有效期", p.cardExpiry) }
                                )
                                DetailFieldRow(
                                    label = "安全码 (CVV/CVC)",
                                    value = p.cardCvv,
                                    isSensitive = true,
                                    useMonospace = true,
                                    onCopy = { onCopySensitive("CVV", p.cardCvv) }
                                )
                                DetailFieldRow(
                                    label = "取款 / 查询密码 (PIN)",
                                    value = p.cardPin,
                                    isSensitive = true,
                                    useMonospace = true,
                                    onCopy = { onCopySensitive("PIN", p.cardPin) }
                                )
                            }
                            VaultCategory.IDENTITY -> {
                                DetailFieldRow(
                                    label = "姓名",
                                    value = p.idFullName,
                                    isSensitive = false,
                                    useMonospace = false,
                                    onCopy = { onCopySensitive("姓名", p.idFullName) }
                                )
                                DetailFieldRow(
                                    label = "证件号码",
                                    value = p.idNumber,
                                    isSensitive = true,
                                    useMonospace = true,
                                    onCopy = { onCopySensitive("证件号", p.idNumber) }
                                )
                                DetailFieldRow(
                                    label = "有效期限",
                                    value = p.idExpiry,
                                    isSensitive = false,
                                    useMonospace = false,
                                    onCopy = { onCopySensitive("有效期", p.idExpiry) }
                                )
                            }
                            VaultCategory.SECURE_NOTE -> Unit
                        }

                        if (p.notes.isNotBlank()) {
                            DetailFieldRow(
                                label = if (item.category == VaultCategory.SECURE_NOTE) "机密内容" else "安全备注",
                                value = p.notes,
                                isSensitive = item.category == VaultCategory.SECURE_NOTE,
                                useMonospace = item.category == VaultCategory.SECURE_NOTE,
                                onCopy = { onCopySensitive("机密内容", p.notes) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                val updatedDate = item.updatedAt?.toDate() ?: item.createdAt?.toDate()
                if (updatedDate != null) {
                    Text(
                        text = "最后加密更新：${dateFormat.format(updatedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    isSensitive: Boolean,
    useMonospace: Boolean,
    onCopy: () -> Unit
) {
    if (value.isBlank()) return
    var revealed by remember(value) { mutableStateOf(!isSensitive) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (revealed) value else "••••••••••••",
                    style = if (useMonospace) {
                        MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (isSensitive) {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        imageVector = if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (revealed) "隐藏" else "显示",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "复制$label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun VaultItemEditorScreen(
    editingItem: DecryptedVaultItem?,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSave: (category: VaultCategory, payload: VaultItemPayload, favorite: Boolean) -> Unit
) {
    BackHandler(onBack = onBack)

    var category by remember(editingItem) {
        mutableStateOf(editingItem?.category ?: VaultCategory.LOGIN)
    }
    var favorite by remember(editingItem) {
        mutableStateOf(editingItem?.favorite ?: false)
    }

    val initial = editingItem?.payload ?: VaultItemPayload()
    var title by remember(editingItem) { mutableStateOf(initial.title) }
    var username by remember(editingItem) { mutableStateOf(initial.username) }
    var password by remember(editingItem) { mutableStateOf(initial.password) }
    var url by remember(editingItem) { mutableStateOf(initial.url) }
    var notes by remember(editingItem) { mutableStateOf(initial.notes) }
    var cardHolder by remember(editingItem) { mutableStateOf(initial.cardHolder) }
    var cardNumber by remember(editingItem) { mutableStateOf(initial.cardNumber) }
    var cardExpiry by remember(editingItem) { mutableStateOf(initial.cardExpiry) }
    var cardCvv by remember(editingItem) { mutableStateOf(initial.cardCvv) }
    var cardPin by remember(editingItem) { mutableStateOf(initial.cardPin) }
    var idFullName by remember(editingItem) { mutableStateOf(initial.idFullName) }
    var idNumber by remember(editingItem) { mutableStateOf(initial.idNumber) }
    var idExpiry by remember(editingItem) { mutableStateOf(initial.idExpiry) }

    var showSecrets by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "取消"
                        )
                    }
                    Text(
                        text = if (editingItem == null) "新增加密条目" else "编辑加密条目",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = { favorite = !favorite }) {
                    Icon(
                        imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "收藏",
                        tint = if (favorite) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择保存类型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = cat == category,
                            onClick = { category = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = {
                        Text(
                            when (category) {
                                VaultCategory.LOGIN -> "名称（如：微信、GitHub、邮箱）*"
                                VaultCategory.CARD -> "卡片名称（如：招商银行储蓄卡）*"
                                VaultCategory.SECURE_NOTE -> "笔记标题（如：助记词、Wi-Fi 密码）*"
                                VaultCategory.IDENTITY -> "证件名称（如：身份证、护照）*"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                when (category) {
                    VaultCategory.LOGIN -> {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名 / 手机号 / 邮箱") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_username_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                            visualTransformation = if (showSecrets) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecrets = !showSecrets }) {
                                    Icon(
                                        imageVector = if (showSecrets) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "切换密码可见"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_password_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    password = VaultCryptoManager.generatePassword(length = 16)
                                    showSecrets = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("editor_generate_password_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("一键生成 16 位强密码")
                            }
                        }
                        if (password.isNotEmpty()) {
                            val strength = remember(password) {
                                VaultCryptoManager.evaluateStrength(password)
                            }
                            StrengthMeterBar(
                                score = strength.score,
                                label = strength.label,
                                entropyBits = strength.entropyBits
                            )
                        }
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("网站域名或 App 名称（选填）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    VaultCategory.CARD -> {
                        OutlinedTextField(
                            value = cardHolder,
                            onValueChange = { cardHolder = it },
                            label = { Text("持卡人姓名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("银行卡号") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { cardExpiry = it },
                                label = { Text("有效期 (MM/YY)") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { cardCvv = it },
                                label = { Text("安全码 CVV") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = cardPin,
                            onValueChange = { cardPin = it },
                            label = { Text("取款 / 查询密码（选填）") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    VaultCategory.IDENTITY -> {
                        OutlinedTextField(
                            value = idFullName,
                            onValueChange = { idFullName = it },
                            label = { Text("证件姓名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = idNumber,
                            onValueChange = { idNumber = it },
                            label = { Text("证件号码") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = idExpiry,
                            onValueChange = { idExpiry = it },
                            label = { Text("签发 / 有效期限") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    VaultCategory.SECURE_NOTE -> Unit
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = {
                        Text(if (category == VaultCategory.SECURE_NOTE) "机密正文（支持多行）" else "备注信息（选填）")
                    },
                    minLines = if (category == VaultCategory.SECURE_NOTE) 5 else 2,
                    maxLines = 10,
                    textStyle = if (category == VaultCategory.SECURE_NOTE) {
                        MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_notes_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            validationError = "请输入标题或名称"
                            return@Button
                        }
                        val payload = VaultItemPayload(
                            title = title.trim(),
                            username = username.trim(),
                            password = password,
                            url = url.trim(),
                            notes = notes,
                            cardHolder = cardHolder.trim(),
                            cardNumber = cardNumber.trim(),
                            cardExpiry = cardExpiry.trim(),
                            cardCvv = cardCvv.trim(),
                            cardPin = cardPin.trim(),
                            idFullName = idFullName.trim(),
                            idNumber = idNumber.trim(),
                            idExpiry = idExpiry.trim()
                        )
                        onSave(category, payload, favorite)
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_item_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("正在加密保存...")
                    } else {
                        Text("AES-256 加密保存", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
