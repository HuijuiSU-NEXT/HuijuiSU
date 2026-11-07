package com.huijui.nova

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.components.HuiGlyph
import com.huijui.nova.components.HuiIcon
import com.huijui.nova.components.HuiIcons
import com.huijui.nova.components.HuiText
import com.huijui.nova.design.HuiAccent
import com.huijui.nova.design.HuiMaterial
import com.huijui.nova.design.HuiMotionLevel
import com.huijui.nova.design.HuiThemeState
import com.huijui.nova.design.NovaUiMode
import com.huijui.nova.design.NovaGlass
import com.huijui.nova.design.NovaThemeMode
import com.huijui.nova.design.LocalNovaUiMode
import com.huijui.nova.design.LocalNovaGlass
import com.huijui.nova.design.LocalNovaDark
import com.huijui.nova.design.novaBg
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.design.LocalHuiShapes
import com.huijui.nova.design.LocalHuiTypography
import com.huijui.nova.material.HuiBackdropHost
import com.huijui.nova.motion.HuiEase
import com.huijui.nova.motion.huiPress
import com.huijui.nova.wallpaper.HuiMediaSpec
import com.huijui.nova.wallpaper.HuiWallpaper

enum class NovaTab(private val zh: String, val icon: HuiGlyph) {
    Home("首页", HuiIcons.Home),
    Modules("模块", HuiIcons.Module),
    Superuser("超级用户", HuiIcons.Shield),
    Settings("设置", HuiIcons.Settings);

    val label: String get() = com.huijui.nova.design.S(zh)
}

/** 从首页卡片打开的全屏子页面。 */
enum class NovaOverlay(val title: String) {
    Tools("全部工具"),
    Terminal("终端"),
    Flash("刷写中心"),
    Detect("春秋检测"),
    Hide("一键隐藏"),
    Legacy4x("4.x 兼容模式"),
    Licenses("开源许可"),
    KernelSettings("内核设置"),
    Checkup("一键体检"),
    Heal("自愈中心"),
    Patches("补丁中心"),
    Susfs("SUSFS"),
    Repo("在线模块"),
    Lab("实验"),
}

/**
 * 独立界面入口。整棵树只依赖 Compose Foundation —— 没有 MaterialTheme、
 * 没有 NavigationBar、没有 Card/Switch/Button。表面全部由 Miuix + GlassCard 提供。
 */
