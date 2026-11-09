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
import androidx.compose.material.icons.twotone.FolderDelete
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.Build
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.RestartAlt
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

/**
 * 极客工具箱。
 *
 * 这一版重做的原因：
 * 1. 每张卡片都有可见的执行结果 —— 之前的按钮只写状态不显示，点了像没反应
 * 2. 卡片按「能干啥」排序，重启这类风险操作放最后并二次确认
 * 3. 结果直接显示在卡片副标题上，Toast 只是补充
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuijuiHubPage(bottomPadding: Dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<File>>(emptyList()) }
    var showRebootConfirm by remember { mutableStateOf(false) }
    var showChunqiuConfirm by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var busyKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    fun runTool(key: String, working: String, done: String, block: suspend () -> Boolean) {
        if (busyKey != null) return
        busyKey = key
        status = status + (key to working)
        scope.launch {
            val ok = try {
                // 最长等 150 秒；超时也要回到 UI，否则会一直显示「正在…」
                kotlinx.coroutines.withTimeoutOrNull(150_000L) {
                    withContext(Dispatchers.IO) {
                        runCatching { block() }.getOrElse { false }
                    }
                } ?: false
            } catch (t: Throwable) {
                false
            }
            val msg = if (ok) done else "失败：命令没有返回（可能未授权 root，或执行超时）"
            status = status + (key to msg)
            busyKey = null
            toastMessage = msg
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "HuiJuiSU 极客工具箱",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Security,
                title = "Root 状态",
                description = status["root"] ?: "查看 root 是否真的可用（功能提示无 root 时先看这里）",
                onClick = {
                    busyKey = "root"
                    status = status + ("root" to "检测中…")
                    scope.launch {
                        val detail = withContext(Dispatchers.IO) {
                            runCatching {
                                val r = Shell.cmd(
                                    "echo APP_UID=$(id -u); " +
                                        "command -v su || echo NO_SU; " +
                                        "su -c 'echo SU_UID=$(id -u)' 2>&1; " +
                                        "ls /data/adb/ksu/bin/ksud 2>/dev/null || true",
                                ).exec()
                                val out = (r.out + r.err).joinToString(" ").replace("\n", " ").trim()
                                "code=${r.code} " + out.take(90)
                            }.getOrElse { "检测失败：${it.message}" }
                        }
                        status = status + ("root" to detail)
                        busyKey = null
                        toastMessage = "Root 状态已刷新"
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.SaveAlt,
                title = "备份 boot 分区",
                description = status["boot"] ?: "防变砖：把当前 boot 镜像导出到 Download",
                onClick = {
                    runTool("boot", "正在备份 boot 分区…", "已备份到 Download") {
                        val ts = System.currentTimeMillis()
                        val out = "/storage/emulated/0/Download/boot_backup_$ts.img"
                        val cmd = """
                            B=${'$'}(readlink -f /dev/block/by-name/boot 2>/dev/null || echo /dev/block/by-name/boot)
                            [ -e "${'$'}B" ] || { echo NO_BOOT; exit 1; }
                            dd if=${'$'}B of='$out' bs=4M 2>/dev/null && echo "OK '$out'"
                        """.trimIndent()
                        Shell.cmd(cmd).exec().out.joinToString("\n").contains("OK ")
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Refresh,
                title = "重启 SystemUI",
                description = status["sysui"] ?: "专治状态栏卡死、界面掉帧，一键刷新界面",
                onClick = {
                    runTool("sysui", "正在重启 SystemUI…", "SystemUI 已重启") {
                        Shell.cmd("pkill -f com.android.systemui").exec().isSuccess
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.CleaningServices,
                title = "清理内存缓存",
                description = status["mem"] ?: "drop_caches 回收系统缓存，释放 RAM",
                onClick = {
                    runTool("mem", "正在清理…", "内存缓存已清理") {
                        Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec().isSuccess
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.FolderDelete,
                title = "清理临时文件",
                description = status["tmp"] ?: "清空 /data/local/tmp 与模块安装残留",
                onClick = {
                    runTool("tmp", "正在清理…", "临时文件已清理") {
                        Shell.cmd(
                            "rm -rf /data/local/tmp/* 2>/dev/null; " +
                                "rm -rf /data/adb/modules_update/* 2>/dev/null; true",
                        ).exec().isSuccess
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.Default.Archive,
                title = "模块备份与还原",
                description = "防止模块变砖，随时打包或恢复已安装的模块",
                onClick = {
                    backupList = ModuleBackupManager.getBackupList(context)
                    showBackupSheet = true
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Shield,
                title = "一键隐藏",
                description = status["hide"] ?: "安装内置隐藏模块（恋雪 v1.4 super）并尝试热激活",
                onClick = {
                    busyKey = "hide"
                    status = status + ("hide" to "正在安装隐藏模块…")
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions
                                    .installHideBundle(context)
                            }.getOrElse { org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions.Result(false, "失败：${it.message}") }
                        }
                        status = status + ("hide" to r.message)
                        busyKey = null
                        toastMessage = r.message
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Search,
                title = "Evil Service 自查",
                description = status["evil"] ?: "扫描常见 Evil Service 痕迹，输出结果摘要",
                onClick = {
                    busyKey = "evil"
                    status = status + ("evil" to "正在扫描…")
                    scope.launch {
                        val summary = withContext(Dispatchers.IO) {
                            runCatching {
                                val out = org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions
                                    .runEvilCheck(context)
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions
                                    .summarizeEvilCheck(out)
                            }.getOrElse { "失败：${it.message}" }
                        }
                        status = status + ("evil" to summary)
                        busyKey = null
                        toastMessage = summary
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Layers,
                title = "元模块状态",
                description = status["meta"] ?: "查看 Hybrid Mount 运行时挂载状态",
                onClick = {
                    busyKey = "meta"
                    status = status + ("meta" to "查询中…")
                    scope.launch {
                        val st = withContext(Dispatchers.IO) {
                            runCatching {
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions.metaStatus()
                            }.getOrElse { "失败：${it.message}" }
                        }
                        status = status + ("meta" to st.take(80))
                        busyKey = null
                        toastMessage = "元模块状态已刷新"
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Extension,
                title = "安装元模块",
                description = status["metainstall"] ?: "安装内置 Hybrid Mount（模块挂载的基础）",
                onClick = {
                    busyKey = "metainstall"
                    status = status + ("metainstall" to "正在安装元模块…")
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions
                                    .installMetaModule(context)
                            }.getOrElse {
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions.Result(false, "失败：${it.message}")
                            }
                        }
                        status = status + ("metainstall" to r.message)
                        busyKey = null
                        toastMessage = r.message
                    }
                },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.Build,
                title = "春秋词条修复",
                description = status["chunqiu"] ?: "运行内置修复脚本（会修改系统属性，需确认）",
                onClick = { showChunqiuConfirm = true },
            )
        }

        item {
            HuijuiToolCard(
                icon = Icons.TwoTone.RestartAlt,
                title = "重启设备",
                description = status["reboot"] ?: "改完内核参数或模块后，重启最稳妥",
                onClick = { showRebootConfirm = true },
            )
        }
    }

    if (showChunqiuConfirm) {
        AlertDialog(
            onDismissRequest = { showChunqiuConfirm = false },
            title = { Text("运行春秋词条修复？") },
            text = { Text("脚本会修改系统属性以规避检测。\n建议先备份 boot 分区，出问题可回滚。") },
            confirmButton = {
                TextButton(onClick = {
                    showChunqiuConfirm = false
                    busyKey = "chunqiu"
                    status = status + ("chunqiu" to "正在执行…")
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions
                                    .runChunqiuFix(context)
                            }.getOrElse {
                                org.bakasu.bakasu.huijui.data.HuijuiBuiltinActions.Result(false, "失败：${it.message}")
                            }
                        }
                        status = status + ("chunqiu" to r.message)
                        busyKey = null
                        toastMessage = r.message
                    }
                }) { Text("执行") }
            },
            dismissButton = {
                TextButton(onClick = { showChunqiuConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text("重启设备？") },
            text = { Text("会立即重启，未保存的操作会中断。") },
            confirmButton = {
                TextButton(onClick = {
                    showRebootConfirm = false
                    runTool("reboot", "正在重启…", "重启指令已发送") {
                        Shell.cmd("svc power reboot || reboot").exec().isSuccess
                    }
                }) { Text("重启") }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) { Text("取消") }
            },
        )
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
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(file.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        String.format("%.2f MB", file.length() / (1024.0 * 1024.0)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                    )
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
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
