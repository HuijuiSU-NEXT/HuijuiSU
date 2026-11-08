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
    
    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    var showBackupSheet by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<File>>(emptyList()) }

    // 关键：之前只写了 toastMessage 状态，从来没有弹出来，
    // 所以这些按钮点起来像“没反应”。这里统一消费一次。
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
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

        // 1. 备份 boot 分区（真实可用：dd 到 Download，失败会明确提示）
        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.SaveAlt,
                title = "备份 boot 分区",
                description = "防变砖：把当前 boot 镜像导出到 Download",
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val ts = System.currentTimeMillis()
                        val out = "/storage/emulated/0/Download/boot_backup_$ts.img"
                        val cmd = """
                            B=${'$'}(readlink -f /dev/block/by-name/boot 2>/dev/null || echo /dev/block/by-name/boot)
                            [ -e "${'$'}B" ] || { echo NO_BOOT; exit 1; }
                            dd if=${'$'}B of='$out' bs=4M 2>/dev/null && echo "OK '$out'"
                        """.trimIndent()
                        val res = Shell.cmd(cmd).exec()
                        val text = res.out.joinToString("\n")
                        withContext(Dispatchers.Main) {
                            toastMessage = when {
                                text.contains("OK ") -> "已备份到 Download"
                                text.contains("NO_BOOT") -> "找不到 boot 分区，可能不是标准布局"
                                else -> "备份失败，请检查 Root 权限"
                            }
                        }
                    }
                }
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
