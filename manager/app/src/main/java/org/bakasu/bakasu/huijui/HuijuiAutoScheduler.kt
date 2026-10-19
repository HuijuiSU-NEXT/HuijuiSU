package org.bakasu.bakasu.huijui

import android.app.Application
import android.os.PowerManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AutoSchedulerManager {
    private var isRunning = false
    private var job: kotlinx.coroutines.Job? = null
    private var currentScene = "等待检测"

    fun start(application: Application, onSceneChanged: (String) -> Unit) {
        if (isRunning) return
        isRunning = true
        val powerManager = application.getSystemService(Application.POWER_SERVICE) as PowerManager

        job = CoroutineScope(Dispatchers.IO).launch {
            var lastPkg = ""
            while (isRunning) {
                try {
                    val isScreenOn = powerManager.isInteractive
                    var newScene = ""

                    if (!isScreenOn) {
                        newScene = "熄屏省电"
                        if (currentScene != newScene) {
                            Shell.cmd("echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec()
                            Shell.cmd("echo powersave > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor").exec()
                            Shell.cmd("echo powersave > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor").exec()
                        }
                    } else {
                        // 获取前台应用包名 (适用于大多数高版本安卓)
                        val topPkg = Shell.cmd("dumpsys activity activities | grep mResumedActivity | tail -1 | awk -F ' ' '{print \$4}' | cut -d'/' -f1").exec().out.firstOrNull()?.trim() ?: ""
                        
                        // 简单的游戏场景识别（可扩充白名单）
                        val isGame = topPkg.contains("game") || topPkg.contains("tmgp") || topPkg.contains("mihoyo") || topPkg.contains("tencent")
                        
                        if (isGame) {
                            newScene = "电竞满血模式"
                            if (currentScene != newScene) {
                                Shell.cmd("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec()
                                Shell.cmd("echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor").exec()
                                Shell.cmd("echo performance > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor").exec()
                                Shell.cmd("stop thermal-engine; stop thermald").exec()
                            }
                        } else {
                            newScene = "日常均衡模式"
                            if (currentScene != newScene) {
                                Shell.cmd("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec()
                                Shell.cmd("echo schedutil > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor").exec()
                                Shell.cmd("echo schedutil > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor").exec()
                                Shell.cmd("start thermal-engine; start thermald").exec()
                            }
                        }
                    }
                    
                    if (currentScene != newScene) {
                        currentScene = newScene
                        withContext(Dispatchers.Main) { onSceneChanged(currentScene) }
                    }
                } catch (e: Exception) {
                    // ignore
                }
                delay(3000) // 每 3 秒检测一次
            }
        }
    }

    fun stop() {
        isRunning = false
        job?.cancel()
        currentScene = "已关闭"
        Shell.cmd("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").exec()
        Shell.cmd("start thermal-engine; start thermald").exec()
    }
}

@Composable
fun AutoSchedulerDialog(onDismiss: () -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("引擎未启动") }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.TwoTone.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("至尊 AI 自动调度", style = MaterialTheme.typography.titleLarge)
                        Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                
                Text("智能识别前台应用与屏幕状态：游戏满血、日常均衡、熄屏省电。无需手动干预。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("启用自动调度引擎", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            if (checked) {
                                AutoSchedulerManager.start(context) { scene ->
                                    statusText = scene
                                }
                            } else {
                                AutoSchedulerManager.stop()
                                statusText = "引擎已关闭"
                            }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            }
        }
    }
}
