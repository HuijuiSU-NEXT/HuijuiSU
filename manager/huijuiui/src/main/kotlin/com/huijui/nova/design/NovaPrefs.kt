package com.huijui.nova.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 界面模式：简约整洁（无背景图）/ 背景动图（视频或图片壁纸）。 */
enum class NovaUiMode(val label: String, val desc: String) {
    Minimal("简约整洁", "纯色底，只留内容"),
    Wallpaper("背景动图", "视频或图片壁纸铺满，卡片浮在上面"),
}

/**
 * 玻璃材质。
 *
 * 参考澎湃 OS 4 的「柔光玻璃」：能感知周围颜色、按内容自动调节透明度；
 * 另一种「清透磨砂」更接近传统毛玻璃——更透、更硬、边界更清楚。
 */
enum class NovaGlass(val label: String, val desc: String) {
    SoftGlow("柔光玻璃", "掺入环境色，边缘柔和发光"),
    ClearFrost("清透磨砂", "低着色、边界清晰、更透"),
}

/** 深浅色：跟随系统，或强制白 / 黑。 */
enum class NovaThemeMode(val label: String) {
    System("跟随系统"),
    Light("白色"),
    Dark("黑色"),
}

val LocalNovaUiMode = staticCompositionLocalOf { NovaUiMode.Wallpaper }
val LocalNovaGlass = staticCompositionLocalOf { NovaGlass.SoftGlow }
/** 当前是否深色（已把「跟随系统」解析过）。 */
val LocalNovaDark = staticCompositionLocalOf { false }

/**
 * 页面文字色 / 底色：直接取主题色。
 *
 * 主题色（LocalHuiColors）由 HuiNovaTheme 分发到整棵树，比另维护一个
 * LocalNovaDark 更可靠——之前正是因为后者在某些 subcomposition 里读不到，
 * 出现「背景变暗、字还是黑的」。
 */
@Composable
fun novaInk(): Color = LocalHuiColors.current.text

@Composable
fun novaDim(): Color = LocalHuiColors.current.textDim

@Composable
fun novaBg(): Color = LocalHuiColors.current.bg
