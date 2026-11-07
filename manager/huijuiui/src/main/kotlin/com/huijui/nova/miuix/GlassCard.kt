package com.huijui.nova.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.design.LocalNovaDark
import com.huijui.nova.design.LocalNovaUiMode
import com.huijui.nova.design.NovaUiMode
import com.huijui.nova.design.LocalNovaGlass
import com.huijui.nova.design.NovaGlass

/**
 * 玻璃卡。
 *
 * 刻意保持极简：一个 Box、一层渐变底、一圈渐变描边。
 * 不用 graphicsLayer、不用 drawWithContent、不用 shadow —— 之前那套组合
 * 在部分 GPU 上会在卡片内部渲染出一块浅色矩形（用户截图里的「白块」）。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    alpha: Float = 0.38f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val style = LocalNovaGlass.current
    val dark = LocalNovaDark.current
    val accent = LocalHuiColors.current.accent
    val shape = RoundedCornerShape(corner)
    val interaction = remember { MutableInteractionSource() }

    val minimal = com.huijui.nova.design.LocalNovaUiMode.current == com.huijui.nova.design.NovaUiMode.Minimal
    val base = when (style) {
        NovaGlass.SoftGlow -> if (dark) alpha * 0.55f else if (minimal) alpha + 0.22f else alpha
        NovaGlass.ClearFrost -> if (dark) alpha * 0.40f else if (minimal) alpha + 0.12f else alpha * 0.62f
    }

    // 壁纸模式：深色玻璃（黑底 + 顶部高光），比白色磨砂更通透也更清晰
    val wallpaper = LocalNovaUiMode.current == NovaUiMode.Wallpaper || LocalNovaDark.current
    val topColor = if (wallpaper) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = (base + 0.10f).coerceAtMost(1f))
    val midColor = if (wallpaper) Color(0xFF0B1020).copy(alpha = 0.24f) else Color.White.copy(alpha = base)
    val botColor = if (wallpaper) Color(0xFF0B1020).copy(alpha = 0.32f) else Color.White.copy(alpha = (base - 0.08f).coerceAtLeast(0.02f))

    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, midColor, botColor)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (style == NovaGlass.SoftGlow) 0.55f else 0.85f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = if (dark) 0.25f else 0.10f),
                    ),
                ),
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(padding),
    ) {
        content()
    }
}
