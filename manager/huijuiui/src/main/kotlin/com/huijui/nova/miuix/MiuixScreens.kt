package com.huijui.nova.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.components.HuiGlyph
import com.huijui.nova.components.HuiIcon
import com.huijui.nova.components.HuiIcons
import com.huijui.nova.design.HuiAccent
import com.huijui.nova.design.huiColors
import com.huijui.nova.design.HuiMaterial
import com.huijui.nova.design.HuiMotionLevel
import com.huijui.nova.design.HuiThemeState
import com.huijui.nova.design.novaInk
import com.huijui.nova.design.novaBg
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.LocalNovaUiMode
import com.huijui.nova.design.NovaGlass
import com.huijui.nova.design.NovaThemeMode
import com.huijui.nova.design.NovaUiMode
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.wallpaper.HuiMediaSpec
import com.huijui.nova.wallpaper.HuiWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.huijui.nova.ui.Text
import com.huijui.nova.ui.MiuixTheme
import com.huijui.nova.ui.lightColorScheme

/* ------------------------------------------------------------------ */
/*  浅色平面版式                                                        */
/* ------------------------------------------------------------------ */


@Composable
private fun MiuixPage(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pageTitle = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) Color.White else novaInk()
    MiuixTheme(colors = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Text(title, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = pageTitle)
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) {
                        Color.White.copy(alpha = 0.75f)
                    } else {
                        novaDim()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = if (LocalNovaUiMode.current == NovaUiMode.Wallpaper) {
            Color.White.copy(alpha = 0.85f)
        } else {
            novaDim()
        },
        modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
    )
}

@Composable
private fun LightCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier.fillMaxWidth()
    GlassCard(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        corner = 20.dp,
        padding = PaddingValues(16.dp),
    ) {
        content()
    }
}

@Composable
private fun RowItem(
    icon: HuiGlyph,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val ink = novaInk()
    val dim = novaDim()
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background((accent ?: dim).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            HuiIcon(icon, size = 16.dp, tint = accent ?: dim)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink)
            if (subtitle != null) Text(subtitle, fontSize = 10.sp, color = dim)
        }
        if (trailing != null) Text(trailing, fontSize = 11.sp, color = accent ?: dim)
    }
}

/* ------------------------------------------------------------------ */
/*  工具页                                                             */
/* ------------------------------------------------------------------ */

@Composable
fun MiuixToolsScreen(
    onSubPageChange: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onOpenLegacy: () -> Unit = {},
    onOpenPatches: () -> Unit = {},
    onOpenSusfs: () -> Unit = {},
) {
    val ink = novaInk()

    var showHide by remember { mutableStateOf(false) }
    var showModules by remember { mutableStateOf(false) }
    var showDetect by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    var confirmReboot by remember { mutableStateOf<String?>(null) }
    val accent = LocalHuiColors.current.accent

    // 子页面是全屏的，打开时把 Dock 收起来，避免底部露出壁纸
    androidx.compose.runtime.LaunchedEffect(showHide, showModules, showDetect, showFlash, showTerminal) {
        onSubPageChange(showHide || showModules || showDetect || showFlash || showTerminal)
    }

    Box(Modifier.fillMaxSize()) {
        MiuixPage(title = "工具", subtitle = "刷写 · 隐藏 · 管理") {
            SectionLabel(S("刷写"))
            LightCard(onClick = { showFlash = true }) {
                RowItem(
                    HuiIcons.Download,
                    S("刷写中心"),
                    S("安装模块 · 修补镜像 · 刷入分区"),
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel(S("隐藏套件"))
            LightCard(onClick = { showHide = true }) {
                RowItem(
                    HuiIcons.Shield,
                    S("一键隐藏（hide_bundle）"),
                    S("Zygisk Next · LSPosed · HMA-OSS · OhMyKeymint"),
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel(S("内核"))
            LightCard(onClick = onOpenSusfs) {
                RowItem(
                    HuiIcons.Gauge,
                    S("SUSFS"),
                    S("隐藏路径 · 日志 · AVC 伪造 · 配置备份"),
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(10.dp))
            LightCard(onClick = { onOpenLegacy() }) {
                RowItem(
                    HuiIcons.Cpu,
                    S("4.x 兼容模式"),
                    S("非 GKI：内核补丁 / 修补 boot"),
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel(S("检查与修复"))
            LightCard(onClick = onOpenPatches) {
                RowItem(
                    HuiIcons.Zap,
                    S("补丁中心"),
                    S("一键隐藏属性 · 重置可调试标记 · 伪装 BL"),
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel(S("检测"))
            LightCard(onClick = { showDetect = true }) {
                RowItem(
                    HuiIcons.Gauge,
                    S("春秋检测"),
                    S("9 组特征 · 属性 / 挂载 / maps / 进程 / SELinux"),
                    "运行",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel("管理")
            LightCard {
                RowItem(HuiIcons.Module, S("模块管理"), S("已装模块 · 启用 / 停用"), "进入", accent, onClick = { showModules = true })
                RowItem(HuiIcons.Terminal, S("终端"), S("临时 Root Shell · 逐条 su 执行"), "进入", accent) {
                    showTerminal = true
                }
                RowItem(HuiIcons.Refresh, S("重启设备"), S("重启 / Recovery / 关机"), "选择", accent) {
                    confirmReboot = "menu"
                }
            }
        }

        if (showHide) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(novaBg()),
            ) {
                MiuixHideBundleScreen(onBack = { showHide = false })
            }
        }
        if (showFlash) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(novaBg(), novaBg(), novaBg().copy(alpha = 0.97f)),
                        ),
                    ),
            ) {
                MiuixFlashScreen(onBack = { showFlash = false })
            }
        }
        if (showTerminal) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(novaBg()),
            ) {
                MiuixTerminalScreen(onBack = { showTerminal = false })
            }
        }
        if (showDetect) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(novaBg(), novaBg(), novaBg().copy(alpha = 0.97f)),
                        ),
                    ),
            ) {
                MiuixDetectScreen(onBack = { showDetect = false })
            }
        }
        if (showModules) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(novaBg(), novaBg(), novaBg().copy(alpha = 0.97f)),
                        ),
                    ),
            ) {
                MiuixModulesScreen(onBack = { showModules = false })
            }
        }
    }

    if (confirmReboot != null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            GlassCard(
                Modifier.padding(26.dp),
                corner = 20.dp,
                padding = PaddingValues(18.dp),
                alpha = 0.96f,
            ) {
                Text(S("重启设备"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "重启" to "reboot",
                    "重启到 Recovery" to "reboot recovery",
                    "重启到 Bootloader" to "reboot bootloader",
                    "关机" to "reboot -p",
                ).forEach { (label, cmd) ->
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (label == "关机") Color(0xFFD9534F) else ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                confirmReboot = null
                                MainScope().launch {
                                    com.huijui.nova.hide.HideBundle.run(cmd)
                                }
                            }
                            .padding(vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    S("取消"),
                    fontSize = 12.sp,
                    color = novaDim(),
                    modifier = Modifier
                        .clickable { confirmReboot = null }
                        .padding(vertical = 6.dp),
                )
            }
        }
    }

}

