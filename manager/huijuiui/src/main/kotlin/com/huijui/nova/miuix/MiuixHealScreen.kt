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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.heal.SelfHeal
import com.huijui.nova.hide.HideBundle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.huijui.nova.ui.Text
import com.huijui.nova.ui.MiuixTheme
import com.huijui.nova.ui.lightColorScheme

/**
 * 自愈中心。
 *
 * 体检告诉你「哪里冲突」，这里直接「修好它」：
 * 停用冲突模块（可逆）、清理缓存、列出最近安装的模块。
 */
@Composable
fun MiuixHealScreen(onBack: () -> Unit) {
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    var conflicts by remember { mutableStateOf<List<SelfHeal.Conflict>>(emptyList()) }
    var recent by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }

    suspend fun load() {
        loading = true
        conflicts = SelfHeal.conflicts()
        recent = SelfHeal.recentModules()
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    fun act(label: String, block: suspend () -> List<String>) {
        if (busy) return
        busy = true
        log = listOf("$ $label")
        MainScope().launch {
            val out = block()
            log = log + out.ifEmpty { listOf("完成") }
            busy = false
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
                    Text(S("自愈中心"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        when {
                            loading -> "扫描中…"
                            conflicts.isEmpty() -> "没有需要处理的冲突"
                            else -> "${conflicts.size} 处冲突可修"
                        },
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Text(
                    "刷新",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { MainScope().launch { load() } }.padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // 一键自愈
            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(16.dp)) {
                Text(S("一键自愈"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "停用每个冲突路径里后加载的模块，重启后生效；随时可以重新启用。",
                    fontSize = 10.sp,
                    color = dim,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.85f))
                            .clickable(enabled = !busy && conflicts.isNotEmpty()) {
                                act("一键自愈") {
                                    val disabled = SelfHeal.healAll()
                                    if (disabled.isEmpty()) listOf("没有需要停用的模块")
                                    else disabled.map { "已停用 $it" }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (busy) "处理中…" else "开始修复",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(enabled = !busy) {
                                act("清理缓存") { SelfHeal.clearCaches() }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("清理缓存", fontSize = 12.sp, color = ink)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 冲突明细
            conflicts.forEach { c ->
                GlassCard(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    corner = 16.dp,
                    padding = PaddingValues(14.dp),
                ) {
                    Text(c.path, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ink, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    c.moduleIds.forEachIndexed { i, id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (i == 0) "保留" else "冲突",
                                fontSize = 9.sp,
                                color = if (i == 0) accent else Color(0xFFD9534F),
                                modifier = Modifier.width(34.dp),
                            )
                            Text(
                                id,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ink,
                                modifier = Modifier.weight(1f),
                            )
                            if (i > 0) {
                                Text(
                                    "停用",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD9534F),
                                    modifier = Modifier
                                        .clickable(enabled = !busy) {
                                            act("停用 $id") {
                                                if (SelfHeal.disable(id)) listOf("已停用 $id") else listOf("失败：$id")
                                            }
                                        }
                                        .padding(6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 最近安装
            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Text(S("最近安装的模块"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                    Spacer(Modifier.height(2.dp))
                    Text("开机异常时优先停用它们", fontSize = 9.sp, color = dim)
                    Spacer(Modifier.height(8.dp))
                    recent.forEach { (id, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                id,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "停用",
                                fontSize = 10.sp,
                                color = Color(0xFFB08A2E),
                                modifier = Modifier
                                    .clickable(enabled = !busy) {
                                        act("停用 $id") {
                                            if (SelfHeal.disable(id)) listOf("已停用 $id") else listOf("失败：$id")
                                        }
                                    }
                                    .padding(6.dp),
                            )
                        }
                    }
                }
            }

            if (log.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B2027))
                        .padding(12.dp),
                ) {
                    Column {
                        log.forEach {
                            Text(
                                it,
                                fontSize = 9.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (it.startsWith("$")) Color(0xFF7FD1C0) else Color(0xFFD6DEE8),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
