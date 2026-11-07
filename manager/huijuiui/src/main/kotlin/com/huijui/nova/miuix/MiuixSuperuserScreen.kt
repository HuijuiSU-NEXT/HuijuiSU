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
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 超级用户页。
 *
 * 数据来自 `/data/adb/ksu/.allowlist`（KernelSU 的授权表），
 * uid → 包名用系统的 PackageManager 解析，不需要额外 root 权限。
 */
@Composable
fun MiuixSuperuserScreen() {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    data class Entry(val uid: Int, val label: String, val pkg: String, val policy: String)

    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        val result = HideBundle.run(
            """
            [ -f /data/adb/ksu/.allowlist ] && cat /data/adb/ksu/.allowlist || echo "__NOFILE__"
            """.trimIndent(),
        )
        val lines = result.output.filter { it.isNotBlank() && !it.startsWith("执行失败") }
        if (lines.firstOrNull() == "__NOFILE__" || lines.isEmpty()) {
            note = if (result.ok) "没有授权记录" else "读不到授权表（需要 Root）"
            entries = emptyList()
            loading = false
            return
        }

        val pm = context.packageManager
        val parsed = lines.mapNotNull { line ->
            val uid = Regex("""(\d{3,6})""").find(line)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@mapNotNull null
            val policy = when {
                line.contains("allow", true) || line.contains("grant", true) -> "允许"
                line.contains("deny", true) -> "拒绝"
                line.endsWith(":2") || line.endsWith("=2") -> "拒绝"
                else -> "允许"
            }
            val pkgs = runCatching { pm.getPackagesForUid(uid) }.getOrNull()
            val pkg = pkgs?.firstOrNull().orEmpty()
            val label = runCatching {
                pkg.takeIf { it.isNotBlank() }?.let {
                    pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
                }
            }.getOrNull()
            Entry(uid = uid, label = label ?: pkg.ifBlank { "uid $uid" }, pkg = pkg, policy = policy)
        }.distinctBy { it.uid }

        entries = parsed
        note = if (parsed.isEmpty()) "授权表存在，但解析不出条目" else ""
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    MiuixTheme(colors = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("超级用户", fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        if (loading) "读取中…" else "${entries.size} 个应用已授权",
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

            if (entries.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(18.dp)) {
                    Text(
                        if (loading) "读取授权表…" else note.ifBlank { "没有授权记录" },
                        fontSize = 12.sp,
                        color = dim,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "在应用第一次请求 root 时，这里会出现记录；也可以在下面的终端里用 ksud 管理。",
                        fontSize = 10.sp,
                        color = dim,
                    )
                }
            } else {
                entries.forEach { e ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        corner = 16.dp,
                        padding = PaddingValues(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    e.label.take(1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink, maxLines = 1)
                                Text(
                                    e.pkg.ifBlank { "uid ${e.uid}" },
                                    fontSize = 10.sp,
                                    color = dim,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                e.policy,
                                fontSize = 11.sp,
                                color = if (e.policy == "允许") accent else Color(0xFFD9534F),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
