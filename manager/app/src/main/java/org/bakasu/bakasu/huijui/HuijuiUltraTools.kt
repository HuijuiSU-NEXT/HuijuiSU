package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))) {
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
fun SelinuxDialog(onDismiss: () -> Unit) {
    var status by remember { mutableStateOf("正在获取...") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { status = (Shell.cmd("getenforce").exec().out.firstOrNull() ?: "Unknown").trim() } }
    UltraDialogWrapper("SELinux 控制器", "当前状态: $status", Icons.TwoTone.Security, onDismiss) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { scope.launch(Dispatchers.IO) { Shell.cmd("setenforce 1").exec(); status = "Enforcing" } }) { Text("Enforcing") }
            Button(onClick = { scope.launch(Dispatchers.IO) { Shell.cmd("setenforce 0").exec(); status = "Permissive" } }) { Text("Permissive") }
        }
    }
}

@Composable
fun DpiDialog(onDismiss: () -> Unit) {
    var dpi by remember { mutableStateOf("") }
    UltraDialogWrapper("屏幕 DPI 修改", "输入自定义 DPI (如 420)", Icons.TwoTone.Tune, onDismiss) {
        OutlinedTextField(value = dpi, onValueChange = { dpi = it.filter { c -> c.isDigit() } }, label = { Text("DPI 数值") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { if (dpi.isNotBlank()) { Shell.cmd("wm density $dpi").exec(); onDismiss() } }, modifier = Modifier.fillMaxWidth()) { Text("应用") }
    }
}

@Composable
fun TcpDialog(onDismiss: () -> Unit) {
    UltraDialogWrapper("TCP 拥塞控制", "切换网络算法，优化延迟", Icons.TwoTone.NetworkCheck, onDismiss) {
        Button(onClick = { Shell.cmd("sysctl -w net.ipv4.tcp_congestion_control=bbr").exec(); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("启用 BBR (提速)") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { Shell.cmd("sysctl -w net.ipv4.tcp_congestion_control=cubic").exec(); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("启用 CUBIC (稳定)") }
    }
}

@Composable
fun DropCachesDialog(onDismiss: () -> Unit) {
    UltraDialogWrapper("内存与缓存清理", "释放 RAM，缓解卡顿", Icons.TwoTone.CleaningServices, onDismiss) {
        Button(onClick = { Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec(); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("立即执行 drop_caches") }
    }
}

@Composable
fun BatteryHealthDialog(onDismiss: () -> Unit) {
    var health by remember { mutableStateOf("读取中...") }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { health = Shell.cmd("dumpsys battery").exec().out.joinToString("\n").ifBlank { "无法读取电池信息" } } }
    UltraDialogWrapper("电池健康", "底层状态信息", Icons.TwoTone.BatteryChargingFull, onDismiss) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Text(health, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()))
        }
    }
}

@Composable
fun WebAdminDialog(onDismiss: () -> Unit) {
    var isRunning by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("未启动") }
    var token by remember { mutableStateOf("") }
    UltraDialogWrapper("网页管理 (WebUI)", "通过局域网在电脑上管理手机", Icons.TwoTone.Computer, onDismiss) {
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
                Button(onClick = { HuijuiWebServer.stop(); isRunning = false; serverUrl = "未启动" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("停止服务器") }
            } else {
                Button(onClick = { HuijuiWebServer.start { url -> serverUrl = url; token = HuijuiWebServer.currentToken; isRunning = true } }, modifier = Modifier.fillMaxWidth()) { Text("启动 Web 服务器") }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { HuijuiWebServer.stop() } }
}
