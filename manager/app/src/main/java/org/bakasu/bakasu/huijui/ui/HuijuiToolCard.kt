package org.bakasu.bakasu.huijui.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.bakasu.bakasu.ui.component.settings.SettingsBaseWidget

/**
 * HuijuiSU 工具卡片 v2.0
 *
 * 直接包装 SettingsBaseWidget，自动走项目的背景模糊系统，
 * 与主页其他设置项风格完全一致。
 */
@Composable
fun HuijuiToolCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    SettingsBaseWidget(
        icon = icon,
        iconPlaceholder = false,
        iconColor = iconTint,
        title = title,
        description = description,
        isOnBackground = true,
        onClick = { onClick() },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}
