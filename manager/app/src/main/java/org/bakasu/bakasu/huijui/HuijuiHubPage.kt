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

@Composable
fun HuijuiHubPage(bottomPadding: Dp = 0.dp) {
    var showSelinux by remember { mutableStateOf(false) }
    var showDpi by remember { mutableStateOf(false) }
    var showTcp by remember { mutableStateOf(false) }
    var showDropCaches by remember { mutableStateOf(false) }
    var showBatteryHealth by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(80.dp)) }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "HuiJuiSU Ultra 工具", 
                    style = MaterialTheme.typography.titleSmall, 
                    color = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 用一个大 Card 包裹，实现一体化
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    // 关键点：降低颜色浓度，增加透明度，让它透出背景
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    )
                ) {
                    Column {
                        HuijuiToolCard(icon = Icons.TwoTone.Security, iconTint = MaterialTheme.colorScheme.primary, title = "SELinux 控制器", description = "Enforcing / Permissive 一键切换", onClick = { showSelinux = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        HuijuiToolCard(icon = Icons.TwoTone.Tune, iconTint = MaterialTheme.colorScheme.primary, title = "屏幕 DPI 修改", description = "突破系统限制，自由调整显示 DPI", onClick = { showDpi = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        HuijuiToolCard(icon = Icons.TwoTone.NetworkCheck, iconTint = MaterialTheme.colorScheme.primary, title = "TCP 拥塞控制", description = "切换 BBR / CUBIC，优化网络延迟", onClick = { showTcp = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        HuijuiToolCard(icon = Icons.TwoTone.CleaningServices, iconTint = MaterialTheme.colorScheme.primary, title = "内存与缓存清理", description = "底层 drop_caches，极速释放 RAM", onClick = { showDropCaches = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        HuijuiToolCard(icon = Icons.TwoTone.BatteryChargingFull, iconTint = MaterialTheme.colorScheme.primary, title = "电池健康与温度", description = "直读底层节点，查看循环次数", onClick = { showBatteryHealth = true })
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
    if (showBatteryHealth) BatteryHealthDialog(onDismiss = { showBatteryHealth = false })
}
