package com.huijui.nova.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huijui.nova.design.HuiSpace
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.design.LocalHuiShapes
import com.huijui.nova.design.LocalHuiTypography
import com.huijui.nova.motion.HuiEase
import com.huijui.nova.motion.huiPress

/* ------------------------------------------------------------------ */
/*  文本                                                               */
/* ------------------------------------------------------------------ */

@Composable
fun HuiText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalHuiColors.current.text,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = color,
            textAlign = align ?: style.textAlign,
            // 玻璃是浅色的、背后是照片，纯色字会糊；加一层极轻的投影拉对比
            shadow = androidx.compose.ui.graphics.Shadow(
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.38f),
                offset = androidx.compose.ui.geometry.Offset(0f, 1.2f),
                blurRadius = 5f,
            ),
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun HuiSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(
                    Brush.verticalGradient(listOf(colors.accent, colors.accentAlt)),
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(Modifier.width(8.dp))
        HuiText(title, type.label, color = colors.textDim, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (onTrailingClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTrailingClick,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                HuiText(trailing, type.micro, color = colors.textFaint)
                HuiIcon(HuiIcons.ChevronRight, size = 11.dp, tint = colors.textFaint)
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  色调节点                                                           */
/* ------------------------------------------------------------------ */

enum class HuiTone { Accent, Positive, Warning, Danger, Neutral }

@Composable
fun huiToneColor(tone: HuiTone): Color {
    val c = LocalHuiColors.current
    return when (tone) {
        HuiTone.Accent -> c.accent
        HuiTone.Positive -> c.positive
        HuiTone.Warning -> c.warning
        HuiTone.Danger -> c.danger
        HuiTone.Neutral -> c.textDim
    }
}

@Composable
fun HuiBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: HuiTone = HuiTone.Accent,
    icon: HuiGlyph? = null,
) {
    val color = huiToneColor(tone)
    val type = LocalHuiTypography.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.28f)), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) HuiIcon(icon, size = 11.dp, tint = color)
        HuiText(text, type.micro, color = color, maxLines = 1)
    }
}

/* ------------------------------------------------------------------ */
/*  按钮                                                               */
/* ------------------------------------------------------------------ */

enum class HuiButtonStyle { Primary, Glass, Ghost, Danger }

@Composable
fun HuiButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: HuiGlyph? = null,
    style: HuiButtonStyle = HuiButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    val shapes = LocalHuiShapes.current
    val interaction = remember { MutableInteractionSource() }

    val background = when (style) {
        HuiButtonStyle.Primary -> Brush.linearGradient(
            listOf(colors.accent, colors.accentGlow),
        )
        HuiButtonStyle.Glass -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)),
        )
        HuiButtonStyle.Ghost -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        HuiButtonStyle.Danger -> Brush.linearGradient(listOf(colors.danger, colors.danger.copy(alpha = 0.72f)))
    }
    val contentColor = when (style) {
        HuiButtonStyle.Primary, HuiButtonStyle.Danger -> if (colors.isDark) Color(0xFF0B0B10) else Color.White
        HuiButtonStyle.Glass -> colors.text
        HuiButtonStyle.Ghost -> colors.textDim
    }

    Box(
        modifier = modifier
            .huiPress(interaction, pressedScale = 0.955f)
            .clip(shapes.pill)
            .background(background)
            .then(
                if (style == HuiButtonStyle.Glass || style == HuiButtonStyle.Ghost) {
                    Modifier.border(
                        BorderStroke(1.dp, colors.outline),
                        shapes.pill,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (icon != null) HuiIcon(icon, size = 15.dp, tint = contentColor)
            HuiText(
                label,
                type.subtitle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = contentColor.copy(alpha = if (enabled) 1f else 0.4f),
                maxLines = 1,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  开关                                                               */
/* ------------------------------------------------------------------ */

@Composable
fun HuiToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHuiColors.current
    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.textFaint.copy(alpha = 0.25f),
        animationSpec = tween(220, easing = HuiEase.Standard.curve),
        label = "track",
    )
    val knob by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(260, easing = HuiEase.Springy.curve),
        label = "knob",
    )
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 27.dp)
            .clip(RoundedCornerShape(50))
            .background(track.copy(alpha = if (checked) 0.95f else 1f))
            .border(BorderStroke(1.dp, colors.outlineSoft), RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = 3.dp + (knob * 19).dp)
                .size(21.dp)
                .clip(RoundedCornerShape(50))
                .background(if (colors.isDark) Color(0xFFF6F6FA) else Color.White),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  分段选择器                                                         */
/* ------------------------------------------------------------------ */

@Composable
fun <T> HuiSegmented(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelOf: (T) -> String = { it.toString() },
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    val shapes = LocalHuiShapes.current
    Row(
        modifier = modifier
            .clip(shapes.pill)
            .background(colors.text.copy(alpha = 0.06f))
            .border(BorderStroke(1.dp, colors.outlineSoft), shapes.pill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val weight by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(240, easing = HuiEase.Emphasized.curve),
                label = "seg$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.pill)
                    .background(
                        if (weight > 0f) colors.accent.copy(alpha = 0.20f * weight) else Color.Transparent,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                HuiText(
                    labelOf(option),
                    type.label,
                    color = if (selected) colors.text else colors.textFaint,
                    maxLines = 1,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  进度与图表                                                         */
/* ------------------------------------------------------------------ */

@Composable
fun HuiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    color: Color = LocalHuiColors.current.accent,
    trackColor: Color = LocalHuiColors.current.text.copy(alpha = 0.10f),
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = HuiEase.Emphasized.curve),
        label = "progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.85f), color),
                    ),
                ),
        )
    }
}

@Composable
fun HuiSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = LocalHuiColors.current.accentAlt,
    strokeWidth: Dp = 1.8.dp,
    fill: Boolean = true,
) {
    if (values.size < 2) return
    Canvas(modifier) {
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val span = (max - min).takeIf { it > 0.0001f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / span) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (fill) {
            val area = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.28f), Color.Transparent),
                ),
            )
        }
        drawPath(
            path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
        )
        val last = values.last()
        val lastY = size.height - ((last - min) / span) * size.height
        drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(size.width, lastY))
    }
}

/* ------------------------------------------------------------------ */
/*  杂项                                                               */
/* ------------------------------------------------------------------ */

@Composable
fun HuiDivider(modifier: Modifier = Modifier, alpha: Float = 0.08f) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, LocalHuiColors.current.text.copy(alpha = alpha), Color.Transparent),
                ),
            ),
    )
}

@Composable
fun HuiEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: HuiGlyph = HuiIcons.Folder,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.text.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            HuiIcon(icon, size = 24.dp, tint = colors.textFaint)
        }
        Spacer(Modifier.height(14.dp))
        HuiText(title, type.subtitle, color = colors.textDim, align = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        HuiText(subtitle, type.bodySmall, color = colors.textFaint, align = TextAlign.Center)
    }
}


/** 横向可滚动的胶囊选择器：选项多的时候用它，避免分段器被挤扁。 */
@Composable
fun HuiChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.94f,
                animationSpec = tween(220, easing = HuiEase.Emphasized.curve),
                label = "chip$index",
            )
            Box(
                Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) colors.accent.copy(alpha = 0.22f) else colors.text.copy(alpha = 0.05f))
                    .border(
                        BorderStroke(1.dp, if (selected) colors.accent.copy(alpha = 0.45f) else colors.outlineSoft),
                        RoundedCornerShape(50),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                HuiText(
                    label,
                    type.label,
                    color = if (selected) colors.text else colors.textFaint,
                    maxLines = 1,
                )
            }
        }
    }
}
