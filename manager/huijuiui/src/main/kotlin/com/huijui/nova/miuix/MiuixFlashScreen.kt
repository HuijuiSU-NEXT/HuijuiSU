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
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import java.io.File

private enum class FlashAction(val title: String, val desc: String, val mime: String) {
    Install("安装模块", "选 zip，交给 ksud / magisk 安装", "application/zip"),
    Patch("修补镜像", "选 boot.img，ksud boot-patch 生成修补件", "*/*"),
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
                val out = File(context.cacheDir, "patched_${System.currentTimeMillis()}.img")
                val cmd = "ksud boot-patch -b '${target.absolutePath}' -o '${out.absolutePath}'"
                log("$ $cmd")
                execute(cmd)
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
                    Text("刷写", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(running ?: "选文件 → 执行", fontSize = 11.sp, color = dim)
                }
            }

            Spacer(Modifier.height(12.dp))

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
                        Text("选择", fontSize = 11.sp, color = accent)
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
                    Text("确认刷入？", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "会把 ${file.name} 写进 boot 分区，写错可能无法开机。",
                        fontSize = 11.sp,
                        color = dim,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "取消",
                            fontSize = 12.sp,
                            color = dim,
                            modifier = Modifier
                                .clickable { confirmFile = null }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                        )
                        Text(
                            "确认刷入",
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
