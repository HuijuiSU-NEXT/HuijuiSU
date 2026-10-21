package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Composable
fun UltraDialogWrapper(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))) {
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
LaunchedEffect(Unit) { withContext(Dispatchers.IO) { status = (runBlocking(Dispatchers.IO) { Shell.cmd("getenforce").exec() }.out.firstOrNull() ?: "Unknown").trim() } }
    UltraDialogWrapper("SELinux 控制器", "当前状态: $status", Icons.TwoTone.Security, onDismiss) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("setenforce 1").exec(); status = "Enforcing" } }) { Text("Enforcing") }
            Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("setenforce 0").exec(); status = "Permissive" } }) { Text("Permissive") }
        }
    }
}

@Composable
fun DpiDialog(onDismiss: () -> Unit) {
    var dpi by remember { mutableStateOf("") }
    UltraDialogWrapper("屏幕 DPI 修改", "输入自定义 DPI (如 420)", Icons.TwoTone.Tune, onDismiss) {
        OutlinedTextField(value = dpi, onValueChange = { dpi = it.filter { c -> c.isDigit() } }, label = { Text("DPI 数值") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { if (dpi.isNotBlank()) { runBlocking(Dispatchers.IO) { Shell.cmd("wm density $dpi").exec() }; onDismiss() } }, modifier = Modifier.fillMaxWidth()) { Text("应用") }
    }
}

@Composable
fun TcpDialog(onDismiss: () -> Unit) {
    UltraDialogWrapper("TCP 拥塞控制", "切换网络算法，优化延迟", Icons.TwoTone.NetworkCheck, onDismiss) {
        Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("sysctl -w net.ipv4.tcp_congestion_control=bbr").exec() }; onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("启用 BBR (提速)") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("sysctl -w net.ipv4.tcp_congestion_control=cubic").exec() }; onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("启用 CUBIC (稳定)") }
    }
}

@Composable
fun DropCachesDialog(onDismiss: () -> Unit) {
    UltraDialogWrapper("内存与缓存清理", "释放 RAM，缓解卡顿", Icons.TwoTone.CleaningServices, onDismiss) {
        Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("sync; echo 3 > /proc/sys/vm/drop_caches").exec() }; onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("立即执行 drop_caches") }
    }
}

@Composable
fun BatteryHealthDialog(onDismiss: () -> Unit) {
    var health by remember { mutableStateOf("读取中...") }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { health = runBlocking(Dispatchers.IO) { Shell.cmd("dumpsys battery").exec() }.out.joinToString("\n").ifBlank { "无法读取电池信息" } } }
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

