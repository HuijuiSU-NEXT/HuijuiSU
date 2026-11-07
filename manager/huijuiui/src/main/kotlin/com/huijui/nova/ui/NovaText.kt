package com.huijui.nova.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 自研文本组件。
 *
 * 之前用的是 Miuix 的 `Text`。Miuix 0.9.4 的 KMP 变体在 AGP 9 下会被 dex 两次，
 * release 合并必挂，所以把 Nova 界面需要的三个组件（Text / MiuixTheme / lightColorScheme）
 * 自己实现一份，彻底去掉对 Miuix 的依赖。
 *
 * 签名刻意和原来对齐，调用点不用改。
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = if (fontSize == TextUnit.Unspecified) 14.sp else fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
        ),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

/** 兼容旧的 `MiuixTheme(colors = lightColorScheme()) { ... }`：直接渲染内容。 */
object NovaColorScheme

fun lightColorScheme(): NovaColorScheme = NovaColorScheme

@Composable
fun MiuixTheme(
    colors: NovaColorScheme = NovaColorScheme,
    content: @Composable () -> Unit,
) {
    content()
}
