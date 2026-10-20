package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.SaveAlt
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HuijuiHubPage(bottomPadding: Dp) {
    var showRescue by remember { mutableStateOf(false) }
    var showSusfs by remember { mutableStateOf(false) }

    if (showRescue) RescueBackupDialog(onDismiss = { showRescue = false })
    if (showSusfs) SusfsDialog(onDismiss = { showSusfs = false })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "HuiJuiSU Ultra 极客工具箱",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 直接调用项目原本的 HuijuiToolCard，完美继承所有项目内样式，绝不冲突！
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
