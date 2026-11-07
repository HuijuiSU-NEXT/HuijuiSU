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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private data class KernelFeature(
    val id: String,
    val label: String,
    val desc: String,
)

private val features = listOf(
    KernelFeature("su_compat", "su 兼容", "为传统 su 调用提供兼容层"),
    KernelFeature("kernel_umount", "内核 umount", "开机时处理模块的挂载回退"),
    KernelFeature("sulog", "sulog", "记录 su 调用日志"),
    KernelFeature("adb_root", "adb root", "允许 adb root"),
    KernelFeature("selinux_hide", "SELinux 隐藏", "对目标进程隐藏 SELinux 状态"),
)

/**
 * 内核设置页。
 *
 * 全部走 `ksud feature`：`get <id>` 读状态，`set <id> <0|1>` 写状态。
 * 不支持的特性（内核没编译进去）会显示「不支持」而不是假装能开。
 */
@Composable
fun MiuixKernelSettingsScreen(onBack: () -> Unit) {
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    val states = remember { mutableStateMapOf<String, String>() }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        val list = HideBundle.run("ksud feature list")
        if (!list.ok && list.output.isEmpty()) {
            note = "读不到特性列表（需要 Root / ksud）"
            loading = false
            return
        }
        note = ""
        features.forEach { f ->
            val r = HideBundle.run("ksud feature check ${f.id}")
            val text = r.output.joinToString(" ")
            states[f.id] = when {
                !r.ok -> "不可用"
                text.contains("unsupported", true) -> "不支持"
                text.contains("enabled", true) || text.contains("= 1") || text.endsWith("1") -> "已开启"
                text.contains("disabled", true) || text.contains("= 0") || text.endsWith("0") -> "已关闭"
                else -> text.take(24).ifBlank { "未知" }
            }
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    fun toggle(f: KernelFeature) {
        if (busy != null) return
        val on = states[f.id] == "已开启"
        busy = f.id
        MainScope().launch {
            HideBundle.run("ksud feature set ${f.id} ${if (on) 0 else 1}")
            busy = null
            load()
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
                    Text("内核设置", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        if (loading) "读取中…" else "ksud feature · ${features.size} 项",
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Text(
                    "刷新",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier
                        .clickable { MainScope().launch { load() } }
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            if (note.isNotBlank()) {
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Text(note, fontSize = 11.sp, color = dim)
                }
                Spacer(Modifier.height(10.dp))
            }

            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(6.dp)) {
                features.forEach { f ->
                    val state = states[f.id] ?: if (loading) "读取中" else "未知"
                    val enabled = state == "已开启"
                    val available = state != "不支持" && state != "不可用"

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = available && busy == null) { toggle(f) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(f.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                            Text(f.desc, fontSize = 9.sp, color = dim)
                        }
                        Text(
                            when {
                                busy == f.id -> "…"
                                !available -> state
                                enabled -> "已开启"
                                else -> "已关闭"
                            },
                            fontSize = 10.sp,
                            color = when {
                                !available -> dim
                                enabled -> accent
                                else -> dim
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(width = 34.dp, height = 18.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (enabled && available) accent.copy(alpha = 0.75f)
                                    else Color.White.copy(alpha = 0.25f),
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                Text(
                    "这些开关直接写内核特性状态，部分需要重启才彻底生效。" +
                        "不支持的项目说明当前内核没编译进去，切换无效。",
                    fontSize = 10.sp,
                    color = dim,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
