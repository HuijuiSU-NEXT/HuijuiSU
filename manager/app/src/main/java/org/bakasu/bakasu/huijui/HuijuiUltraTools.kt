package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UltraDialogWrapper(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                content()
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            }
        }
    }
}

@Composable
fun CpuGovernorDialog(onDismiss: () -> Unit) {
    var currentGov by remember { mutableStateOf("读取中...") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            currentGov = Shell.cmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec().out.firstOrNull() ?: "未知"
        }
    }
    UltraDialogWrapper("CPU 性能调度", "当前调度策略: $currentGov", Icons.TwoTone.Memory) {
        Column {
            listOf("performance" to "性能 (满血)", "schedutil" to "均衡 (日常)", "powersave" to "省电").forEach { (gov, name) ->
                Button(
                    onClick = { 
                        scope.launch(Dispatchers.IO) {
                            Shell.cmd("echo $gov > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec()
                            currentGov = gov
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text(name) }
            }
        }
    }
}

@Composable
fun ThermalDialog(onDismiss: () -> Unit) {
    var isThermalDisabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    UltraDialogWrapper("温控控制", "风险操作：关闭温控可能导致设备过热", Icons.TwoTone.Whatshot) {
        Button(
            onClick = { 
                scope.launch(Dispatchers.IO) {
                    if (!isThermalDisabled) {
                        Shell.cmd("stop thermald; stop thermal-engine").exec()
                    } else {
                        Shell.cmd("start thermald; start thermal-engine").exec()
                    }
                    isThermalDisabled = !isThermalDisabled
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (isThermalDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) { Text(if (isThermalDisabled) "恢复系统温控" else "强制关闭系统温控") }
    }
}

@Composable
fun LogcatDialog(onDismiss: () -> Unit) {
    var logcatText by remember { mutableStateOf("点击下方按钮抓取日志...") }
    val scope = rememberCoroutineScope()
    UltraDialogWrapper("Logcat 日志抓取", "抓取最近 500 行系统日志", Icons.TwoTone.Terminal) {
        Column {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Text(logcatText, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { 
                    scope.launch(Dispatchers.IO) {
                        logcatText = Shell.cmd("logcat -d -t 500").exec().out.joinToString("\n")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("一键抓取 Logcat") }
        }
    }
}

@Composable
fun WebAdminDialog(onDismiss: () -> Unit) {
    var isRunning by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("未启动") }
    var token by remember { mutableStateOf("") }

    UltraDialogWrapper("网页管理 (WebUI)", "通过局域网在电脑上管理手机", Icons.TwoTone.Computer) {
        Column {
            Text("确保手机和电脑在同一 Wi-Fi 下", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            if (isRunning) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("访问地址: $serverUrl?token=$token", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text("安全 Token: $token", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { HuijuiWebServer.stop(); isRunning = false; serverUrl = "未启动" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("停止服务器")
                }
            } else {
                Button(onClick = {
                    HuijuiWebServer.start { url ->
                        serverUrl = url
                        token = HuijuiWebServer.currentToken
                        isRunning = true
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("启动 Web 服务器")
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { HuijuiWebServer.stop() }
    }
}
