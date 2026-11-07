package com.huijui.nova.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private data class LicenseItem(
    val name: String,
    val license: String,
    val desc: String,
    val url: String = "",
)

private val runtimeItems = listOf(
    LicenseItem("Miuix", "Apache-2.0", "HyperOS 风格 Compose 组件", "https://github.com/compose-miuix-ui/miuix"),
    LicenseItem("Jetpack Compose", "Apache-2.0", "界面渲染", "https://developer.android.com/jetpack/compose"),
    LicenseItem("AndroidX", "Apache-2.0", "基础 Android 能力", "https://developer.android.com/jetpack/androidx"),
    LicenseItem("Kotlin / coroutines", "Apache-2.0", "语言运行时与协程", "https://kotlinlang.org"),
    LicenseItem("libsu", "Apache-2.0", "root shell 交互", "https://github.com/topjohnwu/libsu"),
)

private val assetItems = listOf(
    LicenseItem("内置动态壁纸", "维护者提供", "assets/nova/miku_star.mp4"),
    LicenseItem("春秋检测脚本", "维护者提供", "assets/detect/chunqiu_check.sh"),
    LicenseItem("一键隐藏套件", "模块作者提供", "hide-bundle-v2.0.zip · 仅限自用"),
)

private val upstreamItems = listOf(
    LicenseItem("KernelSU", "GPL-3.0", "内核模块与 ksud 原始实现", "https://github.com/tiann/KernelSU"),
    LicenseItem("BakaSU", "GPL-3.0", "本仓库直接上游", "https://github.com/Baka-SU/BakaSU"),
)

/**
 * 开源许可页：一个正规管理器必须能说清楚用了谁的东西、按什么许可分发。
 */
@Composable
fun MiuixLicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

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
                    Text(S("开源许可"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(S("随 APK 分发的组件与来源"), fontSize = 11.sp, color = dim)
                }
            }

            Spacer(Modifier.height(14.dp))

            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("GPL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("HuiJuiSU", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink)
                        Text(S("本项目以 GPL-3.0 发布"), fontSize = 10.sp, color = dim)
                    }
                    Text(
                        S("查看"),
                        fontSize = 11.sp,
                        color = accent,
                        modifier = Modifier
                            .clickable {
                                openExternal(
                                    context,
                                    "https://github.com/HuijuiSU-NEXT/HuijuiSU/blob/main/LICENSE",
                                )
                            }
                            .padding(6.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Group("运行时组件", dim) { runtimeItems.forEach { LicenseRow(it, ink, dim, accent, context) } }
            Spacer(Modifier.height(10.dp))
            Group("内置资源", dim) { assetItems.forEach { LicenseRow(it, ink, dim, accent, context) } }
            Spacer(Modifier.height(10.dp))
            Group("上游项目", dim) { upstreamItems.forEach { LicenseRow(it, ink, dim, accent, context) } }

            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                Text(
                    "完整清单见仓库 docs/THIRD_PARTY_NOTICES.md。隐藏套件内含的 Zygisk Next、" +
                        "OhMyKeymint 等组件有各自授权限制，本项目不为其再分发授权背书。",
                    fontSize = 10.sp,
                    color = dim,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
private fun Group(title: String, dim: Color, content: @Composable () -> Unit) {
    Text(title, fontSize = 11.sp, color = dim, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
    GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(6.dp)) { content() }
}

@Composable
private fun LicenseRow(
    item: LicenseItem,
    ink: Color,
    dim: Color,
    accent: Color,
    context: android.content.Context,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (item.url.isNotBlank()) Modifier.clickable { openExternal(context, item.url) } else Modifier)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
            Text(item.desc, fontSize = 9.sp, color = dim)
        }
        Text(item.license, fontSize = 9.sp, color = accent)
        if (item.url.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text("↗", fontSize = 10.sp, color = dim)
        }
    }
}

private fun openExternal(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
