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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.checkup.Checkup
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 一键体检。
 *
 * 评分 + 逐项结论 + 直达修复入口。这项能力是把「清单式管理器」
 * 变成「会告诉你哪里会炸」的关键。
 */
@Composable
fun MiuixCheckupScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    var report by remember { mutableStateOf(Checkup.Report()) }
    var running by remember { mutableStateOf(true) }

    fun start() {
        running = true
        MainScope().launch {
            report = Checkup.run(context)
            running = false
        }
    }

    LaunchedEffect(Unit) { start() }

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
                    Text(S("一键体检"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        if (running) "检查中…" else report.summary,
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Text(
                    S("重测"),
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { if (!running) start() }.padding(8.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // 评分
            GlassCard(Modifier.fillMaxWidth(), corner = 20.dp, padding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    report.score >= 85 -> accent.copy(alpha = 0.16f)
                                    report.score >= 60 -> Color(0xFFB08A2E).copy(alpha = 0.16f)
                                    else -> Color(0xFFD9534F).copy(alpha = 0.16f)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (running) "…" else "${report.score}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Light,
                            color = when {
                                report.score >= 85 -> accent
                                report.score >= 60 -> Color(0xFFB08A2E)
                                else -> Color(0xFFD9534F)
                            },
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                running -> "正在体检"
                                report.score >= 85 -> "整体健康"
                                report.score >= 60 -> "需要关注"
                                else -> "存在风险"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ink,
                        )
                        Text(
                            when {
                                running -> "正在读取内核、模块与检测状态"
                                report.bad > 0 -> "${report.bad} 项严重 · ${report.warn} 项提醒"
                                report.warn > 0 -> "${report.warn} 项提醒，建议处理"
                                else -> "没有发现需要处理的问题"
                            },
                            fontSize = 10.sp,
                            color = dim,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 明细
            report.items.forEach { item ->
                val color = when (item.level) {
                    Checkup.Level.Good -> accent
                    Checkup.Level.Warn -> Color(0xFFB08A2E)
                    Checkup.Level.Bad -> Color(0xFFD9534F)
                    Checkup.Level.Info -> dim
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    corner = 16.dp,
                    padding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(color),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                            Spacer(Modifier.height(2.dp))
                            Text(item.detail, fontSize = 10.sp, color = dim, lineHeight = 15.sp)
                        }
                        if (item.action.isNotBlank()) {
                            Text(
                                item.action,
                                fontSize = 10.sp,
                                color = color,
                                modifier = Modifier
                                    .clickable { onOpen(item.actionKey) }
                                    .padding(start = 8.dp, top = 2.dp),
                            )
                        }
                    }
                }
            }

            if (running) {
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(16.dp)) {
                    Text("正在检查模块冲突、隐藏特征与备份状态…", fontSize = 11.sp, color = dim)
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
