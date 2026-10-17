package com.huijui.sunext.huijui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.Build
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.SaveAlt
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huijui.sunext.huijui.feature.HuijuiAppManager
import com.huijui.sunext.huijui.feature.HuijuiBackup
import com.huijui.sunext.huijui.feature.HuijuiCheckup
import com.huijui.sunext.huijui.feature.HuijuiFlashCustom
import com.huijui.sunext.huijui.feature.HuijuiPerfMode
import com.huijui.sunext.huijui.feature.HuijuiRescue
import com.huijui.sunext.huijui.ui.HuijuiGkiDownload
import com.huijui.sunext.huijui.ui.HuijuiToolCard
import com.huijui.sunext.ui.component.settings.SegmentedColumn

/**
 * HuiJuiSU NEXT 工具 Hub 页
 *
 * 独立 tab 页面，集合所有内置工具入口。
 */
@Composable
fun HuijuiHubPage(bottomPadding: Dp = 0.dp) {
    var showCheckup by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var showRescue by remember { mutableStateOf(false) }
    var showPerfMode by remember { mutableStateOf(false) }
    var showGkiDownload by remember { mutableStateOf(false) }
    var showFlashCustom by remember { mutableStateOf(false) }
    var showAppManager by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
        item {
            SegmentedColumn(
                modifier = Modifier.fillMaxWidth(),
                title = "HuiJuiSU 工具",
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.Favorite,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "一键体检",
                        description = "电量 / 温度 / BL / 内核 / Root / SELinux / 存储",
                        onClick = { showCheckup = true },
                    )
                }
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.Speed,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "性能模式",
                        description = "省电 / 均衡 / 性能 / 游戏，一键切换系统调度",
                        onClick = { showPerfMode = true },
                    )
                }
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.SaveAlt,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "一键备份包",
                        description = "备份 boot + init_boot + vbmeta，附带 restore.bat",
                        onClick = { showBackup = true },
                    )
                }
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.Build,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "一键救砖",
                        description = "从历史备份目录一键刷回",
                        onClick = { showRescue = true },
                    )
                }
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.Cloud,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "GKI 内核下载",
                        description = "内置模式（built-in）专用，跳转社区预编译内核",
                        onClick = { showGkiDownload = true },
                    )
                }
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.Memory,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "刷入定制内核",
                        description = "刷自己的 boot.img，自动处理 vbmeta 校验",
                        onClick = { showFlashCustom = true },
                    )
                }
                item {
                    HuijuiToolCard(
                        icon = Icons.TwoTone.Apps,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "应用管理器",
                        description = "冻结 / 卸载 / 强停 已安装应用",
                        onClick = { showAppManager = true },
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(bottomPadding + 24.dp))
        }
    }

    if (showCheckup) HuijuiCheckup(onDismiss = { showCheckup = false })
    if (showPerfMode) HuijuiPerfMode(onDismiss = { showPerfMode = false })
    if (showBackup) HuijuiBackup(onDismiss = { showBackup = false })
    if (showRescue) HuijuiRescue(onDismiss = { showRescue = false })
    if (showGkiDownload) HuijuiGkiDownload(onDismiss = { showGkiDownload = false })
    if (showFlashCustom) HuijuiFlashCustom(onDismiss = { showFlashCustom = false })
    if (showAppManager) HuijuiAppManager(onDismiss = { showAppManager = false })
}
