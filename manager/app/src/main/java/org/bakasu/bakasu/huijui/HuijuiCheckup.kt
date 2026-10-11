package org.bakasu.bakasu.huijui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class CheckItem(
    val name: String,
    val status: Status,
    val value: String,
    val suggestion: String = "",
) {
    enum class Status { OK, WARN, FAIL }
}

@Composable
fun HuijuiCheckup(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CheckItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var conclusion by remember { mutableStateOf("") }

    fun rescan() {
        scope.launch {
            loading = true
            val r = withContext(Dispatchers.IO) { runCheckup(context) }
            items = r
            conclusion = buildConclusion(r)
            loading = false
        }
    }

    LaunchedEffect(Unit) { rescan() }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("一键体检", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "HuijuiSU · 系统健康检查",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("正在扫描...")
                    }
                } else {
                    items.forEach { item ->
                        CheckRow(item)
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(conclusion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = !loading, onClick = { rescan() }) { Text("重新扫描") }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(item: CheckItem) {
    val (symbol, color) = when (item.status) {
        CheckItem.Status.OK -> "✓" to MaterialTheme.colorScheme.primary
        CheckItem.Status.WARN -> "⚠" to MaterialTheme.colorScheme.tertiary
        CheckItem.Status.FAIL -> "✗" to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.Top) {
        Text(symbol, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
        Column {
            Row {
                Text(item.name, fontWeight = FontWeight.Medium, modifier = Modifier.width(84.dp))
                Text(item.value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            if (item.suggestion.isNotEmpty()) {
                Text(
                    item.suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun runCheckup(context: Context): List<CheckItem> {
    val out = mutableListOf<CheckItem>()

    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val charging = bm.isCharging
    when {
        level < 15 -> out += CheckItem("电量", CheckItem.Status.FAIL, "$level%", "电量过低，刷机前请充电")
        level < 30 -> out += CheckItem("电量", CheckItem.Status.WARN, "$level%", "建议充到 50% 以上再刷")
        else -> out += CheckItem("电量", CheckItem.Status.OK, "$level%${if (charging) " · 充电中" else ""}")
    }

    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val tempC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
    when {
        tempC >= 45 -> out += CheckItem("温度", CheckItem.Status.FAIL, "%.1f°C".format(tempC), "温度过高，等降温")
        tempC >= 40 -> out += CheckItem("温度", CheckItem.Status.WARN, "%.1f°C".format(tempC), "温度偏高")
        else -> out += CheckItem("温度", CheckItem.Status.OK, "%.1f°C".format(tempC))
    }

    val locked = readProp("ro.boot.flash.locked") == "1"
    val vbState = readProp("ro.boot.verifiedbootstate").ifEmpty { "unknown" }
    out += CheckItem(
        "Bootloader",
        if (locked) CheckItem.Status.WARN else CheckItem.Status.OK,
        if (locked) "已锁定" else "已解锁 ($vbState)",
        if (locked) "锁着刷不了，先解锁" else "",
    )

    val kernel = System.getProperty("os.version") ?: "unknown"
    out += CheckItem("内核", CheckItem.Status.OK, kernel.take(48))

    val hasRoot = try {
        val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        val ok = p.waitFor(3, TimeUnit.SECONDS) &&
            p.inputStream.bufferedReader().readText().contains("uid=0")
        ok
    } catch (t: Throwable) { false }
    out += if (hasRoot) {
        CheckItem("Root", CheckItem.Status.OK, "可用")
    } else {
        CheckItem("Root", CheckItem.Status.FAIL, "不可用", "检查 HuijuiSU 是否已授权")
    }

    val selinux = try {
        val p = ProcessBuilder("getenforce").redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().ifEmpty { "Unknown" }
    } catch (t: Throwable) { "Unknown" }
    out += when (selinux.lowercase()) {
        "enforcing" -> CheckItem("SELinux", CheckItem.Status.OK, selinux)
        "permissive" -> CheckItem("SELinux", CheckItem.Status.WARN, selinux, "宽容模式安全性较低")
        else -> CheckItem("SELinux", CheckItem.Status.WARN, selinux)
    }

    val stat = StatFs(Environment.getDataDirectory().path)
    val freeGB = stat.availableBytes / 1024.0 / 1024.0 / 1024.0
    when {
        freeGB < 1 -> out += CheckItem("存储", CheckItem.Status.FAIL, "%.1f GB".format(freeGB), "空间严重不足")
        freeGB < 4 -> out += CheckItem("存储", CheckItem.Status.WARN, "%.1f GB".format(freeGB), "空间偏少，备份可能失败")
        else -> out += CheckItem("存储", CheckItem.Status.OK, "%.1f GB 可用".format(freeGB))
    }

    return out
}

private fun buildConclusion(items: List<CheckItem>): String {
    val fails = items.count { it.status == CheckItem.Status.FAIL }
    val warns = items.count { it.status == CheckItem.Status.WARN }
    return when {
        fails > 0 -> "✗ $fails 项不合格，请修复后再操作"
        warns > 0 -> "⚠ $warns 项需注意，谨慎操作"
        else -> "✅ 今日宜刷机"
    }
}

private fun readProp(key: String): String = try {
    val p = ProcessBuilder("getprop", key).redirectErrorStream(true).start()
    p.inputStream.bufferedReader().readText().trim()
} catch (t: Throwable) { "" }
