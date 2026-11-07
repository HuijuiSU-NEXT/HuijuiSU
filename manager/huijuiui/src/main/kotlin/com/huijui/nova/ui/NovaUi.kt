package com.huijui.nova.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NovaUI —— 自研组件层，只依赖 Compose foundation。
 *
 * 为什么不引第三方组件库：
 * - 组件库的 AAR 在 AGP 9 下有变体/重复类风险（Miuix 就是这么炸的）
 * - foundation 是 Compose 官方最底层、最稳定的 API，升级 Compose 不会破
 * - 我们的界面本来就是玻璃卡片风格，通用组件库帮不上太多，反而限制造型
 *
 * 这里只提供真正会重复用到的：文本、开关、胶囊按钮。
 */

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            letterSpacing = if (letterSpacing == TextUnit.Unspecified) androidx.compose.ui.unit.TextUnit.Unspecified else letterSpacing,
        ),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

/** 胶囊开关：带弹簧动画，比三方 Switch 更轻。 */
@Composable
fun NovaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Color(0xFF2E9E6B),
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "switch",
    )
    val track = if (enabled) accent.copy(alpha = 0.25f + 0.55f * progress) else Color.White.copy(alpha = 0.2f)
    val knob = Color.White.copy(alpha = if (enabled) 0.95f else 0.5f)
    val source = remember { MutableInteractionSource() }

    Box(
        modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(50))
                .background(knob)
                .then(Modifier.padding(start = (18 * progress).dp)),
        )
    }
}

/** 胶囊按钮：低调的玻璃底 + 强调色文字，用在“应用 / 进入 / 切换”这类次要动作。 */
@Composable
fun NovaPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2E9E6B),
    filled: Boolean = false,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color.copy(alpha = 0.85f) else color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (filled) Color.White else color,
        )
    }
}

/** 统一的圆角/间距刻度，避免各页面各写一套。 */
object NovaTokens {
    val cardCorner: Dp = 18.dp
    val cardPadding: Dp = 16.dp
    val rowGap: Dp = 10.dp
    val titleSize: TextUnit = 24.sp
    val sectionSize: TextUnit = 11.sp
    val bodySize: TextUnit = 13.sp
    val captionSize: TextUnit = 9.sp
}

/** 兼容旧调用：主题容器现在是空壳（颜色由各卡片自己决定）。 */
@Composable
fun MiuixTheme(content: @Composable () -> Unit) = content()

@Composable
fun MiuixTheme(colors: Any?, content: @Composable () -> Unit) = content()

object NovaColorScheme

fun lightColorScheme(): NovaColorScheme = NovaColorScheme