@Composable
fun NovaApp(
    theme: HuiThemeState,
    onAccentChange: (HuiAccent) -> Unit,
    onMaterialChange: (HuiMaterial) -> Unit,
    onMotionChange: (HuiMotionLevel) -> Unit,
    onDarkChange: (Boolean) -> Unit = {},
    wallpaper: HuiWallpaper = HuiWallpaper.Default,
    onWallpaperChange: (HuiWallpaper) -> Unit = {},
    media: HuiMediaSpec? = null,
    onPickMedia: () -> Unit = {},
    onClearMedia: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    uiMode: NovaUiMode = NovaUiMode.Wallpaper,
    onUiModeChange: (NovaUiMode) -> Unit = {},
    glass: NovaGlass = NovaGlass.SoftGlow,
    onGlassChange: (NovaGlass) -> Unit = {},
    themeMode: NovaThemeMode = NovaThemeMode.System,
    onThemeModeChange: (NovaThemeMode) -> Unit = {},
) {
    val i18nCtx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { com.huijui.nova.design.I18n.load(i18nCtx) }
    var tab by remember { mutableIntStateOf(0) }
    var subPageOpen by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<NovaOverlay?>(null) }
    val overlayStack = remember { androidx.compose.runtime.mutableStateListOf<NovaOverlay>() }
    // 打开子页：记住来路，返回时逐级回退（工具 → 补丁 → 工具 → 首页）
    val go: (NovaOverlay) -> Unit = { next ->
        overlay?.let { if (it != next) overlayStack.add(it) }
        overlay = next
    }
    val back: () -> Unit = {
        overlay = if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex) else null
    }
    // 切标签时必须复位：否则从子页面切走，Dock 会一直藏着
    androidx.compose.runtime.LaunchedEffect(tab) { subPageOpen = false }
    val colors = LocalHuiColors.current

    // 返回键：任何子页先回首页，而不是直接退出 Nova
    BackHandler(enabled = tab != NovaTab.Home.ordinal) { tab = NovaTab.Home.ordinal }

    val dark = when (themeMode) {
        NovaThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
        NovaThemeMode.Light -> false
        NovaThemeMode.Dark -> true
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalNovaUiMode provides uiMode,
        LocalNovaGlass provides glass,
        LocalNovaDark provides dark,
    ) {
    HuiBackdropHost(
        wallpaper = wallpaper,
        media = if (uiMode == NovaUiMode.Minimal) null else media,
        // 深色模式下壁纸必须压暗：否则深玻璃 + 深字 + 亮壁纸三个互相打架
        // 壁纸本身不变暗（用户要求：只让卡片变暗），遮罩置 0
        scrim = 0f,
        background = if (uiMode == NovaUiMode.Minimal) {
            { Box(Modifier.fillMaxSize().background(novaBg())) }
        } else {
            null
        },
    ) {
        // 再包一层：AnimatedContent 用的是 subcomposition，个别版本下
        // 外层 provider 读不到，这里补一次保证页面拿到正确的深浅色
        androidx.compose.runtime.CompositionLocalProvider(
            LocalNovaUiMode provides uiMode,
            LocalNovaGlass provides glass,
            LocalNovaDark provides dark,
        ) {
        if (overlay == null) {
        Column(
            Modifier
                .fillMaxSize()
                // 子页面打开时把状态栏/导航栏区域也铺成页面底色，避免底部露出壁纸
                .background(if (subPageOpen) novaBg() else Color.Transparent)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        // 页面切换：新页轻微放大淡入，旧页快速淡出，缩短两层同时占 GPU 的时间
                        (
                            fadeIn(tween(280, easing = HuiEase.Emphasized.curve)) +
                                scaleIn(
                                    initialScale = 0.97f,
                                    animationSpec = tween(320, easing = HuiEase.Emphasized.curve),
                                )
                            ).togetherWith(
                            fadeOut(tween(160, easing = HuiEase.Accelerate.curve)),
                        )
                    },
                    label = "tab",
                ) { index ->
                    when (NovaTab.entries[index]) {
                        NovaTab.Modules -> com.huijui.nova.miuix.MiuixModulesScreen(
                            onOpenRepo = { go(NovaOverlay.Repo) },
                            embedded = true,
                            onBack = {},
                        )
                        NovaTab.Superuser -> com.huijui.nova.miuix.MiuixSuperuserScreen()
                        NovaTab.Home -> com.huijui.nova.miuix.NovaHomeMiuixScreen(
                            onOpen = { go(it) },
                        )
                        NovaTab.Settings -> com.huijui.nova.miuix.MiuixSettingsScreen(
                            theme = theme,
                            uiMode = uiMode,
                            onUiModeChange = onUiModeChange,
                            glass = glass,
                            onGlassChange = onGlassChange,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onAccentChange = onAccentChange,
                            onMaterialChange = onMaterialChange,
                            onMotionChange = onMotionChange,
                            onDarkChange = onDarkChange,
                            wallpaper = wallpaper,
                            onWallpaperChange = onWallpaperChange,
                            media = media,
                            onPickMedia = onPickMedia,
                            onClearMedia = onClearMedia,
                            onClose = onClose,
                            onOpenLicenses = { go(NovaOverlay.Licenses) },
                            onOpenKernelSettings = { go(NovaOverlay.KernelSettings) },
                        )
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !subPageOpen && overlay == null,
                enter = androidx.compose.animation.fadeIn() +
                    androidx.compose.animation.slideInVertically { it / 3 },
                exit = androidx.compose.animation.fadeOut() +
                    androidx.compose.animation.slideOutVertically { it / 3 },
            ) {
            NovaDock(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        }

        // 全屏子页面：铺在壁纸之上（半透明浅色遮罩），主页面此时不渲染
        // 系统返回键 / 手势：先关子页面，再回首页标签，最后才退出
        androidx.activity.compose.BackHandler(enabled = overlay != null) { back() }
        androidx.activity.compose.BackHandler(enabled = overlay == null && tab != 0) {
            tab = 0
        }

        overlay?.let { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                novaBg().copy(alpha = if (dark) 0.78f else 0.62f),
                                novaBg().copy(alpha = if (dark) 0.86f else 0.74f),
                            ),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                when (page) {
                    NovaOverlay.Tools -> com.huijui.nova.miuix.MiuixToolsScreen(
                        onBack = { back() },
                        onOpenLegacy = { go(NovaOverlay.Legacy4x) },
                        onOpenPatches = { go(NovaOverlay.Patches) },
                        onOpenHeal = { go(NovaOverlay.Heal) },
                        onOpenSusfs = { go(NovaOverlay.Susfs) },
                    )
                    NovaOverlay.Terminal -> com.huijui.nova.miuix.MiuixTerminalScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Flash -> com.huijui.nova.miuix.MiuixFlashScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Detect -> com.huijui.nova.miuix.MiuixDetectScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Hide -> com.huijui.nova.miuix.MiuixHideBundleScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Legacy4x -> com.huijui.nova.miuix.MiuixLegacy4xScreen(
                        onBack = { back() },
                        onOpenPatch = { go(NovaOverlay.Flash) },
                    )
                    NovaOverlay.Licenses -> com.huijui.nova.miuix.MiuixLicensesScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.KernelSettings -> com.huijui.nova.miuix.MiuixKernelSettingsScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Repo -> com.huijui.nova.miuix.MiuixRepoScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Susfs -> com.huijui.nova.miuix.MiuixSusfsScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Patches -> com.huijui.nova.miuix.MiuixPatchScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Heal -> com.huijui.nova.miuix.MiuixHealScreen(
                        onBack = { back() },
                    )
                    NovaOverlay.Checkup -> com.huijui.nova.miuix.MiuixCheckupScreen(
                        onBack = { back() },
                        onOpen = { key ->
                            val target = when (key) {
                                "legacy" -> NovaOverlay.Legacy4x
                                "detect" -> NovaOverlay.Detect
                                "flash" -> NovaOverlay.Flash
                                "patches" -> NovaOverlay.Patches
                                "heal" -> NovaOverlay.Heal
                                "modules" -> NovaOverlay.Tools
                                else -> null
                            }
                            if (target != null) go(target)
                        },
                    )
                    NovaOverlay.Lab -> com.huijui.nova.miuix.MiuixLabScreen(
                        theme = theme,
                        onMaterialChange = onMaterialChange,
                        onMotionChange = onMotionChange,
                    )
                }
            }
        }
        }
    }
    }
}

