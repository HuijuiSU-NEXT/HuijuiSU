package org.bakasu.bakasu.huijui.feature

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import org.bakasu.bakasu.huijui.feature.HuijuiFlashCustom
import org.bakasu.bakasu.huijui.ui.HuijuiDialogHeader

/**
 * HuiJuiSU NEXT 定制内核刷入
 *
 * 用户选自己的 boot.img（built-in 内核），Root 权限 dd 刷入，
 * 自动处理 vbmeta 校验防止回滚。全程不需要电脑。
 */
@Composable
fun HuijuiFlashCustom(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(0L) }
    var initBootUri by remember { mutableStateOf<Uri?>(null) }
    var initBootName by remember { mutableStateOf("") }
    var currentSlot by remember { mutableStateOf("?") }
    var kernelRelease by remember { mutableStateOf("") }
    var vbmetaState by remember { mutableStateOf("?") }
    var log by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            currentSlot = readCurrentSlot()
            kernelRelease = runShell("uname -r").trim()
            vbmetaState = runShell("getprop ro.boot.veritymode").trim().ifBlank { "unknown" }
        }
    }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch(Dispatchers.IO) {
                try {
                    selectedName = uri.lastPathSegment?.substringAfterLast('/') ?: "boot.img"
                    val tmp = File(context.cacheDir, "flash_custom_tmp.img")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { out -> input.copyTo(out) }
                    }
                    selectedSize = tmp.length()
                } catch (t: Throwable) {
                    log = "读取文件失败：${t.message}\n"
                }
            }
        }
    }

    val pickInitBoot = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            initBootUri = uri
            initBootName = uri.lastPathSegment?.substringAfterLast('/') ?: "init_boot.img"
        }
    }

    val sizeMb = selectedSize / 1024.0 / 1024.0
    val fileType = when {
        selectedSize == 0L -> null
        selectedName.endsWith(".ko", ignoreCase = true) -> "lkm"
        selectedSize < 1024L * 1024L -> "lkm"
        selectedSize < 10L * 1024L * 1024L -> "small"
        else -> "kernel"
    }

    if (showConfirm && selectedUri != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(if (fileType == "lkm") "⚠️ 确认安装 LKM 模块？" else "⚠️ 确认刷入定制内核？") },
            text = {
                if (fileType == "lkm") {
                    Text(
                        "源文件：$selectedName (${"%.1f".format(sizeMb)} MB)\n\n" +
                            "这是 LKM 模式安装，将使用 ksud boot-patch 修补当前设备的 boot/init_boot 分区。\n\n" +
                            "操作步骤：\n" +
                            "1. 读取当前设备 boot 分区\n" +
                            "2. 把 .ko 注入到 boot 镜像\n" +
                            "3. 刷回修补后的 boot 分区\n\n" +
                            "重启后 LKM 生效，功能与刷入 built-in 内核一致。"
                    )
                } else {
                    Text(
                        "目标分区：/dev/block/by-name/boot_$currentSlot\n" +
                            "源文件：$selectedName (${"%.1f".format(sizeMb)} MB)\n" +
                            (if (initBootUri != null) "init_boot：$initBootName\n" else "") +
                            "\n操作步骤：\n" +
                            "1. 备份当前 boot_$currentSlot 到 /sdcard/\n" +
                            "2. dd 刷入新内核到 boot_$currentSlot\n" +
                            (if (initBootUri != null) "3. 刷回原厂 init_boot（清 LKM 残留）\n" else "") +
                            "4. 尝试关闭 vbmeta 校验\n\n" +
                            "如果刷入的内核不兼容，重启后可能无法开机。\n" +
                            "请确认你已经备份了关键分区。"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    running = true
                    scope.launch {
                        if (fileType == "lkm") {
                            doLkmInstall(context, selectedUri!!) { line ->
                                withContext(Dispatchers.Main) { log += line + "\n" }
                            }
                        } else {
                            doFlash(context, selectedUri!!, initBootUri, currentSlot) { line ->
                                withContext(Dispatchers.Main) { log += line + "\n" }
                            }
                        }
                        running = false
                    }
                }) { Text(if (fileType == "lkm") "确认安装" else "确认刷入") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            })
    }

    Dialog(onDismissRequest = { if (!running) onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                HuijuiDialogHeader(
                    icon = Icons.TwoTone.Build,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "刷入定制内核",
                    subtitle = "built-in 模式专用")
                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("当前设备信息", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("内核版本：$kernelRelease",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace)
                        Text("活跃槽位：$currentSlot",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace)
                        Text("vbmeta：$vbmetaState",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { pickFile.launch("*/*") },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedUri == null) "选择 boot.img" else "重新选择")
                }

                if (selectedUri != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("已选择：$selectedName", style = MaterialTheme.typography.bodyMedium)
                    Text("大小：${"%.2f".format(sizeMb)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    when (fileType) {
                        "lkm" -> Text("📦 检测到 LKM 模块（.ko），将走 LKM 安装流程",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        "small" -> Text("⚠️ 文件偏小，可能不是完整的 boot.img",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { pickInitBoot.launch("*/*") },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (initBootUri == null)
                            "选择原厂 init_boot.img（推荐）"
                        else
                            "重新选择 init_boot"
                    )
                }

                if (initBootUri != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("init_boot：$initBootName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("刷入后会自动清除 LKM 残留",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text("⚠️ 如果之前装过 LKM 模式，强烈建议提供原厂 init_boot.img",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(12.dp))

                if (log.isNotEmpty()) {
                    Text(log,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.height(140.dp).verticalScroll(rememberScrollState()))
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { if (!running) onDismiss() }) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = selectedUri != null && !running && (fileType == "kernel" || fileType == "lkm"),
                        onClick = { showConfirm = true }) { Text(if (running) "刷入中..." else "开始刷入") }
                }
            }
        }
    }
}

