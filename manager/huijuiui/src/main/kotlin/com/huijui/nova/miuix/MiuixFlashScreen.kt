package com.huijui.nova.miuix

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.huijui.nova.ui.Text
import com.huijui.nova.ui.MiuixTheme
import com.huijui.nova.ui.lightColorScheme
import java.io.File

private enum class FlashAction(val title: String, val desc: String, val mime: String) {
    Install("安装模块", "选 zip，交给 ksud / magisk 安装", "application/zip"),
    Patch("修补 boot", "选 boot.img / init_boot.img，生成 KernelSU 修补件", "*/*"),
    Flash("刷入镜像", "选 .img 写入分区（危险，二次确认）", "*/*"),
}

/**
 * 刷写页：装模块 / 修补镜像 / 刷入分区。
 *
 * 全部走 SAF 文件选择器 + `su`，命令和输出原样回显。
 * 失败就把 stderr 摆出来——不做「看起来成功」的假界面。
 */
@Composable
fun MiuixFlashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = LocalHuiColors.current.accent
    val ink = novaInk()
    val dim = novaDim()

    var output by remember { mutableStateOf<List<String>>(emptyList()) }
    var running by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf(FlashAction.Install) }
    var confirmFile by remember { mutableStateOf<File?>(null) }
    var kernelHint by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val status = com.huijui.nova.device.KsuModeDetector.detect()
        kernelHint = if (status.kernelSeries in 1..4) {
            "当前内核 ${status.kernelSeries}.x（非 GKI）：LKM 用不了，需要内核补丁或修补 boot 后刷入"
        } else if (status.kernelSeries >= 5) {
            "当前内核 ${status.kernelSeries}.x：走 LKM，可直接装模块或修补 boot"
        } else {
            ""
        }
    }

    fun log(vararg lines: String) {
        output = (output + lines).takeLast(120)
    }

    fun execute(cmd: String) {
        MainScope().launch {
            log("$ $cmd")
            HideBundle.run(cmd) { line -> log(line) }
            running = null
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            running = null
            return@rememberLauncherForActivityResult
        }
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "picked.bin"
        val target = File(context.cacheDir, "nova_$name")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
        }
        log("文件：${target.absolutePath}（${target.length() / 1024} KB）")

        when (action) {
            FlashAction.Install -> {
                val cmd = "ksud module install '${target.absolutePath}'"
                log("$ $cmd")
                execute(cmd)
            }
            FlashAction.Patch -> {
                // 先校验是不是真正的 Android boot 镜像：前 8 字节必须是 ANDROID!
                val magic = runCatching {
                    target.inputStream().use { ins ->
                        val buf = ByteArray(8)
                        val n = ins.read(buf)
                        if (n >= 8) String(buf, 0, 8) else ""
                    }
                }.getOrDefault("")

                if (magic != "ANDROID!") {
                    log("✗ 这不是 Android boot 镜像（头部不是 ANDROID!）")
                    log("  可能是：init_boot / vendor_boot / 解压后的 Image / 别的文件")
                    log("  请选择设备对应的 boot.img 或 init_boot.img")
                    running = null
                    return@rememberLauncherForActivityResult
                }

                // 输出到系统 Download 目录：ksud 以 root 运行，可直接写入
                val out = File(
                    "/storage/emulated/0/Download",
                    "HuiJuiSU_patched_${System.currentTimeMillis()}.img",
                )
                // 加超时：ksud 在部分机型会卡在解包阶段，避免无限等待
                val cmd = "timeout 180 ksud boot-patch -b '${target.absolutePath}' -o '${out.absolutePath}'"
                log("$ $cmd")

                MainScope().launch {
                    val buf = StringBuilder()
                    val r = com.huijui.nova.hide.HideBundle.run(cmd) { line ->
                        buf.append(line).append('\n')
                        log(line)
                    }
                    // 不以退出码为准，直接看产物是否真的生成
                    val produced = out.exists() && out.length() > 1024
                    if (produced) {
                        log("")
                        log("✓ 已保存到：${out.absolutePath}")
                        log("  大小：${out.length() / 1024} KB")
                        log("  刷入前请确认分区（boot / init_boot）")
                    } else {
                        log("")
                        log("✗ 没有生成修补后的镜像")
                        log("  ksud 退出码：${if (r.ok) 0 else 1}，输出见上")
                        log("  若日志停在解包阶段，说明该镜像格式 ksud 不支持（常见于厂商改过内核压缩）")
                    }
                    if (!r.ok) {
                        val text = buf.toString()
                        // 常见失败：内核不是 gzip 压缩（厂商用 lz4/lzma），ksud 的自动识别会失败
                        if (text.contains("invalid gzip header", true) ||
                            text.contains("Failed to get KMI", true)
                        ) {
                            val kmi = com.huijui.nova.hide.HideBundle.run(
                                "uname -r",
                            ).output.firstOrNull()?.trim()?.split("-")?.firstOrNull()?.split(".")?.take(2)
                                ?.joinToString(".") ?: ""
                            log("")
                            log("↓ 自动识别失败，用内核版本 ${kmi} 重试一次")
                            val retry = "ksud boot-patch -b '${target.absolutePath}' -o '${out.absolutePath}' --kmi $kmi"
                            log("$ $retry")
                            com.huijui.nova.hide.HideBundle.run(retry) { line -> log(line) }
                        } else {
                            log("")
                            log("补丁失败。常见原因：")
                            log("· 镜像内核非 gzip 压缩（厂商改过），ksud 无法解包")
                            log("· 镜像来自别的机型 / 分区不对")
                            log("· 4.x 非 GKI 内核：LKM 补丁不适用，需内核补丁")
                        }
                    }
                    running = null
                }
            }
            FlashAction.Flash -> {
                confirmFile = target
                running = null
            }
        }
    }

    fun pick(a: FlashAction) {
        if (running != null) return
        action = a
        running = a.title
        runCatching { picker.launch(arrayOf(a.mime)) }
    }

    MiuixTheme(colors = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    fontSize = 26.sp,
                    color = ink,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(S("刷写"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(running ?: "选文件 → 执行", fontSize = 11.sp, color = dim)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (kernelHint.isNotBlank()) {
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Text(kernelHint, fontSize = 11.sp, color = dim)
                }
                Spacer(Modifier.height(10.dp))
            }

            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(14.dp)) {
                FlashAction.entries.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { pick(item) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink)
                            Text(item.desc, fontSize = 10.sp, color = dim)
                        }
                        Text(S("选择"), fontSize = 11.sp, color = accent)
                    }
                }
            }

            if (output.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Column {
                        output.forEach { line ->
                            Text(
                                line,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = if (line.startsWith("$")) accent else dim,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(90.dp))
        }

        // 刷入确认
        confirmFile?.let { file ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                GlassCard(Modifier.padding(28.dp), corner = 20.dp, padding = PaddingValues(18.dp)) {
                    Text(S("确认刷入？"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "会把 ${file.name} 写进 boot 分区，写错可能无法开机。",
                        fontSize = 11.sp,
                        color = dim,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            S("取消"),
                            fontSize = 12.sp,
                            color = dim,
                            modifier = Modifier
                                .clickable { confirmFile = null }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                        )
                        Text(
                            S("确认刷入"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD9534F),
                            modifier = Modifier
                                .clickable {
                                    confirmFile = null
                                    execute(
                                        "dd if='${file.absolutePath}' of=/dev/block/by-name/init_boot 2>&1 || " +
                                            "dd if='${file.absolutePath}' of=/dev/block/by-name/boot 2>&1",
                                    )
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
