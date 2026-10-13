package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Speed

/**
 * HuijuiSU 性能模式切换器 v2.0.0
 *
 * 四档性能调度，通过 sysfs 调整 CPU governor 与 boost 参数。
 * 具体频率表因机型而异，后续版本会做 per-device 适配。
 */
@Composable
fun HuijuiPerfMode(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf<PerfMode?>(null) }
    var log by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val gov = readSysfs("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            currentMode = PerfMode.fromGovernor(gov)
            log = "当前 CPU governor: " + gov.ifBlank { "未知" }
        }
    }

    Dialog(onDismissRequest = { if (!running) onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                HuijuiDialogHeader(
                    icon = Icons.TwoTone.Speed,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "性能模式",
                    subtitle = "一键切换系统调度策略",
                )
                Spacer(Modifier.height(16.dp))

                PerfMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = {
                                if (running) return@RadioButton
                                scope.launch {
                                    running = true
                                    log = "正在切换到「" + mode.label + "」...\n"
                                    val ok = withContext(Dispatchers.IO) { applyMode(mode) }
                                    log += if (ok) "✓ 已切换到 " + mode.label else "✗ 切换失败"
                                    if (ok) currentMode = mode
                                    running = false
                                }
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                mode.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                mode.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (log.isNotEmpty()) {
                    Text(
                        log,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .height(120.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { if (!running) onDismiss() }) { Text("关闭") }
                }
            }
        }
    }
}

enum class PerfMode(
    val label: String,
    val desc: String,
    val governor: String,
) {
    POWER_SAVE("省电", "限制 CPU 频率，降低功耗", "powersave"),
    BALANCED("均衡", "日常使用，兼顾性能与功耗", "schedutil"),
    PERFORMANCE("性能", "释放 CPU 满血性能", "performance"),
    GAMING("游戏", "性能 + 关闭后台调度限制", "performance"),
    ;

    companion object {
        fun fromGovernor(gov: String): PerfMode? = when {
            gov.contains("powersave") -> POWER_SAVE
            gov.contains("performance") -> PERFORMANCE
            gov.contains("schedutil") -> BALANCED
            else -> null
        }
    }
}

private fun applyMode(mode: PerfMode): Boolean {
    return try {
        val sb = StringBuilder()
        // 1. 遍历 cpu0-cpu7，设置 governor
        for (i in 0..7) {
            val p = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_governor"
            sb.appendLine("[ -e " + p + " ] && echo " + mode.governor + " > " + p)
        }
        // 2. 附加调优
        when (mode) {
            PerfMode.POWER_SAVE -> {
                sb.appendLine("echo 0 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null")
            }
            PerfMode.BALANCED -> {
                sb.appendLine("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null")
            }
            PerfMode.PERFORMANCE -> {
                sb.appendLine("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null")
                sb.appendLine("echo 1 > /sys/module/cpu_boost/parameters/boost_ms 2>/dev/null")
            }
            PerfMode.GAMING -> {
                sb.appendLine("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null")
                sb.appendLine("for d in /sys/class/devfreq/*; do [ -e " + "${'$'}d/governor" + " ] && echo performance > " + "${'$'}d/governor" + " 2>/dev/null; done")
            }
        }
        sb.appendLine("echo done")
        val out = runAsRoot(sb.toString())
        out.contains("done")
    } catch (t: Throwable) {
        false
    }
}

private fun readSysfs(path: String): String = try {
    File(path).readText().trim()
} catch (t: Throwable) {
    ""
}

private fun runAsRoot(cmd: String): String = try {
    val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
    val out = p.inputStream.bufferedReader().readText()
    p.waitFor(30, TimeUnit.SECONDS)
    out
} catch (t: Throwable) {
    t.message ?: ""
}