private suspend fun readCurrentSlot(): String = withContext(Dispatchers.IO) {
    val out = runShell("getprop ro.boot.slot_suffix").trim()
    when (out) {
        "_a" -> "a"
        "_b" -> "b"
        else -> {
            val cmdline = try { File("/proc/cmdline").readText() } catch (_: Throwable) { "" }
            when {
                cmdline.contains("androidboot.slot_suffix=_a") -> "a"
                cmdline.contains("androidboot.slot_suffix=_b") -> "b"
                else -> "a"
            }
        }
    }
}

private suspend fun doFlash(
    context: Context,
    uri: Uri,
    initBootUri: Uri?,
    slot: String,
    onLog: suspend (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val tmp = File(context.cacheDir, "flash_custom_tmp.img")
        if (!tmp.exists()) {
            onLog("源文件不存在")
            return@withContext Result.failure(Exception("源文件不存在"))
        }

        val backupDir = File(
            Environment.getExternalStorageDirectory(),
            "HuiJuiSU_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}")
        backupDir.mkdirs()
        onLog("备份目录：${backupDir.absolutePath}")
        val bootImg = File(backupDir, "boot_${slot}.img")
        onLog("备份 boot_$slot ...")
        val backupOut = runShell("dd if=/dev/block/by-name/boot_$slot of=${bootImg.absolutePath} bs=4096 2>&1")
        if (backupOut.contains("error", true) || backupOut.contains("denied", true)) {
            onLog("备份失败：${backupOut.take(120)}")
            return@withContext Result.failure(Exception("备份失败"))
        }
        onLog("✓ 已备份到 ${bootImg.absolutePath}")

        onLog("刷入 boot_$slot ...")
        val flashOut = runShell("dd if=${tmp.absolutePath} of=/dev/block/by-name/boot_$slot bs=4096 2>&1")
        if (flashOut.contains("error", true) || flashOut.contains("denied", true)) {
            onLog("刷入失败：${flashOut.take(120)}")
            return@withContext Result.failure(Exception("刷入失败"))
        }
        onLog("✓ 刷入完成")

        // 检查 init_boot 分区（Android 13+）
        val hasInitBoot = File("/dev/block/by-name/init_boot_$slot").exists()
        if (hasInitBoot) {
            onLog("")
            onLog("检测到 init_boot_$slot 分区（Android 13+）")
            val initBootBackup = File(backupDir, "init_boot_${slot}_before_flash.img")
            onLog("备份当前 init_boot ...")
            runShell("dd if=/dev/block/by-name/init_boot_$slot of=${initBootBackup.absolutePath} bs=4096 2>&1")
            onLog("✓ 已备份到 ${initBootBackup.absolutePath}")

            if (initBootUri != null) {
                onLog("刷入原厂 init_boot（清除 LKM 残留）...")
                val initTmp = File(context.cacheDir, "flash_init_boot_tmp.img")
                try {
                    context.contentResolver.openInputStream(initBootUri)?.use { input ->
                        initTmp.outputStream().use { out -> input.copyTo(out) }
                    }
                } catch (t: Throwable) {
                    onLog("读取 init_boot 失败：${t.message}")
                }
                if (initTmp.exists() && initTmp.length() > 0) {
                    val initFlashOut = runShell(
                        "dd if=${initTmp.absolutePath} of=/dev/block/by-name/init_boot_$slot bs=4096 2>&1"
                    )
                    if (initFlashOut.contains("error", true) || initFlashOut.contains("denied", true)) {
                        onLog("⚠️ init_boot 刷入失败：${initFlashOut.take(120)}")
                    } else {
                        onLog("✓ init_boot 已刷回原厂，LKM 残留已清除")
                    }
                }
            } else {
                onLog("")
                onLog("⚠️ 未提供原厂 init_boot.img")
                onLog("   如果之前装过 LKM，新内核可能无法正常初始化")
                onLog("   请手动刷回原厂 init_boot，或者用一键救砖恢复")
            }
        }

        onLog("")
        onLog("处理 vbmeta 校验 ...")
        val avbOut = runShell("avbctl disable-verification 2>&1")
        if (avbOut.contains("error", true) || avbOut.contains("not found", true)
            || avbOut.contains("inaccessible", true)) {
            onLog("⚠️ avbctl 不可用，跳过 vbmeta 处理")
            onLog("  如果重启后内核没变，可能是 vbmeta 回滚了")
        } else {
            onLog("✓ vbmeta 校验已关闭")
        }

        onLog("")
        onLog("刷入完成，请手动重启到系统")
        onLog("重启后执行 su -c \"uname -r\" 验证内核版本是否变化")

        Result.success(Unit)
    } catch (t: Throwable) {
        onLog("异常：${t.message}")
        Result.failure(t)
    }
}