/**
 * 悬浮玻璃 Dock。
 *
 * 相比上一版的三处优化：
 * 1. **只有一条指示器**：位置由一个 Float 驱动，在 `drawBehind` 里按行宽算出矩形，
 *    不再给每个 item 各挂一个 `animateFloatAsState`，切换时不会 4 个 item 同时重组；
 * 2. **标签常驻**：不再用 `weight > 0.35` 决定显不显示，避免文字「弹进弹出」，
 *    只用颜色和图标缩放表达选中；
 * 3. **点击热区 ≥ 48dp**、带 `selectableGroup` 与 `selected` 语义，无障碍和单手点按都更稳。
 */
@Composable
fun NovaDock(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    val shapes = LocalHuiShapes.current
    val haptic = LocalHapticFeedback.current

    var rowWidth by remember { mutableStateOf(0) }
    val tabCount = NovaTab.entries.size
    val position by animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 620f,
            visibilityThreshold = 0.001f,
        ),
        label = "dockPosition",
    )

    // 玻璃 Dock：和卡片同一套材质语言（深色玻璃 + 顶部高光 + 底部压暗），
    // 不再用 shadow —— 它在部分 GPU 上会渲染成一块实心塑料。
    val wallpaper = LocalNovaUiMode.current == NovaUiMode.Wallpaper
    val dockTop = if (wallpaper) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.72f)
    val dockMid = if (wallpaper) Color(0xFF0B1020).copy(alpha = 0.26f) else Color.White.copy(alpha = 0.55f)
    val dockBot = if (wallpaper) Color(0xFF0B1020).copy(alpha = 0.34f) else Color.White.copy(alpha = 0.45f)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shapes.pill)
            .background(Brush.verticalGradient(listOf(dockTop, dockMid, dockBot)))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (wallpaper) 0.42f else 0.9f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = if (wallpaper) 0.18f else 0.06f),
                    ),
                ),
                shapes.pill,
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { rowWidth = it.width }
                .drawBehind {
                    // 指示器只在绘制阶段运行：读 position，不触发重组
                    if (rowWidth <= 0 || tabCount == 0) return@drawBehind
                    val itemWidth = rowWidth.toFloat() / tabCount
                    val inset = 5.dp.toPx()
                    val left = position * itemWidth + inset
                    val top = inset
                    val width = itemWidth - inset * 2
                    val height = size.height - inset * 2
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = 0.30f),
                                colors.accentAlt.copy(alpha = 0.16f),
                            ),
                        ),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(height / 2f),
                    )
                    drawRoundRect(
                        color = colors.accent.copy(alpha = 0.35f),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(height / 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    )
                },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaTab.entries.forEachIndexed { index, item ->
                    DockItem(
                        item = item,
                        active = index == selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (index != selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    item: NovaTab,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    val interaction = remember { MutableInteractionSource() }
    val dockOnDark = LocalNovaUiMode.current == NovaUiMode.Wallpaper
    val iconTint by animateColorAsState(
        targetValue = when {
            active && dockOnDark -> Color.White
            active -> Color(0xFF101820)
            dockOnDark -> Color.White.copy(alpha = 0.62f)
            else -> Color(0xFF5B6875)
        },
        animationSpec = tween(220, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "dockTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "dockScale",
    )

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .huiPress(interaction, pressedScale = 0.94f)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = item.label,
            ) { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = item.label
                selected = active
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
        ) {
            HuiIcon(item.icon, size = 19.dp, tint = iconTint)
        }
        Spacer(Modifier.width(6.dp))
        HuiText(
            item.label,
            type.micro.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
            color = iconTint,
            maxLines = 1,
        )
    }
}

/** 顶部品牌条：文字 wordmark + 圆圈入口，替代 TopAppBar。 */
@Composable
fun NovaTopBar(
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalHuiColors.current
    val type = LocalHuiTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                HuiText(
                    "HuiJui",
                    type.headline.copy(fontWeight = FontWeight.Black),
                    color = colors.text,
                )
                HuiText(
                    "SU",
                    type.headline.copy(fontWeight = FontWeight.Black),
                    color = colors.accent,
                )
                Spacer(Modifier.width(6.dp))
                HuiText(
                    "NOVA",
                    type.micro,
                    color = colors.textFaint,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            HuiText(subtitle, type.micro, color = colors.textFaint, maxLines = 1)
        }
        NovaClock()
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** 圆形玻璃按钮，顶栏右侧那类。 */
@Composable
fun HuiRoundButton(
    icon: HuiGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 38,
) {
    val colors = LocalHuiColors.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.text.copy(alpha = 0.06f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HuiIcon(icon, size = (size * 0.45f).dp, tint = colors.textDim)
    }
}

/** 顶栏时钟：大号时间 + 日期，每 20 秒对一次表。 */
@Composable
fun NovaClock(modifier: Modifier = Modifier) {
    val colors = LocalHuiColors.current
    var now by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            now = java.util.Calendar.getInstance()
            kotlinx.coroutines.delay(20_000)
        }
    }
    val hh = "%02d".format(now.get(java.util.Calendar.HOUR_OF_DAY))
    val mm = "%02d".format(now.get(java.util.Calendar.MINUTE))
    val week = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[
        now.get(java.util.Calendar.DAY_OF_WEEK) - 1,
    ]
    val md = "%d月%d日".format(
        now.get(java.util.Calendar.MONTH) + 1,
        now.get(java.util.Calendar.DAY_OF_MONTH),
    )
    Column(modifier, horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HuiText(
                hh,
                androidx.compose.ui.text.TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp,
                ),
                color = colors.text,
            )
            HuiText(
                ":",
                androidx.compose.ui.text.TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Light,
                ),
                color = colors.accent,
                modifier = Modifier.padding(horizontal = 1.dp),
            )
            HuiText(
                mm,
                androidx.compose.ui.text.TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp,
                ),
                color = colors.text,
            )
        }
        HuiText("$md · $week", LocalHuiTypography.current.micro, color = colors.textFaint)
    }
}
