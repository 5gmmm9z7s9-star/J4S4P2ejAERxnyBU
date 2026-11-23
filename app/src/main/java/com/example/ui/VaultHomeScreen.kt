package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DecryptedVaultItem
import com.example.data.VaultCategory
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun VaultHomeScreen(
    items: List<DecryptedVaultItem>,
    totalCount: Int,
    searchQuery: String,
    selectedFilter: VaultFilter,
    healthScore: Int,
    biometricEnabled: Boolean,
    itemsError: String?,
    onSearchChange: (String) -> Unit,
    onFilterChange: (VaultFilter) -> Unit,
    onItemClick: (DecryptedVaultItem) -> Unit,
    onQuickCopy: (DecryptedVaultItem) -> Unit,
    onToggleFavorite: (DecryptedVaultItem) -> Unit,
    onAddClick: () -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
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
                        text = "密盾保险箱",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (biometricEnabled) "已解锁 · 双重保险保护中" else "已解锁 · 主密码保护中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                onClick = onLockNow,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.testTag("lock_now_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "立即锁定",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "锁定",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Compact Vault Stats Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VaultMetricPill(
                label = "加密条目",
                value = totalCount.toString(),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            VaultMetricPill(
                label = "密码健康度",
                value = "$healthScore%",
                accent = if (healthScore >= 80) MaterialTheme.colorScheme.secondary else AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("搜索网站、账号、银行卡或笔记...") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "清空搜索"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (itemsError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = itemsError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            EmptyVaultState(
                hasItemsInVault = totalCount > 0,
                onAddClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    VaultItemCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onQuickCopy = { onQuickCopy(item) },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VaultItemCard(
    item: DecryptedVaultItem,
    onClick: () -> Unit,
    onQuickCopy: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val (icon, tint, containerColor) = categoryVisuals(item.category)
    val subtitle = remember(item) {
        when (item.category) {
            VaultCategory.LOGIN -> item.payload.username.ifBlank { item.payload.url.ifBlank { "账号密码" } }
            VaultCategory.CARD -> {
                val digits = item.payload.cardNumber.filter { it.isDigit() }
                if (digits.length >= 4) "•••• ${digits.takeLast(4)}" else item.payload.cardHolder.ifBlank { "银行卡" }
            }
            VaultCategory.SECURE_NOTE -> item.payload.notes.lineSequence().firstOrNull()?.take(36).orEmpty().ifBlank { "机密笔记" }
            VaultCategory.IDENTITY -> item.payload.idFullName.ifBlank { "重要证件" }
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_item_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.category.displayName,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.payload.title.ifBlank { "未命名条目" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = containerColor.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = item.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_toggle_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (item.favorite) "取消收藏" else "标为收藏",
                    tint = if (item.favorite) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onQuickCopy,
                modifier = Modifier.testTag("quick_copy_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "快速复制核心机密",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyVaultState(
    hasItemsInVault: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = "空保险箱插图",
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasItemsInVault) "没有找到匹配的条目" else "保险箱还是空的",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasItemsInVault) {
                    "请尝试更换搜索关键词或切换分类筛选"
                } else {
                    "添加您的重要账号密码、银行卡、助记词或证件信息，全程本地加密保存"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasItemsInVault) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("empty_add_item_button")
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("存入第一条机密")
                }
            }
        }
    }
}

@Composable
fun categoryVisuals(category: VaultCategory): Triple<ImageVector, Color, Color> {
    return when (category) {
        VaultCategory.LOGIN -> Triple(
            Icons.Rounded.Key,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        )
        VaultCategory.CARD -> Triple(
            Icons.Rounded.CreditCard,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        )
        VaultCategory.SECURE_NOTE -> Triple(
            Icons.Rounded.Description,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
        )
        VaultCategory.IDENTITY -> Triple(
            Icons.Rounded.Badge,
            AmberWarning,
            AmberWarning.copy(alpha = 0.16f)
        )
    }
}