private fun runShell(cmd: String): String =
    HuijuiShellUtils.runAsRoot(cmd, timeoutSeconds = 120)

/**
 * LKM 模式安装：把用户提供的 .ko 模块注入到当前设备的 boot 分区
 *
 * 调用 ksud boot-patch -f -m <lkm_path>
 * -f 表示用当前设备读到的 boot/init_boot
 * -m 指定 lkm 模块路径
 */
private suspend fun doLkmInstall(
    context: Context,
    uri: Uri,
    onLog: suspend (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val tmp = File(context.cacheDir, "lkm_install_tmp.ko")
        onLog("复制 .ko 到临时目录 ...")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> input.copyTo(out) }
            }
        } catch (t: Throwable) {
            onLog("读取文件失败：${t.message}")
            return@withContext Result.failure(t)
        }

        if (!tmp.exists() || tmp.length() == 0L) {
            onLog("临时文件写入失败")
            return@withContext Result.failure(Exception("临时文件写入失败"))
        }
        onLog("✓ 大小 ${tmp.length() / 1024} KB")

        onLog("")
        onLog("调用 ksud boot-patch -f -m ...")
        val out = runShell("ksud boot-patch -f -m ${tmp.absolutePath} 2>&1")
        onLog(out.trim())

        if (out.contains("error", true) || out.contains("denied", true)
            || out.contains("failed", true)) {
            onLog("")
            onLog("⚠️ 安装可能失败，请检查上方日志")
            return@withContext Result.failure(Exception("ksud 返回错误"))
        }

        onLog("")
        onLog("✓ LKM 安装完成")
        onLog("重启后生效。重启后执行 su -c \"uname -r\" 确认内核版本")
        Result.success(Unit)
    } catch (t: Throwable) {
        onLog("异常：${t.message}")
        Result.failure(t)
    }
}
