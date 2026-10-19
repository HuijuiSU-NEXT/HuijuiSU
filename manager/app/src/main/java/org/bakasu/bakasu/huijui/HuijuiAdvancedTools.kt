package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material.icons.twotone.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. GPU 极限锁频
@Composable
fun GpuControlDialog(onDismiss: () -> Unit) {
    var currentFreq by remember { mutableStateOf("读取中...") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            currentFreq = Shell.cmd("cat /sys/class/kgsl/kgsl-3d0/gpuclk").exec().out.firstOrNull()?.trim() ?: "不支持"
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))) {
            Column(Modifier.padding(20.dp)) {
                Text("GPU 极限锁频", style = MaterialTheme.typography.titleLarge)
                Text("当前频率: $currentFreq", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        Shell.cmd("echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on").exec()
                        Shell.cmd("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on").exec()
                        Shell.cmd("echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on").exec()
                        currentFreq = "已锁定最高频 (满血模式)"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("🔥 锁定最高频 (强制满血)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        Shell.cmd("echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on").exec()
                        Shell.cmd("echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on").exec()
                        Shell.cmd("echo 0 > /sys/class/kgsl/kgsl-3d0/force_rail_on").exec()
                        currentFreq = "已恢复系统动态调频"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("⚖️ 恢复动态调频") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            }
        }
    }
}

// 2. 分区备份救援
@Composable
fun RescueBackupDialog(onDismiss: () -> Unit) {
    var backupLog by remember { mutableStateOf("等待备份...") }
    var isBackingUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))) {
            Column(Modifier.padding(20.dp)) {
                Text("至尊分区备份", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Text(backupLog, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    isBackingUp = true
                    scope.launch(Dispatchers.IO) {
                        val dir = "/sdcard/HuiJuiSU_Ultra_Backup"
                        Shell.cmd("mkdir -p $dir").exec()
                        backupLog = "开始备份到 $dir...\n"
                        val partitions = listOf("boot", "init_boot", "dtbo", "vbmeta")
                        for (p in partitions) {
                            val res = Shell.cmd("dd if=/dev/block/by-name/$p of=$dir/${p}.img").exec()
                            backupLog += if (res.isSuccess) "✅ $p 备份成功\n" else "❌ $p 备份失败\n"
                        }
                        isBackingUp = false
                        backupLog += "🎉 备份完成！请妥善保管！"
                    }
                }, enabled = !isBackingUp, modifier = Modifier.fillMaxWidth()) { Text(if (isBackingUp) "正在备份..." else "🚑 一键备份核心分区") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            }
        }
    }
}

// 3. SUSFS 深度隐藏
@Composable
fun SusfsDialog(onDismiss: () -> Unit) {
    var status by remember { mutableStateOf("检查中...") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val check = Shell.cmd("cat /proc/sys/kernel/susfs | head -n 1").exec()
            status = if (check.isSuccess) "SUSFS 已启用" else "SUSFS 未启用或不支持"
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))) {
            Column(Modifier.padding(20.dp)) {
                Text("SUSFS 深度隐藏", style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    Shell.cmd("su -c 'echo 1 > /sys/kernel/susfs/enable'").exec()
                }, modifier = Modifier.fillMaxWidth()) { Text("开启自动隐藏 (AUTO_ADD)") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    Shell.cmd("su -c 'echo 0 > /sys/kernel/susfs/enable'").exec()
                }, modifier = Modifier.fillMaxWidth()) { Text("关闭隐藏") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            }
        }
    }
}
