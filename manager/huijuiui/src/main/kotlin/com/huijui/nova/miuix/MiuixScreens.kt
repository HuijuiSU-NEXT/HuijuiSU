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
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.LocalNovaUiMode
import com.huijui.nova.design.NovaGlass
import com.huijui.nova.design.NovaThemeMode
import com.huijui.nova.design.NovaUiMode
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.wallpaper.HuiMediaSpec
import com.huijui.nova.wallpaper.HuiWallpaper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

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
            SectionLabel("刷写")
            LightCard(onClick = { showFlash = true }) {
                RowItem(
                    HuiIcons.Download,
                    "刷写中心",
                    "安装模块 · 修补镜像 · 刷入分区",
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel("隐藏套件")
            LightCard(onClick = { showHide = true }) {
                RowItem(
                    HuiIcons.Shield,
                    "一键隐藏（hide_bundle）",
                    "Zygisk Next · LSPosed · HMA-OSS · OhMyKeymint",
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel("内核")
            LightCard(onClick = { onOpenLegacy() }) {
                RowItem(
                    HuiIcons.Cpu,
                    "4.x 兼容模式",
                    "非 GKI：内核补丁 / 修补 boot",
                    "进入",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel("检测")
            LightCard(onClick = { showDetect = true }) {
                RowItem(
                    HuiIcons.Gauge,
                    "春秋检测",
                    "9 组特征 · 属性 / 挂载 / maps / 进程 / SELinux",
                    "运行",
                    accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel("管理")
            LightCard {
                RowItem(HuiIcons.Module, "模块管理", "已装模块 · 启用 / 停用", "进入", accent, onClick = { showModules = true })
                RowItem(HuiIcons.Terminal, "终端", "临时 Root Shell · 逐条 su 执行", "进入", accent) {
                    showTerminal = true
                }
                RowItem(HuiIcons.Refresh, "重启设备", "重启 / Recovery / 关机", "选择", accent) {
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
                Text("重启设备", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ink)
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
                    "取消",
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
        SectionLabel("材质")
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
) {
    val accent = LocalHuiColors.current.accent
    val ink = novaInk()
    val context = androidx.compose.ui.platform.LocalContext.current

    MiuixPage(title = "设置", subtitle = "外观 · 壁纸 · 关于") {
        SectionLabel("界面")
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

        SectionLabel("玻璃")
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

        SectionLabel("深浅色")
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

        SectionLabel("强调色")
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

        SectionLabel("壁纸")
        LightCard {
            RowItem(
                HuiIcons.Palette,
                if (media != null) "当前：媒体壁纸" else "当前：${wallpaper.label}",
                media?.let { if (it.kind.name == "Video") "视频" else "图片" } ?: "程序化",
                onClick = onPickMedia,
            )
            if (media != null) {
                RowItem(HuiIcons.Close, "移除媒体壁纸", onClick = onClearMedia)
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel("关于")
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
                "HuiJuiSU Nova",
                "$versionName ($versionCode) · Compose 独立界面",
                "当前",
                accent,
            ) {}
            RowItem(
                HuiIcons.Download,
                "检查更新",
                if (updateState.isBlank()) "从 GitHub Releases 获取最新版本" else updateState,
                "检查",
                accent,
            ) {
                if (updateState.isBlank()) {
                    updateState = "检查中…"
                    kotlinx.coroutines.MainScope().launch {
                        updateState = runCatching {
                            val conn = java.net.URL(
                                "https://api.github.com/repos/HuijuiSU-NEXT/HuijuiSU/releases/latest",
                            ).openConnection() as java.net.HttpURLConnection
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            conn.setRequestProperty("User-Agent", "HuiJuiSU")
                            val body = conn.inputStream.bufferedReader().readText()
                            val tag = Regex(""""tag_name"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                            when {
                                tag == null -> "获取失败"
                                tag == versionName -> "已是最新（$tag）"
                                else -> "有新版本 $tag"
                            }
                        }.getOrElse { "网络不可用" }
                    }
                }
            }
            RowItem(
                HuiIcons.Rocket,
                "GitHub 源码",
                "github.com/HuijuiSU/HuiJuiSU",
                "打开",
                accent,
            ) { openUrl(context, "https://github.com/HuijuiSU/HuiJuiSU") }
            RowItem(
                HuiIcons.Shield,
                "开源许可",
                "GPL-3.0 · 组件清单",
                "查看",
                accent,
            ) { onOpenLicenses() }
            RowItem(
                HuiIcons.SoundOn,
                "Telegram 频道",
                "t.me/Huijui123",
                "加入",
                accent,
            ) { openUrl(context, "https://t.me/Huijui123") }
            RowItem(
                HuiIcons.Bell,
                "QQ 群",
                "763278354",
                "复制",
                accent,
            ) { copyText(context, "763278354") }
            if (onClose != null) {
                RowItem(HuiIcons.Close, "关闭 Nova", "回到经典界面") { onClose() }
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
