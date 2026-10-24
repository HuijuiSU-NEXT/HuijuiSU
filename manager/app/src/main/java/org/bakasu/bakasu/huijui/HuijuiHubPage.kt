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
import org.bakasu.bakasu.huijui.ui.HuijuiFlashDialog
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
        // 5. 批量刷入镜像
        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.SaveAlt,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "批量刷入镜像",
                description = "支持多选 .img 文件，自动识别分区刷入",
                onClick = { showFlashDialog = true }
            )
        }
    }


    if (showFlashDialog) {
        HuijuiFlashDialog(
            onDismiss = { showFlashDialog = false }
        )
    }

    if (showRescue) {
        // 假设原来有这个弹窗
        // RescueBackupDialog(onDismiss = { showRescue = false })
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
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Text(
                        text = "本地模块备份",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // 🚀 新增：一键备份所有模块按钮
                    androidx.compose.material3.Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val installedModules = java.io.File("/data/adb/modules").listFiles()
                                    var count = 0
                                    installedModules?.forEach { dir ->
                                        if (dir.isDirectory && !dir.name.startsWith(".")) {
                                            val res = org.bakasu.bakasu.data.module.ModuleBackupManager.backupModule(dir.name, context)
                                            if (res.isSuccess) count++
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        backupList = org.bakasu.bakasu.data.module.ModuleBackupManager.getBackupList(context)
                                        android.widget.Toast.makeText(context, "成功备份 $count 个模块！", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "备份失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        androidx.compose.material3.Text("一键备份所有")
                    }
                }
                
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                if (backupList.isEmpty()) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text("还没有任何备份，点击右上角一键备份吧~", color = androidx.compose.ui.graphics.Color.Gray)
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
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, "还原指令已发送", android.widget.Toast.LENGTH_SHORT).show()
                                                    showBackupSheet = false
                                                }
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
