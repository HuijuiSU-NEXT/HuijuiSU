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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.device.KsuModeDetector
import com.huijui.nova.device.KsuStatus
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 4.x（非 GKI）专用模式。
 *
 * 4.x 内核不能加载 LKM，KernelSU 只支持「内核补丁」路线。这个页面把
 * 那条路线需要的东西摆在一起：确认内核、修补 boot、以及自己编内核时的命令。
 */
@Composable
fun MiuixLegacy4xScreen(
    onBack: () -> Unit,
    onOpenPatch: () -> Unit,
) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFFB08A2E)

    var status by remember { mutableStateOf(KsuStatus()) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { status = KsuModeDetector.detect() }

    val guide = """
# 1. 拉取设备内核源码（以你机型的官方仓库为准）
git clone --depth=1 <kernel-repo> -b <branch> kernel && cd kernel

# 2. 应用 KernelSU 补丁（本仓库 kernel/ 目录提供 patch）
curl -LSs "https://raw.githubusercontent.com/HuijuiSU-NEXT/HuijuiSU/main/kernel/setup.sh" | bash -s main

# 3. 用设备原本的编译脚本构建 boot.img
#    （AOSP / 厂商 kernel 的 build.sh 各不相同，按仓库 README 走）

# 4. 刷入
fastboot flash boot out/arch/arm64/boot/Image.gz-dtb
""".trimIndent()

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
                    Text("4.x 兼容模式", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        if (status.kernelSeries > 0) "当前内核 ${status.kernelSeries}.x" else "未识别到内核版本",
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("非 GKI", fontSize = 10.sp, color = accent)
                }
            }

            Spacer(Modifier.height(14.dp))

            // 为什么
            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(16.dp)) {
                Text("为什么 4.x 用不了 LKM", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "LKM 方案依赖 GKI 内核（5.10+，带 KMI 与模块化构建）。4.x 是单体内核，" +
                        "没有可加载的 kernelsu.ko，只能把 KernelSU 编进内核镜像或修补 boot。",
                    fontSize = 11.sp,
                    color = dim,
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            // 两条路
            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(16.dp)) {
                Text("两条可行路线", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth().clickable { onOpenPatch() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("1", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("修补现成 boot.img", fontSize = 13.sp, color = ink, fontWeight = FontWeight.Medium)
                        Text("有官方 boot 镜像时最快，ksud 直接改包", fontSize = 10.sp, color = dim)
                    }
                    Text("去修补", fontSize = 11.sp, color = accent)
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("2", fontSize = 12.sp, color = dim, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("编内核补丁", fontSize = 13.sp, color = ink, fontWeight = FontWeight.Medium)
                        Text("有内核源码时最彻底，见下方命令", fontSize = 10.sp, color = dim)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 编译指引
            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("内核编译流程", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ink, modifier = Modifier.weight(1f))
                    Text(
                        if (copied) "已复制" else "复制",
                        fontSize = 11.sp,
                        color = accent,
                        modifier = Modifier
                            .clickable {
                                runCatching {
                                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("ksu", guide))
                                    copied = true
                                }
                            }
                            .padding(6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B2027))
                        .padding(12.dp),
                ) {
                    Text(
                        guide,
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD6DEE8),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                Text(
                    "在这个模式下，模块管理会显示不可用：非 GKI 的 KernelSU 不加载模块。",
                    fontSize = 10.sp,
                    color = dim,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
