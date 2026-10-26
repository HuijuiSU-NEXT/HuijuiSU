package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.data.module.ModuleBackupManager
import com.topjohnwu.superuser.Shell
import java.io.File
import android.widget.Toast
import org.bakasu.bakasu.huijui.ui.HuijuiFlashDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuijuiHubPage(bottomPadding: Dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showRescue by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<File>>(emptyList()) }
    var showFlashDialog by remember { mutableStateOf(false) }

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
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showRescue = true
                    android.widget.Toast.makeText(context, "救援功能正在接入中", android.widget.Toast.LENGTH_SHORT).show() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                HuijuiToolCard(
                    icon = Icons.TwoTone.SaveAlt,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "分区备份救援",
                    description = "一键备份 boot/dtbo 等核心分区防变砖",
                    onClick = { 
                        showRescue = true
                    }
                )
            }
        }

        // 2. 重启 SystemUI
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { scope.launch(Dispatchers.IO) { val res = Shell.cmd("pkill -f com.android.systemui").exec(); withContext(Dispatchers.Main) { Toast.makeText(context, if (res.isSuccess) "已发送重启指令" else "重启失败", Toast.LENGTH_SHORT).show() } } },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                HuijuiToolCard(
                    icon = Icons.TwoTone.Refresh,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "重启 SystemUI",
                    description = "专治状态栏卡死、界面掉帧，一键刷新界面",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val res = Shell.cmd("pkill -f com.android.systemui").exec()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, if (res.isSuccess) "重启指令已发送" else "重启失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // 3. 极速清理内存
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { scope.launch(Dispatchers.IO) { val res = Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec(); withContext(Dispatchers.Main) { Toast.makeText(context, if (res.isSuccess) "内存已清理" else "清理失败", Toast.LENGTH_SHORT).show() } } },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                HuijuiToolCard(
                    icon = Icons.TwoTone.CleaningServices,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "极速清理内存",
                    description = "底层 drop_caches，强制回收系统缓存，释放 RAM",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val res = Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, if (res.isSuccess) "内存已清理" else "清理失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // 4. 模块备份
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { backupList = ModuleBackupManager.getBackupList(context)
                    showBackupSheet = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                HuijuiToolCard(
                    icon = Icons.Default.Archive,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "模块备份与还原",
                    description = "防止模块变砖，随时打包或恢复已安装的模块",
                    onClick = {
                        backupList = ModuleBackupManager.getBackupList(context)
                        showBackupSheet = true
                    }
                )
            }
        }

        // 5. 批量刷入镜像
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showFlashDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                HuijuiToolCard(
                    icon = Icons.TwoTone.SaveAlt,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "批量刷入镜像",
                    description = "支持多选 .img 文件，自动识别分区刷入",
                    onClick = { showFlashDialog = true }
                )
            }
        }
    }

    if (showRescue) {
        android.widget.Toast.makeText(context, "弹窗准备中", android.widget.Toast.LENGTH_SHORT).show()
    }

    if (showFlashDialog) {
        HuijuiFlashDialog(onDismiss = { showFlashDialog = false })
    }

    if (showBackupSheet) {
        ModalBottomSheet(onDismissRequest = { showBackupSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("本地模块备份", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = Shell.cmd("ls /data/adb/modules").exec()
                                val moduleNames = result.out.filter { it.isNotBlank() && !it.startsWith(".") }
                                var count = 0
                                moduleNames.forEach { name ->
                                    val res = ModuleBackupManager.backupModule(name, context)
                                    if (res.isSuccess) count++
                                }
                                withContext(Dispatchers.Main) {
                                    backupList = ModuleBackupManager.getBackupList(context)
                                    Toast.makeText(context, "成功备份 $count 个模块！", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Text("一键备份所有")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(backupList) { file ->
                        ListItem(
                            headlineContent = { Text(file.name) },
                            supportingContent = { Text("${file.length() / 1024 / 1024} MB") },
                            trailingContent = { 
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}