/* ------------------------------------------------------------------ */
/*  实验页                                                             */
/* ------------------------------------------------------------------ */

@Composable
fun MiuixLabScreen(
    theme: HuiThemeState,
    onMaterialChange: (HuiMaterial) -> Unit,
    onMotionChange: (HuiMotionLevel) -> Unit,
) {
    val ink = novaInk()

    val accent = LocalHuiColors.current.accent
    MiuixPage(title = "实验", subtitle = "材质 · 动效") {
        SectionLabel(S("材质"))
        LightCard {
            HuiMaterial.entries.forEach { material ->
                val active = theme.material == material
                RowItem(
                    icon = when (material) {
                        HuiMaterial.GlossyGlass -> HuiIcons.Star
                        HuiMaterial.SoftGlass -> HuiIcons.Layers
                        HuiMaterial.LiquidGlass -> HuiIcons.Sparkle
                        HuiMaterial.Solid -> HuiIcons.Cpu
                    },
                    title = material.label,
                    subtitle = material.description,
                    trailing = if (active) "使用中" else null,
                    accent = if (active) accent else null,
                    onClick = { onMaterialChange(material) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel("动效强度")
        LightCard {
            HuiMotionLevel.entries.forEach { level ->
                RowItem(
                    HuiIcons.Sparkle,
                    level.label,
                    "${"%.0f".format(level.scale * 100)}% 动效幅度",
                    trailing = if (theme.motion == level) "使用中" else null,
                    accent = if (theme.motion == level) accent else null,
                    onClick = { onMotionChange(level) },
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  设置页                                                             */
/* ------------------------------------------------------------------ */

@Composable
fun MiuixSettingsScreen(
    theme: HuiThemeState,
    uiMode: NovaUiMode = NovaUiMode.Wallpaper,
    onUiModeChange: (NovaUiMode) -> Unit = {},
    glass: NovaGlass = NovaGlass.SoftGlow,
    onGlassChange: (NovaGlass) -> Unit = {},
    themeMode: NovaThemeMode = NovaThemeMode.System,
    onThemeModeChange: (NovaThemeMode) -> Unit = {},
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
    onOpenLicenses: () -> Unit = {},
    onOpenKernelSettings: () -> Unit = {},
) {
    val accent = LocalHuiColors.current.accent
    val ink = novaInk()
    val context = androidx.compose.ui.platform.LocalContext.current

    MiuixPage(title = "设置", subtitle = "外观 · 壁纸 · 关于") {
        SectionLabel(S("界面"))
        LightCard {
            NovaUiMode.entries.forEach { mode ->
                RowItem(
                    if (mode == NovaUiMode.Minimal) HuiIcons.Grid else HuiIcons.Play,
                    mode.label,
                    mode.desc,
                    trailing = if (uiMode == mode) "使用中" else null,
                    accent = if (uiMode == mode) accent else null,
                    onClick = { onUiModeChange(mode) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("玻璃"))
        LightCard {
            NovaGlass.entries.forEach { g ->
                RowItem(
                    if (g == NovaGlass.SoftGlow) HuiIcons.Sparkle else HuiIcons.Layers,
                    g.label,
                    g.desc,
                    trailing = if (glass == g) "使用中" else null,
                    accent = if (glass == g) accent else null,
                    onClick = { onGlassChange(g) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("深浅色"))
        LightCard {
            NovaThemeMode.entries.forEach { m ->
                RowItem(
                    when (m) {
                        NovaThemeMode.System -> HuiIcons.Settings
                        NovaThemeMode.Light -> HuiIcons.Sun
                        NovaThemeMode.Dark -> HuiIcons.Moon
                    },
                    m.label,
                    when (m) {
                        NovaThemeMode.System -> "跟随系统设置，默认白色"
                        NovaThemeMode.Light -> "始终白色"
                        NovaThemeMode.Dark -> "始终黑色"
                    },
                    trailing = if (themeMode == m) "使用中" else null,
                    accent = if (themeMode == m) accent else null,
                    onClick = { onThemeModeChange(m) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("强调色"))
        LightCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HuiAccent.entries.forEach { item ->
                    val color = huiColors(accent = item, dark = theme.dark).accent
                    val selected = theme.accent == item
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                            .clickable { onAccentChange(item) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("内核"))
        LightCard(onClick = onOpenKernelSettings) {
            RowItem(
                HuiIcons.Cpu,
                S("内核设置"),
                S("su 兼容 · umount · sulog · adb root · SELinux 隐藏"),
                "进入",
                accent,
            )
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("语言"))
        LightCard {
            RowItem(
                HuiIcons.Info,
                S("界面语言"),
                if (com.huijui.nova.design.I18n.lang == com.huijui.nova.design.Lang.En) "English" else "中文",
                "切换",
                accent,
            ) {
                com.huijui.nova.design.I18n.toggle(context)
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("壁纸"))
        LightCard {
            RowItem(
                HuiIcons.Palette,
                if (media != null) "当前：媒体壁纸" else "当前：${wallpaper.label}",
                media?.let { if (it.kind.name == "Video") "视频" else "图片" } ?: "程序化",
                onClick = onPickMedia,
            )
            if (media != null) {
                RowItem(HuiIcons.Close, S("移除媒体壁纸"), onClick = onClearMedia)
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel(S("关于"))
        LightCard {
            val pkgInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val versionName = pkgInfo?.versionName ?: "?"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                pkgInfo?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION") (pkgInfo?.versionCode ?: 0).toLong()
            }
            var updateState by remember { mutableStateOf("") }
            RowItem(
                HuiIcons.Info,
                S("HuiJuiSU Nova"),
                S("$versionName ($versionCode) · Compose 独立界面"),
                "当前",
                accent,
            ) {}
            RowItem(
                HuiIcons.Download,
                S("检查更新"),
                if (updateState.isBlank()) S("从 GitHub Releases 获取最新版本") else updateState,
                S("检查"),
                accent,
            ) {
                if (updateState.isBlank()) {
                    updateState = S("检查中…")
                    kotlinx.coroutines.MainScope().launch {
                        withContext(Dispatchers.IO) {
                            val cache = java.io.File(context.cacheDir, "release.json")
                            val rel = com.huijui.nova.update.UpdateChecker.latest(cache)
                            updateState = when {
                                rel == null -> "网络不可用"
                                com.huijui.nova.update.UpdateChecker.compare(versionName, rel.tag) < 0 ->
                                    "有新版本 ${rel.tag}" + if (rel.fromCache) "（缓存）" else ""
                                else -> "已是最新（${rel.tag}）"
                            }
                        }
                    }
                }
            }
            RowItem(
                HuiIcons.Rocket,
                S("GitHub 源码"),
                S("github.com/HuijuiSU/HuiJuiSU"),
                "打开",
                accent,
            ) { openUrl(context, "https://github.com/HuijuiSU/HuiJuiSU") }
            RowItem(
                HuiIcons.Shield,
                S("开源许可"),
                S("GPL-3.0 · 组件清单"),
                "查看",
                accent,
            ) { onOpenLicenses() }
            RowItem(
                HuiIcons.SoundOn,
                S("Telegram 频道"),
                S("t.me/Huijui123"),
                "加入",
                accent,
            ) { openUrl(context, "https://t.me/Huijui123") }
            RowItem(
                HuiIcons.Bell,
                S("QQ 群"),
                S("763278354"),
                "复制",
                accent,
            ) { copyText(context, "763278354") }
            if (onClose != null) {
                RowItem(HuiIcons.Close, S("关闭 Nova"), S("回到经典界面")) { onClose() }
            }
        }
    }
}

/** 打开外部链接（浏览器 / TG）。 */
private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** 复制到剪贴板（QQ 群号这种没法直接跳的）。 */
private fun copyText(context: android.content.Context, text: String) {
    runCatching {
        val manager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        manager.setPrimaryClip(android.content.ClipData.newPlainText("nova", text))
    }
}