@Composable
fun CpuGovernorDialog(onDismiss: () -> Unit) {
    var currentGov by remember { mutableStateOf("读取中...") }
LaunchedEffect(Unit) { withContext(Dispatchers.IO) { currentGov = runBlocking(Dispatchers.IO) { Shell.cmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec() }.out.firstOrNull() ?: "未知" } }
    UltraDialogWrapper("CPU 性能调度", "当前策略: $currentGov", Icons.TwoTone.Memory, onDismiss) {
        Column {
            listOf("performance" to "性能 (满血)", "schedutil" to "均衡 (日常)", "powersave" to "省电").forEach { (gov, name) ->
                Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("echo $gov > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec(); currentGov = gov } }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(name) }
            }
        }
    }
}

@Composable
fun ThermalDialog(onDismiss: () -> Unit) {
    var isThermalDisabled by remember { mutableStateOf(false) }
UltraDialogWrapper("温控控制", "风险操作：关闭温控可能导致过热", Icons.TwoTone.Whatshot, onDismiss) {
        Button(onClick = { runBlocking(Dispatchers.IO) { if (!isThermalDisabled) Shell.cmd("stop thermald; stop thermal-engine").exec() else Shell.cmd("start thermald; start thermal-engine").exec(); isThermalDisabled = !isThermalDisabled } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (isThermalDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) { Text(if (isThermalDisabled) "恢复系统温控" else "强制关闭系统温控") }
    }
}

@Composable
fun LogcatDialog(onDismiss: () -> Unit) {
    var logcatText by remember { mutableStateOf("点击下方按钮抓取日志...") }
UltraDialogWrapper("Logcat 日志抓取", "抓取最近 500 行系统日志", Icons.TwoTone.Terminal, onDismiss) {
        Column {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Text(logcatText, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { runBlocking(Dispatchers.IO) { logcatText = Shell.cmd("logcat -d -t 500").exec().out.joinToString("\n") } }, modifier = Modifier.fillMaxWidth()) { Text("一键抓取 Logcat") }
        }
    }
}

@Composable
fun HardwareMonitorDialog(onDismiss: () -> Unit) {
    var cpuFreq by remember { mutableStateOf("读取中...") }
    var gpuFreq by remember { mutableStateOf("读取中...") }
    var batteryTemp by remember { mutableStateOf("读取中...") }
    var ramUsage by remember { mutableStateOf("读取中...") }
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                cpuFreq = runBlocking(Dispatchers.IO) { Shell.cmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq | awk '{print $1/1000 \" MHz\"}'").exec() }.out.firstOrNull() ?: "未知"
                gpuFreq = runBlocking(Dispatchers.IO) { Shell.cmd("cat /sys/class/kgsl/kgsl-3d0/gpuclk | awk '{print $1/1000000 \" MHz\"}'").exec() }.out.firstOrNull() ?: "不支持"
                batteryTemp = runBlocking(Dispatchers.IO) { Shell.cmd("dumpsys battery | grep temperature | awk '{print $2/10 \" °C\"}'").exec() }.out.firstOrNull() ?: "未知"
                ramUsage = runBlocking(Dispatchers.IO) { Shell.cmd("free -m | grep Mem | awk '{print $3 \" MB / \" $2 \" MB\"}'").exec() }.out.firstOrNull() ?: "未知"
            }
            delay(1000)
        }
    }
    UltraDialogWrapper("实时硬件仪表盘", "每秒自动刷新底层硬件状态", Icons.TwoTone.Speed, onDismiss) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("CPU 频率", style = MaterialTheme.typography.bodyMedium); Text(cpuFreq, fontFamily = FontFamily.Monospace) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("GPU 频率", style = MaterialTheme.typography.bodyMedium); Text(gpuFreq, fontFamily = FontFamily.Monospace) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("电池温度", style = MaterialTheme.typography.bodyMedium); Text(batteryTemp, fontFamily = FontFamily.Monospace) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("内存占用", style = MaterialTheme.typography.bodyMedium); Text(ramUsage, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}

@Composable
fun AutoSchedulerDialog(onDismiss: () -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("引擎未启动") }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    UltraDialogWrapper("至尊 AI 自动调度", statusText, Icons.TwoTone.AutoAwesome, onDismiss) {
        Column {
            Text("智能识别前台应用与屏幕状态：游戏满血、日常均衡、熄屏省电。")
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("启用自动调度", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isEnabled, onCheckedChange = { checked ->
                    isEnabled = checked
                    if (checked) AutoSchedulerManager.start(context) { scene -> statusText = scene } else { AutoSchedulerManager.stop(); statusText = "引擎已关闭" }
                })
            }
        }
    }
}

@Composable
fun GpuControlDialog(onDismiss: () -> Unit) {
    var currentFreq by remember { mutableStateOf("读取中...") }
LaunchedEffect(Unit) { withContext(Dispatchers.IO) { currentFreq = runBlocking(Dispatchers.IO) { Shell.cmd("cat /sys/class/kgsl/kgsl-3d0/gpuclk").exec() }.out.firstOrNull()?.trim() ?: "不支持" } }
    UltraDialogWrapper("GPU 极限锁频", "当前频率: $currentFreq", Icons.TwoTone.Memory, onDismiss) {
        Column {
            Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on").exec(); Shell.cmd("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on").exec(); Shell.cmd("echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on").exec(); currentFreq = "已锁定最高频" } }, modifier = Modifier.fillMaxWidth()) { Text("🔥 锁定最高频") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on").exec(); Shell.cmd("echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on").exec(); Shell.cmd("echo 0 > /sys/class/kgsl/kgsl-3d0/force_rail_on").exec(); currentFreq = "已恢复系统动态调频" } }, modifier = Modifier.fillMaxWidth()) { Text("⚖️ 恢复动态调频") }
        }
    }
}

