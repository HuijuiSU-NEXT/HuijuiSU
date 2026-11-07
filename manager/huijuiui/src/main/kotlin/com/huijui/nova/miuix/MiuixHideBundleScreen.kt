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
 * 一键隐藏套件（hide_bundle）接入页。
 *
 * 作者的脚本原样调用，管理器只负责状态、按钮、输出。
 * 界面已经换成和其余页面一致的浅色玻璃。
 */
@Composable
fun MiuixHideBundleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    var status by remember { mutableStateOf(HideBundle.Status()) }
    var output by remember { mutableStateOf<List<String>>(emptyList()) }
    var running by remember { mutableStateOf<String?>(null) }
    var root by remember { mutableStateOf(false) }

    suspend fun refresh() {
        root = HideBundle.hasRoot()
        status = HideBundle.readStatus()
    }

    LaunchedEffect(Unit) { refresh() }

    fun exec(label: String, command: String) {
        if (running != null) return
        running = label
        output = listOf("$ $command")
        MainScope().launch {
            HideBundle.run(command) { line -> output = (output + line).takeLast(120) }
            running = null
            refresh()
        }
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
                    Text(S("一键隐藏套件"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        "Zygisk Next · LSPosed · HMA-OSS · OhMyKeymint",
                        fontSize = 10.sp,
                        color = dim,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                !root -> Color(0xFFD9534F).copy(alpha = 0.14f)
                                status.installed -> accent.copy(alpha = 0.14f)
                                else -> dim.copy(alpha = 0.12f)
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        when {
                            !root -> "无 Root"
                            status.installed -> status.version ?: "已安装"
                            else -> "未安装"
                        },
                        fontSize = 10.sp,
                        color = when {
                            !root -> Color(0xFFD9534F)
                            status.installed -> accent
                            else -> dim
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 组成
            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(14.dp)) {
                HideBundle.subModules.forEach { (id, name, desc) ->
                    val installed = status.subStates.firstOrNull { it.first == id }?.second == true
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (installed) "●" else "○",
                            fontSize = 10.sp,
                            color = if (installed) accent else dim,
                            modifier = Modifier.width(18.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                            Text(desc, fontSize = 9.sp, color = dim)
                        }
                        Text(if (installed) "已装" else "缺失", fontSize = 10.sp, color = if (installed) accent else dim)
                    }
                }
            }

            if (!status.installed) {
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Text(
                        "还没装。可以直接用下面的「安装内置套件」——包已经在 APK 里，不用再找 zip。",
                        fontSize = 11.sp,
                        color = dim,
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .clickable {
                                if (running == null) {
                                    running = "安装内置包"
                                    output = listOf("$ 从 APK 内置资源安装")
                                    MainScope().launch {
                                        HideBundle.installBundled(
                                            context = context,
                                            onLine = { line -> output = (output + line).takeLast(120) },
                                        )
                                        running = null
                                        refresh()
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "安装内置套件（${"%.0f".format(HideBundle.bundledSize(context) / 1024f / 1024f)} MB）",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 操作
            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(6.dp)) {
                listOf(
                    Triple("补装", "一键补装（oneclick.sh）", HideBundle.install()),
                    Triple("重装", "强制重装（--force）", HideBundle.forceReinstall()),
                    Triple("重导", "重导配置与 keybox（--reimport）", HideBundle.reimport()),
                    Triple("名单", "扫描并写 denylist（applist.sh --apply）", HideBundle.scanLists()),
                    Triple("日志", "查看 /data/adb/hide_bundle.log", HideBundle.tailLog()),
                ).forEach { (tag, label, cmd) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { exec(tag, cmd) }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(tag, fontSize = 9.sp, color = dim)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(label, fontSize = 12.sp, color = ink, modifier = Modifier.weight(1f))
                        Text(if (running == tag) "…" else "运行", fontSize = 10.sp, color = accent)
                    }
                }
            }

            // 输出
            if (output.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 300.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B2027))
                        .padding(12.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        output.forEach { line ->
                            Text(
                                line,
                                fontSize = 9.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (line.startsWith("$")) Color(0xFF7FD1C0) else Color(0xFFD6DEE8),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
