package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bakasu.bakasu.ui.theme.CardConfig
import org.koin.compose.koinInject
import org.bakasu.bakasu.ui.theme.blurEffect

@Composable
fun HuijuiHubPage(bottomPadding: Dp = 0.dp) {
    // 核心：注入全局卡片配置
    val cardConfig = koinInject<CardConfig>()

    var showSelinux by remember { mutableStateOf(false) }
    var showDpi by remember { mutableStateOf(false) }
    var showTcp by remember { mutableStateOf(false) }
    var showDropCaches by remember { mutableStateOf(false) }
    var showBattery by remember { mutableStateOf(false) }
    var showCpu by remember { mutableStateOf(false) }
    var showThermal by remember { mutableStateOf(false) }
    var showLogcat by remember { mutableStateOf(false) }
    var showGpu by remember { mutableStateOf(false) }
    var showRescue by remember { mutableStateOf(false) }
    var showSusfs by remember { mutableStateOf(false) }
    var showWebAdmin by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(80.dp)) }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "HuiJuiSU Ultra 极客工具箱",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .blurEffect(),
                    shape = RoundedCornerShape(24.dp),
                    // 核心：透明度联动全局滑块
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        HuijuiToolCard(icon = Icons.TwoTone.Security, iconTint = MaterialTheme.colorScheme.primary, title = "SELinux 控制器", description = "Enforcing / Permissive 一键切换", onClick = { showSelinux = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Memory, iconTint = MaterialTheme.colorScheme.primary, title = "CPU 性能调度", description = "一键切换 CPU governor 调度策略", onClick = { showCpu = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Whatshot, iconTint = MaterialTheme.colorScheme.error, title = "系统温控开关", description = "一键停用系统温控服务（极客专用）", onClick = { showThermal = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Terminal, iconTint = MaterialTheme.colorScheme.primary, title = "Logcat 日志抓取", description = "一键抓取底层系统日志排查 Bug", onClick = { showLogcat = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Tune, iconTint = MaterialTheme.colorScheme.primary, title = "屏幕 DPI 修改", description = "突破系统限制，自由调整显示 DPI", onClick = { showDpi = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.NetworkCheck, iconTint = MaterialTheme.colorScheme.primary, title = "TCP 拥塞控制", description = "切换 BBR / CUBIC，优化网络延迟", onClick = { showTcp = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.CleaningServices, iconTint = MaterialTheme.colorScheme.primary, title = "内存与缓存清理", description = "底层 drop_caches，极速释放 RAM", onClick = { showDropCaches = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.BatteryChargingFull, iconTint = MaterialTheme.colorScheme.primary, title = "电池健康与温度", description = "直读底层节点，查看循环次数", onClick = { showBattery = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Memory, iconTint = MaterialTheme.colorScheme.primary, title = "GPU 极限锁频", description = "一键锁定最高频，释放独显性能", onClick = { showGpu = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.SaveAlt, iconTint = MaterialTheme.colorScheme.primary, title = "分区备份救援", description = "一键备份 boot/dtbo 等核心分区防变砖", onClick = { showRescue = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Security, iconTint = MaterialTheme.colorScheme.primary, title = "SUSFS 深度隐藏", description = "内核级 Root 隐藏，躲避检测", onClick = { showSusfs = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Computer, iconTint = MaterialTheme.colorScheme.primary, title = "网页管理 (WebUI)", description = "通过局域网在电脑上管理手机", onClick = { showWebAdmin = true })
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(bottomPadding + 120.dp)) }
    }

    if (showSelinux) SelinuxDialog(onDismiss = { showSelinux = false })
    if (showDpi) DpiDialog(onDismiss = { showDpi = false })
    if (showTcp) TcpDialog(onDismiss = { showTcp = false })
    if (showDropCaches) DropCachesDialog(onDismiss = { showDropCaches = false })
    if (showBattery) BatteryHealthDialog(onDismiss = { showBattery = false })
    if (showCpu) CpuGovernorDialog(onDismiss = { showCpu = false })
    if (showThermal) ThermalDialog(onDismiss = { showThermal = false })
    if (showLogcat) LogcatDialog(onDismiss = { showLogcat = false })
    if (showGpu) GpuControlDialog(onDismiss = { showGpu = false })
    if (showRescue) RescueBackupDialog(onDismiss = { showRescue = false })
    if (showSusfs) SusfsDialog(onDismiss = { showSusfs = false })
    if (showWebAdmin) WebAdminDialog(onDismiss = { showWebAdmin = false })
}