@Composable
fun RescueBackupDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var backupLog by remember { mutableStateOf("等待备份...") }
    var isBackingUp by remember { mutableStateOf(false) }
UltraDialogWrapper("分区备份救援", "一键备份核心分区防变砖", Icons.TwoTone.SaveAlt, onDismiss) {
        Column {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(150.dp)) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(backupLog, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    isBackingUp = true
                    runBlocking(Dispatchers.IO) {
                        // 修复：改用免权限的安全专属目录，绝不被 Android 13+ 拦截
                        val dir = "/storage/emulated/0/HuiJuiSU_Backup"
                        // 强制提权创建目录
                        runBlocking(Dispatchers.IO) { Shell.cmd("su -c 'mkdir -p \"$dir\" && chmod 777 \"$dir\"'").exec() }
                        backupLog = "开始备份...\n"
                        
                        // 动态获取当前槽位 (A/B分区)
                        val slot = Shell.cmd("su -c 'getprop ro.boot.slot_suffix'").exec().out.firstOrNull()?.trim() ?: ""
                        
                        listOf("boot", "init_boot", "dtbo", "vbmeta").forEach { p ->
                            // 动态尝试 带后缀 和 不带后缀 的分区名
                            val targetPart = if (Shell.cmd("su -c 'ls /dev/block/by-name/${p}$slot'").exec().isSuccess) "${p}$slot" else p
                            val res = Shell.cmd("su -c 'dd if=/dev/block/by-name/$targetPart of=\"$dir/$targetPart.img\"'").exec()
                            backupLog += if (res.isSuccess) "✅ $targetPart 成功\n" else "❌ $targetPart 失败\n"
                        }
                        isBackingUp = false
                        backupLog += "🎉 备份完成！\n\n📁 文件保存路径：\n$dir"
                    }
                },
                enabled = !isBackingUp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isBackingUp) "正在备份..." else "🚑 一键备份")
            }
        }
    }
}

@Composable
fun SusfsDialog(onDismiss: () -> Unit) {
    var status by remember { mutableStateOf("检查中...") }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { status = if (runBlocking(Dispatchers.IO) { Shell.cmd("cat /proc/sys/kernel/susfs | head -n 1").exec() }.isSuccess) "SUSFS 状态 (需内核支持)" else "SUSFS 未启用" } }
    UltraDialogWrapper("SUSFS 深度隐藏", status, Icons.TwoTone.Security, onDismiss) {
        Column {
            Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("su -c 'echo 1 > /sys/kernel/susfs/enable'").exec() } }, modifier = Modifier.fillMaxWidth()) { Text("开启自动隐藏") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { runBlocking(Dispatchers.IO) { Shell.cmd("su -c 'echo 0 > /sys/kernel/susfs/enable'").exec() } }, modifier = Modifier.fillMaxWidth()) { Text("关闭隐藏") }
        }
    }
}

// 恢复被删掉的自动调度引擎
object AutoSchedulerManager {
    private var isRunning = false
    private var job: kotlinx.coroutines.Job? = null
    private var currentScene = "等待检测"

    fun start(application: android.app.Application, onSceneChanged: (String) -> Unit) {
        // [已物理阉割] 防止后台死循环疯狂跑 Shell 导致手机发烫死机
        return
    }

    fun stop() {
isRunning = false
        job?.cancel()
        currentScene = "已关闭"
        runBlocking(Dispatchers.IO) { Shell.cmd("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec() }
        runBlocking(Dispatchers.IO) { Shell.cmd("start thermal-engine; start thermald").exec() }
    }
}