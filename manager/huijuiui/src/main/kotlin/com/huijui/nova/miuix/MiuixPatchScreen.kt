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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.patches.Patches
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 补丁中心：内置的一键修复/隐藏补丁。
 *
 * 和「体检」配合：体检发现问题 → 这里直接打补丁。
 * 所有补丁都是 resetprop 临时改，重启自动恢复，不会写死到分区。
 */
@Composable
fun MiuixPatchScreen(onBack: () -> Unit) {
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    val values = remember { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf<String?>(null) }
    var output by remember { mutableStateOf<List<String>>(emptyList()) }
    var pending by remember { mutableStateOf<Patches.Patch?>(null) }

    suspend fun refresh() {
        Patches.builtin.forEach { p ->
            values[p.id] = Patches.currentValue(p)
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun run(patch: Patches.Patch) {
        if (busy != null) return
        busy = patch.id
        output = listOf("$ ${patch.name}")
        MainScope().launch {
            val r = Patches.apply(patch)
            output = output + r.output.ifEmpty { listOf("（无输出）") }
            busy = null
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
                    Text(S("补丁中心"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text("${Patches.builtin.size} 个内置补丁 · 重启自动恢复", fontSize = 11.sp, color = dim)
                }
                Text(
                    S("刷新"),
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { MainScope().launch { refresh() } }.padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Patches.Group.entries.forEach { group ->
                val list = Patches.builtin.filter { it.group == group }
                if (list.isEmpty()) return@forEach
                Text(
                    group.label,
                    fontSize = 11.sp,
                    color = dim,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp),
                )
                list.forEach { patch ->
                val riskColor = when (patch.risk) {
                    Patches.Risk.Safe -> accent
                    Patches.Risk.Caution -> Color(0xFFB08A2E)
                    Patches.Risk.Dangerous -> Color(0xFFD9534F)
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    corner = 16.dp,
                    padding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(patch.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(riskColor.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        when (patch.risk) {
                                            Patches.Risk.Safe -> "安全"
                                            Patches.Risk.Caution -> "谨慎"
                                            Patches.Risk.Dangerous -> "危险"
                                        },
                                        fontSize = 8.sp,
                                        color = riskColor,
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(patch.desc, fontSize = 10.sp, color = dim, lineHeight = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "当前：${values[patch.id] ?: "…"}",
                                fontSize = 9.sp,
                                color = dim,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (busy == patch.id) "…" else if (patch.id == "restore_all") "说明" else "应用",
                            fontSize = 11.sp,
                            color = riskColor,
                            modifier = Modifier
                                .clickable {
                                    if (patch.risk == Patches.Risk.Safe) {
                                        run(patch)
                                    } else {
                                        pending = patch
                                    }
                                }
                                .padding(6.dp),
                        )
                    }
                }
                }
                Spacer(Modifier.height(6.dp))
            }

            // 输出
            if (output.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 220.dp)
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

        // 风险确认
        pending?.let { patch ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                GlassCard(Modifier.padding(26.dp), corner = 20.dp, padding = PaddingValues(18.dp), alpha = 0.94f) {
                    Text("应用「${patch.name}」？", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${patch.desc}\n\n修改属性：${patch.target}\n重启后自动恢复。",
                        fontSize = 10.sp,
                        color = dim,
                        lineHeight = 15.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Text(
                            S("取消"),
                            fontSize = 12.sp,
                            color = dim,
                            modifier = Modifier
                                .clickable { pending = null }
                                .padding(8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            S("应用"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB08A2E),
                            modifier = Modifier
                                .clickable {
                                    val p = patch
                                    pending = null
                                    run(p)
                                }
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
