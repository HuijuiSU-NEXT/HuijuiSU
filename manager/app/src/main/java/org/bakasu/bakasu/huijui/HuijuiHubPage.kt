package org.bakasu.bakasu.huijui

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Archive
import androidx.compose.ui.platform.LocalContext
import org.bakasu.bakasu.data.module.ModuleBackupManager
import java.io.File
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuijuiHubPage(bottomPadding: Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showBackupSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var backupList by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<java.io.File>>(emptyList()) }
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
            HuijuiToolCard(
                icon = Icons.TwoTone.SaveAlt,
                title = "分区备份救援",
                description = "一键备份 boot/dtbo 等核心分区防变砖",
                onClick = { showRescue = true }
            )
        }

        // 2. 重启 SystemUI
        item {
            HuijuiToolCard(
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
            HuijuiToolCard(
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
        
        // 🚀 4. 新增：模块备份与还原
        item {
            HuijuiToolCard(
                icon = androidx.compose.material.icons.Icons.Default.Archive,
                title = "模块备份与还原",
                description = "防止模块变砖，随时打包或恢复已安装的模块",
                onClick = {
                    backupList = org.bakasu.bakasu.data.module.ModuleBackupManager.getBackupList(context)
                    showBackupSheet = true
                }
            )
        }

        }

    }
}

// 全新定制的“液态玻璃”卡片，保留高级质感
@Composable
fun HuijuiToolCard(
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
    
    if (showBackupSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showBackupSheet = false }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .padding(16.dp)
                    .fillMaxHeight(0.8f)
            ) {
                androidx.compose.material3.Text(
                    text = "本地模块备份",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                if (backupList.isEmpty()) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text("暂无备份文件", color = androidx.compose.ui.graphics.Color.Gray)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                        items(backupList) { file ->
                            androidx.compose.foundation.layout.Row(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.layout.Column {
                                    androidx.compose.material3.Text(file.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                    androidx.compose.material3.Text(
                                        String.format("%.2f MB", file.length() / (1024.0 * 1024.0)),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.Gray
                                    )
                                }
                                androidx.compose.foundation.layout.Row {
                                    androidx.compose.material3.Button(onClick = {
                                        scope.launch {
                                            val result = org.bakasu.bakasu.data.module.ModuleBackupManager.restoreBackup(file)
                                            if (result.isSuccess) {
                                                showBackupSheet = false
                                            }
                                        }
                                    }) { androidx.compose.material3.Text("还原") }

                                    androidx.compose.material3.IconButton(onClick = {
                                        org.bakasu.bakasu.data.module.ModuleBackupManager.deleteBackup(file)
                                        backupList = org.bakasu.bakasu.data.module.ModuleBackupManager.getBackupList(context)
                                    }) {
                                        androidx.compose.material3.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                            contentDescription = "删除"
                                        )
                                    }
                                }
                            }
                            androidx.compose.material3.HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}