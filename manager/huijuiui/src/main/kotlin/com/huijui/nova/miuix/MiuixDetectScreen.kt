package com.huijui.nova.miuix

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.detect.ChunqiuCheck
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 春秋检测页。
 *
 * 脚本原样内置，这里只负责：跑、把输出按 `[FOUND]/[ ok ]/[ note]/[nn]` 分类、
 * 命中项标红、给出结论。检测逻辑一行都没搬进 Kotlin——作者换脚本不用改 App。
 */
@Composable
fun MiuixDetectScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = LocalHuiColors.current.accent
    val ink = novaInk()
    val dim = novaDim()

    var report by remember { mutableStateOf(ChunqiuCheck.Report()) }
    var running by remember { mutableStateOf(false) }
    var quiet by remember { mutableStateOf(false) }
    val failed = !running && report.groups == 0 && report.lines.any { it.kind == ChunqiuCheck.Kind.Plain }

    fun start() {
        if (running) return
        running = true
        kotlinx.coroutines.MainScope().launch {
            report = ChunqiuCheck.run(context, quiet = quiet)
            running = false
        }
    }

    LaunchedEffect(Unit) { start() }

    MiuixTheme(colors = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
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
                    Text(S("春秋检测"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        when {
                            running -> "检测中…"
                            failed -> "无法执行（需要 Root）"
                            else -> "共 ${report.groups} 组 · 命中 ${report.hits} 项"
                        },
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Text(
                    if (running) "…" else "重测",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { start() },
                )
            }

            Spacer(Modifier.height(12.dp))

            // 结论卡
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                corner = 18.dp,
                padding = PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    failed -> Color(0xFFB08A2E)
                                    report.hits == 0 -> Color(0xFF2E9E6B)
                                    else -> Color(0xFFD9534F)
                                },
                            ),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                failed -> "没能跑起来"
                                report.hits == 0 -> "环境较干净"
                                else -> "命中 ${report.hits} 项特征"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ink,
                        )
                        Text(
                            when {
                                failed -> "模拟器/未授权时跑不了，真机上才有结果"
                                report.hits == 0 -> "同类检测看不到明显痕迹"
                                else -> "下面标红的是被看到的地方"
                            },
                            fontSize = 10.sp,
                            color = dim,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${report.hits}", fontSize = 26.sp, fontWeight = FontWeight.Light, color = ink)
                        Text("命中", fontSize = 10.sp, color = dim)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                report.lines.forEach { line ->
                    when (line.kind) {
                        ChunqiuCheck.Kind.Section -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "· ${line.text}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = dim,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        ChunqiuCheck.Kind.Found -> LineRow("✕", line.text, Color(0xFFD9534F))
                        ChunqiuCheck.Kind.Ok -> LineRow("✓", line.text, Color(0xFF2E9E6B))
                        ChunqiuCheck.Kind.Note -> LineRow("·", line.text, dim)
                        ChunqiuCheck.Kind.Plain -> {
                            if (line.text.isNotBlank() && line.text.length < 90) {
                                Text(
                                    line.text,
                                    fontSize = 10.sp,
                                    color = dim,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(90.dp))
            }
        }
    }
}

@Composable
private fun LineRow(symbol: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(symbol, fontSize = 11.sp, color = color, modifier = Modifier.width(16.dp))
        Text(
            text,
            fontSize = 11.sp,
            color = color,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
