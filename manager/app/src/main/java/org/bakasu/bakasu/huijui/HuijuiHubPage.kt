package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.SaveAlt
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.ui.theme.blurEffect // 导入项目自带的模糊特效

@Composable
fun HuijuiHubPage(bottomPadding: Dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showRescue by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    if (showRescue) RescueBackupDialog(onDismiss = { showRescue = false })

    // 统一的结果弹窗
    if (toastMessage != null) {
        AlertDialog(
            onDismissRequest = { toastMessage = null },
            title = { Text("系统提示") },
            text = { Text(toastMessage!!) },
            confirmButton = {
                TextButton(onClick = { toastMessage = null }) { Text("确定") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "HuiJuiSU 极客工具箱",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 1. 核心救援
        item {
            GlassToolCard(
                icon = Icons.TwoTone.SaveAlt,
                title = "分区备份救援",
                description = "一键备份 boot/dtbo 等核心分区防变砖",
                onClick = { showRescue = true }
            )
        }

        // 2. 重启 SystemUI
        item {
            GlassToolCard(
                icon = Icons.TwoTone.Refresh,
                title = "重启 SystemUI",
                description = "专治状态栏卡死、界面掉帧，一键刷新界面",
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val res = Shell.cmd("pkill -f com.android.systemui").exec()
                        withContext(Dispatchers.Main) {
                            toastMessage = if (res.isSuccess) "SystemUI 重启指令已发送！屏幕可能会闪烁一下。" else "重启失败，请检查 Root 权限。"
                        }
                    }
                }
            )
        }

        // 3. 极速清理内存
        item {
            GlassToolCard(
                icon = Icons.TwoTone.CleaningServices,
                title = "极速清理内存",
                description = "底层 drop_caches，强制回收系统缓存，释放 RAM",
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val res = Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec()
                        withContext(Dispatchers.Main) {
                            toastMessage = if (res.isSuccess) "内存缓存已极速清理完毕！" else "清理失败，请检查 Root 权限。"
                        }
                    }
                }
            )
        }
    }
}

// 全新定制的“液态玻璃”卡片，保留高级质感
@Composable
fun GlassToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .blurEffect(), // 注入核心的模糊特效！
        shape = RoundedCornerShape(20.dp), // 大圆角，更贴合现代 UI
        colors = CardDefaults.cardColors(
            // 半透明背景，让壁纸透过来
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
