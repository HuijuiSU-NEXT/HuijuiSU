package com.huijui.nova.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------------------------------------------------------------------
 *  HuiJui Nova · 设计令牌
 *  这是一套完全独立于 Material Design / BakaSU 的设计语言。
 *  颜色、字阶、形状、间距全部自定义，不引用任何 material3 组件或 ColorScheme。
 * ------------------------------------------------------------------------- */

/** 三套强调色种子，决定整个界面的气味。 */
enum class HuiAccent(val label: String, val labelEn: String) {
    Aurora("极光", "Aurora"),
    Ember("熔岩", "Ember"),
    Mint("薄荷", "Mint"),
    Glacier("冰川", "Glacier"),
    Mono("石墨", "Mono"),
    Miku("初音", "Miku"),
}

@Immutable
data class HuiColors(
    val bg: Color,
    val bgDeep: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceFlat: Color,
    val outline: Color,
    val outlineSoft: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val accent: Color,
    val accentAlt: Color,
    val accentDeep: Color,
    val accentGlow: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color,
    val glassTint: Color,
    val glassTintStrong: Color,
    val sheen: Color,
    val shadow: Color,
    val isDark: Boolean,
)

private val Obsidian = Color(0xFF07070B)
private val ObsidianDeep = Color(0xFF030305)

/** 按强调色 + 明暗生成完整色板。所有数值都是手调的，不经过 tonal palette 算法。 */
fun huiColors(accent: HuiAccent, dark: Boolean = true): HuiColors {
    val a = when (accent) {
        HuiAccent.Aurora -> Triple(Color(0xFF9C8CFF), Color(0xFF57E0FF), Color(0xFFB9A8FF))
        HuiAccent.Ember -> Triple(Color(0xFFFF9A62), Color(0xFFFF6B95), Color(0xFFFFC48A))
        HuiAccent.Mint -> Triple(Color(0xFF54E8B4), Color(0xFF7BE0FF), Color(0xFFA6FFDC))
        HuiAccent.Glacier -> Triple(Color(0xFF7FB8FF), Color(0xFFB7E6FF), Color(0xFF9ED0FF))
        HuiAccent.Mono -> Triple(Color(0xFFE8E8EE), Color(0xFFA9A9B8), Color(0xFFCFCFD8))
        // 初音配色：青绿 #39C5BB 为主，洋红 #E12885 做副调
        HuiAccent.Miku -> Triple(Color(0xFF39C5BB), Color(0xFFE12885), Color(0xFF7DF3EA))
    }
    return if (dark) {
        HuiColors(
            bg = Obsidian,
            bgDeep = ObsidianDeep,
            surface = Color(0xFF111118),
            surfaceHigh = Color(0xFF1A1A24),
            surfaceFlat = Color(0xFF14141C),
            outline = Color(0x33FFFFFF),
            outlineSoft = Color(0x14FFFFFF),
            text = Color(0xFFF4F4F8),
            textDim = Color(0xA8E8E8F0),
            textFaint = Color(0x66D8D8E4),
            accent = a.first,
            accentAlt = a.second,
            accentDeep = Color(0xFF3A2E7A),
            accentGlow = a.third,
            positive = Color(0xFF5FE3A1),
            warning = Color(0xFFFFC857),
            danger = Color(0xFFFF6E7A),
            glassTint = Color(0x14FFFFFF),
            glassTintStrong = Color(0x22FFFFFF),
            sheen = Color(0x40FFFFFF),
            shadow = Color(0xCC000000),
            isDark = true,
        )
    } else {
        HuiColors(
            bg = Color(0xFFF6F6FA),
            bgDeep = Color(0xFFECECF3),
            surface = Color(0xFFFFFFFF),
            surfaceHigh = Color(0xFFF1F1F7),
            surfaceFlat = Color(0xFFFAFAFD),
            outline = Color(0x1A000000),
            outlineSoft = Color(0x0D000000),
            text = Color(0xFF14141B),
            textDim = Color(0xA814141B),
            textFaint = Color(0x6614141B),
            accent = a.first,
            accentAlt = a.second,
            accentDeep = Color(0xFFD8D4FF),
            accentGlow = a.third,
            positive = Color(0xFF14985F),
            warning = Color(0xFFB47700),
            danger = Color(0xFFD63B4B),
            glassTint = Color(0x66FFFFFF),
            glassTintStrong = Color(0x99FFFFFF),
            sheen = Color(0x66FFFFFF),
            shadow = Color(0x33000000),
            isDark = false,
        )
    }
}

@Immutable
data class HuiTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val micro: TextStyle,
    val numeric: TextStyle,
) {
    companion object {
        val Default = HuiTypography(
            display = TextStyle(
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.4).sp,
                fontFamily = FontFamily.SansSerif,
            ),
            headline = TextStyle(
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                fontFamily = FontFamily.SansSerif,
            ),
            title = TextStyle(
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                fontFamily = FontFamily.SansSerif,
            ),
            subtitle = TextStyle(
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            body = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.05.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            bodySmall = TextStyle(
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.1.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            label = TextStyle(
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            micro = TextStyle(
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            numeric = TextStyle(
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Start,
            ),
        )
    }
}

@Immutable
data class HuiShapes(
    val hero: Shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomEnd = 36.dp, bottomStart = 14.dp),
    val card: Shape = RoundedCornerShape(28.dp),
    val tile: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 10.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),
    val sheet: Shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
    val tiny: Shape = RoundedCornerShape(12.dp),
    val notch: Shape = RoundedCornerShape(topStart = 8.dp, topEnd = 28.dp, bottomEnd = 8.dp, bottomStart = 28.dp),
)

object HuiSpace {
    val xxs: Dp = 3.dp
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 16.dp
    val lg: Dp = 22.dp
    val xl: Dp = 30.dp
    val xxl: Dp = 44.dp
    val gutter: Dp = 18.dp
}

val LocalHuiColors = staticCompositionLocalOf { huiColors(HuiAccent.Aurora) }
val LocalHuiTypography = staticCompositionLocalOf { HuiTypography.Default }
val LocalHuiShapes = staticCompositionLocalOf { HuiShapes() }
