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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.device.KsuModeDetector
import com.huijui.nova.device.RootCheck
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import com.huijui.nova.modules.NovaModules
import com.huijui.nova.ui.Text
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 诊断页。
 *
 * 出问题时，用户不用描述现象——把这一页的内容复制给我就能定位：
 * root 状态、ksud 路径与版本、ksud 目录、授权表条目数、模块数、最近的错误。
 */
@Composable
fun MiuixDiagnoseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF2E9E6B)

    var lines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var raw by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val list = mutableListOf<Pair<String, String>>()
        fun add(k: String, v: String) { list += k to v }

        val pkg = runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode else 0L})"
        }.getOrDefault("?")

        add("包名", context.packageName)
        add("版本", pkg)
        add("系统", "Android ${android.os.Build.VERSION.RELEASE} · SDK ${android.os.Build.VERSION.SDK_INT}")

        val state = RootCheck.check()
        add("root 状态", when (state) {
            RootCheck.State.Granted -> "已授权"
            RootCheck.State.Denied -> "被拒绝"
            RootCheck.State.Unavailable -> "无法调用 su"
            RootCheck.State.Unknown -> "未知"
        })
        if (RootCheck.detail.isNotBlank()) add("su 输出", RootCheck.detail)

        val ksu = KsuModeDetector.detect()
        add("内核", ksu.kernel.ifBlank { "?" })
        add("工作模式", ksu.mode.label)
        add("ksud", ksu.ksud.ifBlank { "未找到" })
        if (ksu.ksudVersion.isNotBlank()) add("ksud 版本", ksu.ksudVersion)

        val ksuDir = HideBundle.run("ls /data/adb/ksu 2>/dev/null | head -20; exit 0")
        add("ksu 目录", if (ksuDir.output.isEmpty()) "不可读" else ksuDir.output.joinToString(", "))

        val allow = HideBundle.runRaw("wc -c < /data/adb/ksu/.allowlist 2>/dev/null; exit 0")
        add("授权表大小", allow.toString(Charsets.UTF_8).trim().ifBlank { "不可读" })

        val mods = NovaModules.list()
        add("模块数量", mods.size.toString())
        add("模块（前 8）", mods.take(8).joinToString(", ") { it.id }.ifBlank { "-" })

        val probe = HideBundle.run("command -v ksud; exit 0")
        add("ksud 探测", probe.output.joinToString(" ").ifBlank { "-" })

        lines = list
        raw = list.joinToString("\n") { "${it.first}: ${it.second}" }
        loading = false
    }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Column {
            Spacer(Modifier.height(14.dp))
            Row {
                Text(
                    "‹",
                    fontSize = 26.sp,
                    color = ink,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(S("诊断"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text("出问题时把这页内容发给开发者", fontSize = 11.sp, color = dim)
                }
                Text(
                    if (copied) "已复制" else "复制",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier
                        .clickable {
                            runCatching {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("diag", raw))
                                copied = true
                            }
                        }
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(Modifier.fillMaxWidth(), corner = 18.dp, padding = PaddingValues(14.dp)) {
                if (loading) {
                    Text("正在采集…", fontSize = 11.sp, color = dim)
                } else {
                    lines.forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(k, fontSize = 10.sp, color = dim, modifier = Modifier.width(88.dp))
                            Text(
                                v,
                                fontSize = 10.sp,
                                color = ink,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            if (!loading && RootCheck.state != RootCheck.State.Granted) {
                Spacer(Modifier.height(10.dp))
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(14.dp)) {
                    Text(
                        "root 不可用时的处理：\n" +
                            "1. 确认设备已刷入本项目的 KernelSU（LKM 或内核内置）\n" +
                            "2. 打开 KernelSU 管理器，把「HuiJuiSU」加入超级用户\n" +
                            "3. 若内核是用别处构建的，管理器包名/签名需要与内核构建参数一致\n" +
                            "4. 仍不行就把本页复制发出来",
                        fontSize = 10.sp,
                        color = dim,
                        lineHeight = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
