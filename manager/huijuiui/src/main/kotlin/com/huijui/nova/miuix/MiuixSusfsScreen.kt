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
import com.huijui.nova.susfs.Susfs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.huijui.nova.ui.Text
import com.huijui.nova.ui.MiuixTheme
import com.huijui.nova.ui.lightColorScheme

/**
 * SUSFS 设置页。
 *
 * 对标 SukiSU/ReSukiSU 的 SUSFS 面板，但把 `ksud susfs config` 的能力全放出来：
 * 总开关、三个子开关、隐藏路径增删、备份与恢复。
 */
@Composable
fun MiuixSusfsScreen(onBack: () -> Unit) {
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF7C6BF0)

    var status by remember { mutableStateOf(Susfs.Status()) }
    var paths by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var newPath by remember { mutableStateOf("") }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }

    suspend fun refresh() {
        loading = true
        status = Susfs.status()
        paths = Susfs.listPaths()
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    fun act(label: String, block: suspend () -> HideBundleResult) {
        if (busy) return
        busy = true
        log = listOf("$ $label")
        MainScope().launch {
            val r = block()
            log = log + r.output.ifEmpty { listOf("（无输出）") }
            busy = false
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
                    Text(S("SUSFS"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        when {
                            loading -> "读取中…"
                            status.error.isNotBlank() -> status.error
                            status.enabled -> "已启用 · ${paths.size} 条隐藏路径"
                            else -> "已安装 · 未启用"
                        },
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Text(
                    S("刷新"),
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { MainScope().launch { refresh() } }.padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            if (status.error.isNotBlank()) {
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(16.dp)) {
                    Text(status.error, fontSize = 11.sp, color = dim)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "SUSFS 需要内核编译进 susfs 支持；当前内核没有时这里只能看，不能改。",
                        fontSize = 10.sp,
                        color = dim,
                        lineHeight = 15.sp,
                    )
                }
            } else {
                // 总开关
                GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(S("SUSFS 总开关"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                            Text(S("启用后才会应用下面的隐藏规则"), fontSize = 9.sp, color = dim)
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (status.enabled) accent.copy(alpha = 0.75f)
                                    else Color.White.copy(alpha = 0.25f),
                                )
                                .clickable(enabled = !busy) {
                                    act(if (status.enabled) "disable" else "enable") {
                                        Susfs.setEnabled(!status.enabled)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (status.enabled) "已启用" else "已关闭",
                                fontSize = 10.sp,
                                color = if (status.enabled) Color.White else ink,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 子开关
                GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(6.dp)) {
                    ToggleRow("logging", "日志", "记录 SUSFS 操作", status.logging, ink, dim, accent) {
                        act("logging") { Susfs.setToggle("logging", !status.logging) }
                    }
                    ToggleRow(
                        "avc_log_spoofing",
                        "AVC 日志伪造",
                        "对目标进程伪造 SELinux 拒绝日志",
                        status.avcLogSpoofing,
                        ink,
                        dim,
                        accent,
                    ) { act("avc_log_spoofing") { Susfs.setToggle("avc_log_spoofing", !status.avcLogSpoofing) } }
                    ToggleRow(
                        "hide_sus_mnts",
                        "隐藏 sus 挂载",
                        "对非 root 进程隐藏 sus 挂载点",
                        status.hideSusMnts,
                        ink,
                        dim,
                        accent,
                    ) { act("hide_sus_mnts") { Susfs.setToggle("hide_sus_mnts_for_non_su_procs", !status.hideSusMnts) } }
                }

                Spacer(Modifier.height(10.dp))

                // 隐藏路径
                GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(14.dp)) {
                    Text(S("隐藏路径"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "路径及其子路径对已 umount 的应用进程不可见（只对 uid ≥ 10000 生效）",
                        fontSize = 9.sp,
                        color = dim,
                    )
                    Spacer(Modifier.height(10.dp))

                    paths.forEach { path ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                path,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                S("删除"),
                                fontSize = 10.sp,
                                color = Color(0xFFD9534F),
                                modifier = Modifier
                                    .clickable(enabled = !busy) { act("rm $path") { Susfs.removePath(path) } }
                                    .padding(4.dp),
                            )
                        }
                    }

                    if (paths.isEmpty()) {
                        Text(S("还没有隐藏路径"), fontSize = 10.sp, color = dim)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ink.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = newPath,
                            onValueChange = { newPath = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ink),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (newPath.isEmpty()) {
                                    Text("/data/adb/modules/xxx", fontSize = 11.sp, color = dim.copy(alpha = 0.6f))
                                }
                                inner()
                            },
                        )
                        Text(
                            S("添加"),
                            fontSize = 11.sp,
                            color = accent,
                            modifier = Modifier
                                .clickable(enabled = !busy && newPath.isNotBlank()) {
                                    val p = newPath.trim()
                                    newPath = ""
                                    act("add $p") { Susfs.addPath(p, loop = true) }
                                }
                                .padding(6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 备份 / 恢复
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Text(S("配置备份"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            S("导出到 /sdcard"),
                            fontSize = 11.sp,
                            color = accent,
                            modifier = Modifier
                                .clickable(enabled = !busy) {
                                    act("backup") { Susfs.backupTo("/sdcard/susfs_backup.json") }
                                }
                                .padding(6.dp),
                        )
                        Text(
                            S("恢复默认"),
                            fontSize = 11.sp,
                            color = Color(0xFFB08A2E),
                            modifier = Modifier
                                .clickable(enabled = !busy) { act("restore") { Susfs.restoreDefault() } }
                                .padding(6.dp),
                        )
                    }
                }
            }

            if (log.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B2027))
                        .padding(12.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        log.forEach { line ->
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

private typealias HideBundleResult = com.huijui.nova.hide.HideBundle.Result

@Composable
private fun ToggleRow(
    key: String,
    title: String,
    desc: String,
    value: Boolean,
    ink: Color,
    dim: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink)
            Text(desc, fontSize = 9.sp, color = dim)
        }
        Text(if (value) "已开启" else "已关闭", fontSize = 10.sp, color = if (value) accent else dim)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(width = 34.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(if (value) accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.25f)),
        )
    }
}
