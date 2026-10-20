package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.koin.compose.koinInject

@Composable
fun HuijuiHubPage(bottomPadding: Dp = 0.dp) {
    val themeConfig = koinInject<ThemeConfig>()
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
    var showSchedule by remember { mutableStateOf(false) }
    var showMonitor by remember { mutableStateOf(false) }

    // 完全复刻主页：外层必须包 blurSource()，提供抓取背景的锚点
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

                // 完全复刻主页的玻璃卡片写法
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        , // 开启毛玻璃
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        // 主页的逻辑是：开启了模糊就用全透明，没开启就用带透明度的纯色
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.SaveAlt, iconTint = MaterialTheme.colorScheme.primary, title = "分区备份救援", description = "一键备份 boot/dtbo 等核心分区防变砖", onClick = { showRescue = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HuijuiToolCard(icon = Icons.TwoTone.Security, iconTint = MaterialTheme.colorScheme.primary, title = "SUSFS 深度隐藏", description = "内核级 Root 隐藏，躲避检测", onClick = { showSusfs = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        // [已安全切除] 高危/无用功能入口已隐藏
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
    if (showMonitor) HardwareMonitorDialog(onDismiss = { showMonitor = false })
    if (showSchedule) AutoSchedulerDialog(onDismiss = { showSchedule = false })
}
