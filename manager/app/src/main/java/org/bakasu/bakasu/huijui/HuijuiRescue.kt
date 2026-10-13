package org.bakasu.bakasu.huijui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Build

@Composable
fun HuijuiRescue(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var dirs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember { mutableStateOf<File?>(null) }
    var running by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val root = Environment.getExternalStorageDirectory()
            dirs = root.listFiles { f ->
                f.isDirectory && f.name.startsWith("HuijuiSU_Backup_")
            }?.sortedByDescending { it.name } ?: emptyList()
        }
    }

    if (showConfirm && selected != null) {
        val f = selected!!
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("⚠️ 确认刷回？") },
            text = {
                Text(
                    "即将把「${f.name}」中的 vbmeta / init_boot / boot 刷回当前设备。\n\n" +
                    "刷回顺序：vbmeta → init_boot → boot（A/B 双槽位）。\n\n" +
                    "⚠️ 此操作有风险，如果当前系统版本与备份底包不匹配，可能导致无法开机。\n\n" +
                    "请确认已完整备份当前分区，并了解救砖流程。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    scope.launch {
                        running = true
                        log = "开始刷回 ${f.name} ...\n"
                        val r = doRescueSuspend(f) { line ->
                            withContext(Dispatchers.Main) { log += line + "\n" }
                        }
                        log += if (r.isSuccess) "✓ 完成，请手动重启到系统" else "✗ ${r.exceptionOrNull()?.message}"
                        running = false
                    }
                }) { Text("确认刷回") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            },
        )
    }

    Dialog(onDismissRequest = { if (!running) onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                HuijuiDialogHeader(
                    icon = Icons.TwoTone.Build,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "一键救砖",
                    subtitle = "选择历史备份目录，一键 dd 刷回。顺序：vbmeta → init_boot → boot",
                )
                Spacer(Modifier.height(16.dp))

                if (dirs.isEmpty()) {
                    Text("未找到任何备份目录", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                    Text("请先在「一键备份包」中创建备份", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("选择备份目录：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        dirs.forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected == f, onClick = { selected = f })
                                Text(
                                    f.name,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                if (log.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        log,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .height(140.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { if (!running) onDismiss() }) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = selected != null && !running,
                        onClick = { showConfirm = true },
                    ) { Text("开始刷回") }
                }
            }
        }
    }
}

private suspend fun doRescueSuspend(
    dir: File,
    log: suspend (String) -> Unit,
): Result<Unit> = withContext(Dispatchers.IO) {
    val order = listOf("vbmeta_a", "vbmeta_b", "init_boot_a", "init_boot_b", "boot_a", "boot_b")
    for (name in order) {
        val img = File(dir, "$name.img")
        if (!img.exists()) {
            log("跳过 $name（无文件）")
            continue
        }
        log("刷入 $name ...")
        val r = runAsRoot("dd if=${img.absolutePath} of=/dev/block/by-name/$name bs=4096 2>&1")
        if (r.contains("No such file", true) || r.contains("denied", true) || r.contains("error", true)) {
            return@withContext Result.failure(Exception("$name 失败：${r.take(120)}"))
        }
    }
    Result.success(Unit)
}

private fun runAsRoot(cmd: String): String = try {
    val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
    val out = p.inputStream.bufferedReader().readText()
    p.waitFor(60, TimeUnit.SECONDS)
    out
} catch (t: Throwable) { t.message ?: "" }
