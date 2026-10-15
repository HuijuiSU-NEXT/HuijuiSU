package org.bakasu.bakasu.huijui.feature

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.SaveAlt
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import org.bakasu.bakasu.huijui.data.HuijuiModuleSnapshot
import org.bakasu.bakasu.huijui.ui.HuijuiDialogHeader

data class BackupProgress(
    val name: String,
    val status: String,
    val size: String = "",
)

@Composable
fun HuijuiBackup(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<List<BackupProgress>>(emptyList()) }
    var outDir by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    fun start() {
        scope.launch {
            running = true
            done = false
            errorMsg = ""
            progress = emptyList()
            val result = doBackupSuspend(context) { p ->
                withContext(Dispatchers.Main) { progress = p }
            }
            if (result.isSuccess) {
                outDir = result.getOrNull().orEmpty()
                done = true
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "备份失败"
            }
            running = false
        }
    }

    Dialog(onDismissRequest = { if (!running) onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                HuijuiDialogHeader(
                    icon = Icons.TwoTone.SaveAlt,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "一键备份包",
                    subtitle = "boot / init_boot / vbmeta → /sdcard/HuijuiSU_Backup_日期/",
                )
                Spacer(Modifier.height(16.dp))

                if (progress.isNotEmpty()) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        progress.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (sym, color) = when (p.status) {
                                    "成功" -> "✓" to MaterialTheme.colorScheme.primary
                                    "失败" -> "✗" to MaterialTheme.colorScheme.error
                                    "备份中" -> "…" to MaterialTheme.colorScheme.tertiary
                                    else -> "○" to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(sym, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                                Text(p.name, modifier = Modifier.width(110.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                Text(p.status, style = MaterialTheme.typography.bodySmall)
                                if (p.size.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(p.size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                } else if (!running) {
                    Text(
                        "将备份当前槽位 boot / init_boot / vbmeta。\n完成后同一目录会生成 restore.bat（PC 端 fastboot 一键刷回）。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (done) {
                    Spacer(Modifier.height(10.dp))
                    Text("完成 → $outDir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { if (!running) onDismiss() }) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = !running, onClick = { start() }) {
                        Text(if (done) "重新备份" else "开始备份")
                    }
                }
            }
        }
    }
}

private suspend fun doBackupSuspend(
    context: Context,
    onProgress: suspend (List<BackupProgress>) -> Unit,
): Result<String> = withContext(Dispatchers.IO) {
    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val dir = File(Environment.getExternalStorageDirectory(), "HuijuiSU_Backup_$date")
    if (!dir.exists() && !dir.mkdirs()) {
        return@withContext Result.failure(Exception("无法创建目录 ${dir.absolutePath}"))
    }

    val items = mutableListOf<BackupProgress>()
    onProgress(items.toList())

    val partitions = listOf("boot_a", "boot_b", "init_boot_a", "init_boot_b", "vbmeta_a", "vbmeta_b")
    for (part in partitions) {
        items += BackupProgress(part, "备份中")
        onProgress(items.toList())

        val src = "/dev/block/by-name/$part"
        val dst = File(dir, "$part.img")
        val exists = File(src).exists()
        if (!exists) {
            items[items.lastIndex] = items.last().copy(status = "跳过", size = "分区不存在")
            onProgress(items.toList())
            continue
        }
        runAsRoot("dd if=$src of=${dst.absolutePath} bs=4096 2>&1")
        val ok = dst.exists() && dst.length() > 0
        items[items.lastIndex] = items.last().copy(
            status = if (ok) "成功" else "失败",
            size = if (ok) humanSize(dst.length()) else "",
        )
        onProgress(items.toList())
    }

    // 模块快照
    items += BackupProgress("modules", "备份中")
    onProgress(items.toList())
    try {
        val snapshot = HuijuiModuleSnapshot.capture()
        val ok = HuijuiModuleSnapshot.writeToFile(snapshot, dir)
        items[items.lastIndex] = items.last().copy(
            status = if (ok) "成功" else "失败",
            size = "${snapshot.modules.size} 个模块",
        )
    } catch (t: Throwable) {
        items[items.lastIndex] = items.last().copy(
            status = "失败",
            size = t.message.orEmpty().take(20),
        )
    }
    onProgress(items.toList())

    File(dir, "restore.bat").writeText(buildRestoreBat())
    Result.success(dir.absolutePath)
}

private fun buildRestoreBat(): String = buildString {
    appendLine("@echo off")
    appendLine("chcp 65001 > nul")
    appendLine("echo HuijuiSU 一键还原脚本")
    appendLine("echo 请确保手机已进入 fastboot 模式并连接电脑")
    appendLine("pause")
    listOf("vbmeta_a", "vbmeta_b", "init_boot_a", "init_boot_b", "boot_a", "boot_b").forEach { name ->
        appendLine("if exist \"%~dp0$name.img\" fastboot flash $name \"%~dp0$name.img\"")
    }
    appendLine("fastboot reboot")
    appendLine("pause")
}

private fun runAsRoot(cmd: String): String =
    HuijuiShellUtils.runAsRoot(cmd, timeoutSeconds = 60)

private fun humanSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}
