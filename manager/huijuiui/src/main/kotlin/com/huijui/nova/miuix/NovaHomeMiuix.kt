package com.huijui.nova.miuix

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.components.HuiIcon
import com.huijui.nova.components.HuiIcons
import com.huijui.nova.device.rememberDeviceSnapshot
import com.huijui.nova.design.novaInk
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.LocalNovaUiMode
import com.huijui.nova.design.NovaUiMode
import com.huijui.nova.design.LocalHuiColors
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 重制版首页：HyperOS 风格的浅色平面。
 *
 * 和之前那套「玻璃」彻底分家：
 * - 不做模糊、不做折射、不叠加三层蒙版（那些在低端 GPU 上全是功耗）；
 * - 卡片是**半透明白**，文字是深墨色，对比度靠色差而不是靠描边；
 * - 圆角、间距、字号全部来自 Miuix 的 HyperOS 规范。
 */
@Composable
fun NovaHomeMiuixScreen(modifier: Modifier = Modifier) {
    val device = rememberDeviceSnapshot()
    var ksu by remember { androidx.compose.runtime.mutableStateOf(com.huijui.nova.device.KsuStatus()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        ksu = com.huijui.nova.device.KsuModeDetector.detect()
    }
    val ink = novaInk()
    val dim = novaDim()
    val accent = LocalHuiColors.current.accent
    

    MiuixTheme(colors = lightColorScheme()) {
        val cardColor = Color.White.copy(alpha = 0.68f)
        val ink = Color(0xFF101820)
        val dim = Color(0xFF4A5A68)

        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // 时间
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    val cal = java.util.Calendar.getInstance()
                    Text(
                        "%02d:%02d".format(
                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                            cal.get(java.util.Calendar.MINUTE),
                        ),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) Color.White else ink,
                    )
                    Text(
                        "%d月%d日".format(
                            java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
                            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH),
                        ),
                        fontSize = 12.sp,
                        color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) Color.White.copy(alpha = 0.8f) else dim,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Nova Edition",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) Color.White else ink,
                    )
                    Text(
                        device.model,
                        fontSize = 11.sp,
                        color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) Color.White.copy(alpha = 0.7f) else dim,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 主卡
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                corner = 22.dp,
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(50))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            HuiIcon(HuiIcons.Shield, size = 20.dp, tint = accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Root 已激活", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ink)
                            Text(
                                "Android ${device.androidRelease} · ${device.abi}",
                                fontSize = 11.sp,
                                color = dim,
                            )
                        }
                        Text("86%", fontSize = 26.sp, fontWeight = FontWeight.Light, color = ink)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("内核", device.kernel.substringBefore("-").ifEmpty { "?" }, Modifier.weight(1f), ink, dim)
                        MiniStat("内存", device.ramLabel, Modifier.weight(1f), ink, dim)
                        MiniStat("核心", "${device.cores} 核", Modifier.weight(1f), ink, dim)
                    }

                    Spacer(Modifier.height(12.dp))

                    // 工作模式：未工作 / LKM / GKI / 非 GKI
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when (ksu.mode) {
                                        com.huijui.nova.device.KsuMode.Lkm,
                                        com.huijui.nova.device.KsuMode.Gki -> Color(0xFF2E9E6B)
                                        com.huijui.nova.device.KsuMode.Legacy -> Color(0xFFB08A2E)
                                        com.huijui.nova.device.KsuMode.Unknown -> Color(0xFF7A8899)
                                        com.huijui.nova.device.KsuMode.NotWorking -> Color(0xFFD9534F)
                                    },
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "工作模式 · ${ksu.mode.label}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ink,
                            )
                            Text(ksu.mode.desc, fontSize = 9.sp, color = dim)
                        }
                        if (ksu.detail.isNotBlank()) {
                            Text(ksu.detail, fontSize = 9.sp, color = dim, maxLines = 1)
                        }
                    }
                }
            }

            // 两张指标卡
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    corner = 20.dp,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("电池温度", fontSize = 11.sp, color = dim)
                        Spacer(Modifier.height(4.dp))
                        Text("31.4°", fontSize = 28.sp, fontWeight = FontWeight.Light, color = ink)
                        Text("正常", fontSize = 11.sp, color = accent)
                    }
                }
                GlassCard(
                    modifier = Modifier.weight(1f),
                    corner = 20.dp,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("已装模块", fontSize = 11.sp, color = dim)
                        Spacer(Modifier.height(4.dp))
                        Text("7", fontSize = 28.sp, fontWeight = FontWeight.Light, color = ink)
                        Text("+1 待更新", fontSize = 11.sp, color = accent)
                    }
                }
            }

            // 快捷操作
            Text(
                "快捷操作",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) {
                    Color.White.copy(alpha = 0.85f)
                } else {
                    dim
                },
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(HuiIcons.Download, "刷入内核", "payload.bin / boot.img", accent, ink, dim, cardColor, Modifier.weight(1f))
                ActionCard(HuiIcons.Shield, "隐藏中心", "SUSFS · 挂载隔离", accent, ink, dim, cardColor, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(HuiIcons.Module, "模块仓库", "1416 个模块在线", accent, ink, dim, cardColor, Modifier.weight(1f))
                ActionCard(HuiIcons.Terminal, "终端", "临时 Root Shell", accent, ink, dim, cardColor, Modifier.weight(1f))
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier, ink: Color, dim: Color) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, color = dim)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
    }
}

@Composable
private fun ActionCard(
    icon: com.huijui.nova.components.HuiGlyph,
    title: String,
    subtitle: String,
    accent: Color,
    ink: Color,
    dim: Color,
    cardColor: Color,
    modifier: Modifier,
) {
    GlassCard(
        modifier = modifier,
        corner = 20.dp,
        padding = PaddingValues(16.dp),
    ) {
        Column {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                HuiIcon(icon, size = 18.dp, tint = accent)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink)
            Text(subtitle, fontSize = 10.sp, color = dim)
        }
    }
}
