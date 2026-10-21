package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.twotone.CleaningServices
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.data.module.ModuleBackupManager
import com.topjohnwu.superuser.Shell
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuijuiHubPage(bottomPadding: Dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showRescue by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    var showBackupSheet by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<File>>(emptyList()) }

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
                            toastMessage = if (res.isSuccess) "SystemUI 重启指令已发送！" else "重启失败，请检查 Root 权限。"
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

        // 4. 模块备份
        item {
            HuijuiToolCard(
                icon = Icons.Default.Archive,
                title = "模块备份与还原",
                description = "防止模块变砖，随时打包或恢复已安装的模块",
                onClick = {
                    backupList = ModuleBackupManager.getBackupList(context)
                    showBackupSheet = true
                }
            )
        }
    }

    if (showRescue) {
        // 假设原来有这个弹窗
        // RescueBackupDialog(onDismiss = { showRescue = false })
    }

    if (showBackupSheet) {
        ModalBottomSheet(onDismissRequest = { showBackupSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                Text("本地模块备份", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (backupList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("暂无备份文件", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(backupList) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(file.name, fontWeight = FontWeight.Medium)
                                    Text(String.format("%.2f MB", file.length() / (1024.0 * 1024.0)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Row {
                                    Button(onClick = {
                                        scope.launch {
                                            val result = ModuleBackupManager.restoreBackup(file)
                                            if (result.isSuccess) {
                                                showBackupSheet = false
                                            }
                                        }
                                    }) { Text("还原") }
                                    
                                    IconButton(onClick = {
                                        ModuleBackupManager.deleteBackup(file)
                                        backupList = ModuleBackupManager.getBackupList(context)
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HuijuiToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
