package org.bakasu.bakasu.huijui.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.huijui.UltraDialogWrapper
import java.io.File

data class FlashTask(val uri: Uri, val fileName: String, var partition: String)

@Composable
fun HuijuiFlashDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var flashLog by remember { mutableStateOf("等待添加镜像...") }
    var isFlashing by remember { mutableStateOf(false) }
    var pendingTasks by remember { mutableStateOf(listOf<FlashTask>()) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var allDone by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newTasks = uris.map { uri ->
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "unknown.img"
                val guessPart = fileName.substringBefore(".img").lowercase()
                val partition = if (guessPart in listOf("boot", "init_boot", "dtbo", "vbmeta", "recovery")) guessPart else "boot"
                FlashTask(uri, fileName, partition)
            }
            pendingTasks = pendingTasks + newTasks
            flashLog = "已添加 ${newTasks.size} 个文件"
        }
    }

    UltraDialogWrapper("危险操作：批量刷入", "将多个 .img 镜像写入底层分区", Icons.TwoTone.Warning, onDismiss) {
        Column {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                items(pendingTasks) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task.fileName, modifier = Modifier.weight(1f), maxLines = 1)
                        OutlinedTextField(
                            value = task.partition,
                            onValueChange = { newPart ->
                                val newList = pendingTasks.toMutableList()
                                val index = pendingTasks.indexOf(task)
                                newList[index] = task.copy(partition = newPart.trim())
                                pendingTasks = newList
                            },
                            modifier = Modifier.width(100.dp),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            pendingTasks = pendingTasks.toMutableList().also { it.remove(task) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "移除")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { launcher.launch(arrayOf("application/octet-stream", "image/*")) },
                    enabled = !isFlashing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("➕ 添加 .img")
                }

                Button(
                    onClick = {
                        if (pendingTasks.isEmpty()) return@Button
                        isFlashing = true; allDone = false; flashLog = "开始批量刷入...\n"
                        scope.launch(Dispatchers.IO) {
                            var successCount = 0
                            try {
                                val slot = Shell.cmd("getprop ro.boot.slot_suffix").exec().out.firstOrNull()?.trim() ?: ""
                                for (task in pendingTasks) {
                                    withContext(Dispatchers.Main) { flashLog += "⏳ ${task.fileName} -> ${task.partition} ...\n" }
                                    val tempFile = File(context.cacheDir, "temp_${task.fileName}")
                                    context.contentResolver.openInputStream(task.uri)?.use { input ->
                                        tempFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    val partPathAB = "/dev/block/by-name/${task.partition}${slot}"
                                    val partPathDefault = "/dev/block/by-name/${task.partition}"
                                    val finalPath = if (Shell.cmd("ls $partPathAB").exec().isSuccess) partPathAB else partPathDefault
                                    
                                    val res = Shell.cmd("su -c 'dd if=\"${tempFile.absolutePath}\" of=$finalPath'").exec()
                                    withContext(Dispatchers.Main) {
                                        if (res.isSuccess) { flashLog += "✅ ${task.partition} 成功\n"; successCount++ } else { flashLog += "❌ ${task.partition} 失败\n" }
                                    }
                                    tempFile.delete()
                                }
                                withContext(Dispatchers.Main) { isFlashing = false; allDone = true; flashLog += "🎉 完毕！"; showRebootDialog = true }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { flashLog += "❌ 异常: ${e.message}"; isFlashing = false }
                            }
                        }
                    },
                    enabled = !isFlashing && pendingTasks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isFlashing) "刷入中..." else "⚡ 批量刷入")
                }
            }

            if (allDone) {
                Button(
                    onClick = { Shell.cmd("su -c 'reboot'").exec() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("🔄 立即重启生效")
                }
            }

            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(100.dp)) {
                SelectionContainer {
                    Text(
                        text = flashLog,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("🎉 刷入完成") },
            text = { Text("所有任务已处理完毕，建议立即重启生效。") },
            confirmButton = {
                TextButton(onClick = { Shell.cmd("su -c 'reboot'").exec() }) {
                    Text("立即重启")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("稍后")
                }
            }
        )
    }
}
