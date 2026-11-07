package com.huijui.nova.miuix

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.LocalNovaDark
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 简易终端。
 *
 * 不是完整 PTY（那要 JNI + forkpty），而是「每条命令起一次 su -c」：
 * 对刷机场景够用（ls / cat / getprop / ksud / dd），而且不会因为
 * 交互式 shell 退出把界面卡住。
 *
 * 有 root 走 su，没有就退回 sh，命令失败照样把 stderr 打出来。
 */
@Composable
fun MiuixTerminalScreen(onBack: () -> Unit) {
    val ink = novaInk()
    val dim = novaDim()
    val dark = LocalNovaDark.current

    var lines by remember { mutableStateOf(listOf("Nova Shell · 输入命令后回车，su 不可用时自动退回 sh")) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun run(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty() || busy) return
        busy = true
        lines = (lines + "› $cmd").takeLast(200)
        MainScope().launch {
            // 先 su，失败退回 sh：没 root 时至少能跑只读命令
            var result = HideBundle.run(cmd)
            if (result.output.isEmpty() || result.output.first().startsWith("执行失败")) {
                val plain = runCatching {
                    val p = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
                    val out = mutableListOf<String>()
                    p.inputStream.bufferedReader().forEachLine { out += it }
                    val code = p.waitFor()
                    com.huijui.nova.hide.HideBundle.Result(code, out)
                }.getOrNull()
                if (plain != null) {
                    lines = (lines + "（su 不可用，已用普通 sh 执行）").takeLast(200)
                    result = plain
                }
            }
            val output = result.output.ifEmpty { listOf("（无输出，退出码 ${result.exitCode}）") }
            lines = (lines + output).takeLast(200)
            busy = false
        }
    }

    MiuixTheme(colors = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding(),
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
                    Text(S("终端"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(if (busy) "执行中…" else "su -c，逐条执行", fontSize = 11.sp, color = dim)
                }
                Text(
                    S("清屏"),
                    fontSize = 12.sp,
                    color = dim,
                    modifier = Modifier
                        .clickable { lines = emptyList() }
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // 快捷命令
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("id", "getprop ro.build.version.release", "ls /data/adb").forEach { quick ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ink.copy(alpha = 0.06f))
                            .clickable { run(quick) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(quick, fontSize = 10.sp, color = dim, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 输出区
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (dark) Color(0xFF0B0F14) else Color(0xFF1B2027),
                    )
                    .padding(12.dp),
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    lines.forEach { line ->
                        Text(
                            line,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.startsWith("›")) Color(0xFF7FD1C0) else Color(0xFFD6DEE8),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 输入行
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ink.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$", fontSize = 12.sp, color = dim, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ink,
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text("输入命令…", fontSize = 13.sp, color = dim.copy(alpha = 0.6f))
                        }
                        inner()
                    },
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ink.copy(alpha = 0.10f))
                        .clickable { run(input); input = "" }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(S("运行"), fontSize = 11.sp, color = ink)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
