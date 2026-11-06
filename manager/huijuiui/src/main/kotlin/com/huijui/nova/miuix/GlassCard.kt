package com.huijui.nova.miuix

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.design.LocalNovaDark
import com.huijui.nova.design.LocalNovaGlass
import com.huijui.nova.design.NovaGlass

/**
 * 玻璃卡。
 *
 * 之前用 `Modifier.shadow` 做投影，模拟器上会把投影画成一个实心白方块压在卡片里
 * （截图里那块白斑）。现在全部改成自绘：
 *
 * 1. 底色：竖向渐变，半透明白，上亮下暗 —— 不画投影，避免再次出方块；
 * 2. 外发光：一圈柔和的强调色晕，用 3dp 描边模拟，不占额外图层；
 * 3. 边缘高光：顶部 1dp 白线，底部 1dp 暗线，玻璃的「厚度」来自这里。
 *
 * 透明度整体压到 30% 上下 —— 目标是「看得穿」，不是「白纸」。
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
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.978f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 720f),
        label = "cardPress",
    )
    val radiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { corner.toPx() }

    // 简约模式底下是纯色，太透会「消失」，这里加一档让卡片有存在感
    val minimal = com.huijui.nova.design.LocalNovaUiMode.current == com.huijui.nova.design.NovaUiMode.Minimal
    val baseAlpha = when (style) {
        NovaGlass.SoftGlow -> if (dark) alpha * 0.55f else if (minimal) alpha + 0.22f else alpha
        NovaGlass.ClearFrost -> if (dark) alpha * 0.40f else if (minimal) alpha + 0.10f else alpha * 0.62f
    }
    val ambient = if (style == NovaGlass.SoftGlow) accent.copy(alpha = 0.05f) else Color.Transparent

    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = (baseAlpha + 0.10f).coerceAtMost(1f)),
                        Color.White.copy(alpha = baseAlpha),
                        Color.White.copy(alpha = (baseAlpha - 0.08f).coerceAtLeast(0.02f)),
                    ).map { it.compositeOver(ambient) },
                ),
            )
            .drawWithContent {
                drawContent()

                val w = size.width
                val h = size.height
                val r = CornerRadius(radiusPx, radiusPx)
                val inset = 0.75.dp.toPx()
                val strokeInset = Size(w - inset * 2, h - inset * 2)

                // 外发光：贴着边缘的一圈强调色，扩散感靠低透明度 + 宽描边
                if (style == NovaGlass.SoftGlow) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.22f),
                                Color.Transparent,
                                accent.copy(alpha = 0.12f),
                            ),
                        ),
                        topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                        size = Size(w + 2.dp.toPx(), h + 2.dp.toPx()),
                        cornerRadius = CornerRadius(radiusPx + 1.dp.toPx(), radiusPx + 1.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }

                // 边缘高光：顶部亮、底部暗
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (style == NovaGlass.SoftGlow) 0.55f else 0.85f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = if (dark) 0.30f else 0.10f),
                        ),
                    ),
                    topLeft = Offset(inset, inset),
                    size = strokeInset,
                    cornerRadius = CornerRadius(radiusPx - inset, radiusPx - inset),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
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

private fun Color.compositeOver(over: Color): Color {
    if (over.alpha == 0f) return this
    val a = alpha + over.alpha * (1 - alpha)
    if (a == 0f) return Color.Transparent
    val r = (red * alpha + over.red * over.alpha * (1 - alpha)) / a
    val g = (green * alpha + over.green * over.alpha * (1 - alpha)) / a
    val b = (blue * alpha + over.blue * over.alpha * (1 - alpha)) / a
    return Color(r, g, b, a)
}
