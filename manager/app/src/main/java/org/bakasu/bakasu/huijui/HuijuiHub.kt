package org.bakasu.bakasu.huijui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Build
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.SaveAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SettingsBaseWidget

@Composable
fun HuijuiHub() {
    var showCheckup by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var showRescue by remember { mutableStateOf(false) }

    SegmentedColumn(
        modifier = Modifier.fillMaxWidth(),
        title = "Huijui 工具",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Favorite,
                iconPlaceholder = false,
                title = "一键体检",
                description = "电量 / 温度 / BL / 内核 / Root / SELinux / 存储",
                onClick = { showCheckup = true },
            )
        }
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.SaveAlt,
                iconPlaceholder = false,
                title = "一键备份包",
                description = "备份 boot + init_boot + vbmeta，附带 restore.bat",
                onClick = { showBackup = true },
            )
        }
        item {
            SettingsBaseWidget(
                icon = Icons.TwoTone.Build,
                iconPlaceholder = false,
                title = "一键救砖",
                description = "从历史备份目录一键刷回",
                onClick = { showRescue = true },
            )
        }
    }

    if (showCheckup) HuijuiCheckup(onDismiss = { showCheckup = false })
    if (showBackup) HuijuiBackup(onDismiss = { showBackup = false })
    if (showRescue) HuijuiRescue(onDismiss = { showRescue = false })
}
