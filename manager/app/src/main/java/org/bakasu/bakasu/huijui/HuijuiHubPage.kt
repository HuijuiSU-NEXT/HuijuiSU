package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.SaveAlt
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HuijuiHubPage() {
    var showRescue by remember { mutableStateOf(false) }
    var showSusfs by remember { mutableStateOf(false) }

    // 弹窗挂载
    if (showRescue) RescueBackupDialog(onDismiss = { showRescue = false })
    if (showSusfs) SusfsDialog(onDismiss = { showSusfs = false })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "HuiJuiSU Ultra 极客工具箱",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 只保留两个最安全的功能
        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.SaveAlt,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "分区备份救援",
                description = "一键备份 boot/dtbo 等核心分区防变砖",
                onClick = { showRescue = true }
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Security,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "SUSFS 深度隐藏",
                description = "内核级 Root 隐藏，躲避检测",
                onClick = { showSusfs = true }
            )
        }
    }
}

@Composable
fun HuijuiToolCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
