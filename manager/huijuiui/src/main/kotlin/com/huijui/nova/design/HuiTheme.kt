package com.huijui.nova.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 三种材质。整个 Nova 界面的所有卡片、面板、导航条都从这三种材质里取
 * 表面渲染方式，而不是像 Material 那样统一用 tonal elevation。
 */
enum class HuiMaterial(
    val label: String,
    val labelEn: String,
    val description: String,
    /** 背景模糊半径（dp）。纯色为 0。 */
    val blur: Float,
    /** 玻璃底色叠加透明度。 */
    val tintAlpha: Float,
    /** 高光/反射条强度。 */
    val sheen: Float,
    /** 是否启用 AGSL 折射（液态玻璃）。 */
    val refract: Boolean,
) {
    GlossyGlass(
        label = "高光玻璃",
        labelEn = "Glossy Glass",
        description = "低雾面 + 强镜面高光，像一块抛光过的玻璃压在画面上",
        blur = 18f,
        tintAlpha = 0.035f,
        sheen = 1.35f,
        refract = false,
    ),
    SoftGlass(
        label = "柔光玻璃",
        labelEn = "Soft Glass",
        description = "大半径模糊 + 均匀雾面，像隔着一层磨砂亚克力看背景",
        blur = 26f,
        tintAlpha = 0.065f,
        sheen = 0.45f,
        refract = false,
    ),
    LiquidGlass(
        label = "液态玻璃",
        labelEn = "Liquid Glass",
        description = "折射位移 + 边缘色散 + 流动高光，玻璃会「呼吸」",
        blur = 14f,
        tintAlpha = 0.04f,
        sheen = 1.0f,
        refract = true,
    ),
    Solid(
        label = "纯色",
        labelEn = "Solid",
        description = "零模糊的实心层，最省电、最锐利，适合户外强光",
        blur = 0f,
        tintAlpha = 1f,
        sheen = 0.12f,
        refract = false,
    ),
}

/**
 * 玻璃质量档位。它不改变材质选择，只决定「为这块玻璃花多少 GPU」：
 * 低档位把模糊半径砍半并关闭 AGSL 折射，观感接近柔光玻璃但每帧采样的
 * 面积和着色开销都降下来。由动效强度自动推导，也可以单独覆盖。
 */
enum class HuiGlassQuality { Low, Balanced, High }

/** 动画强度：影响全局动效的时长倍率与装饰性动画的开启。 */
enum class HuiMotionLevel(val label: String, val scale: Float, val decorative: Boolean) {
    Calm("克制", 0.65f, false),
    Flow("舒展", 1f, true),
    Wild("张扬", 1.45f, true),
}

@Immutable
data class HuiThemeState(
    val accent: HuiAccent,
    val dark: Boolean,
    val material: HuiMaterial,
    val motion: HuiMotionLevel,
    val colors: HuiColors,
) {
    /** 动效强度直接决定玻璃档位：克制档省电，张扬档给满。 */
    val glassQuality: HuiGlassQuality
        get() = when (motion) {
            HuiMotionLevel.Calm -> HuiGlassQuality.Low
            HuiMotionLevel.Flow -> HuiGlassQuality.Balanced
            HuiMotionLevel.Wild -> HuiGlassQuality.High
        }
}

val LocalHuiMaterial = compositionLocalOf { HuiMaterial.LiquidGlass }
val LocalHuiMotionLevel = compositionLocalOf { HuiMotionLevel.Flow }
val LocalHuiGlassQuality = staticCompositionLocalOf { HuiGlassQuality.Balanced }

@Composable
fun rememberHuiThemeState(
    accent: HuiAccent = HuiAccent.Aurora,
    dark: Boolean = isSystemInDarkTheme().not(),
    material: HuiMaterial = HuiMaterial.LiquidGlass,
    motion: HuiMotionLevel = HuiMotionLevel.Flow,
): HuiThemeState = HuiThemeState(
    accent = accent,
    dark = dark,
    material = material,
    motion = motion,
    colors = huiColors(accent, dark),
)

/**
 * Nova 主题。刻意不提供 `MaterialTheme` 兼容层——新的界面不该被 Material
 * 的 ColorScheme/Typography 语义绑住。
 */
@Composable
fun HuiNovaTheme(
    state: HuiThemeState,
    typography: HuiTypography = HuiTypography.Default,
    shapes: HuiShapes = HuiShapes(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHuiColors provides state.colors,
        LocalHuiTypography provides typography,
        LocalHuiShapes provides shapes,
        LocalHuiMaterial provides state.material,
        LocalHuiMotionLevel provides state.motion,
        LocalHuiGlassQuality provides state.glassQuality,
        content = content,
    )
}
