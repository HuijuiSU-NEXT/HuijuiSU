package com.huijui.sunext.nova

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.huijui.nova.NovaApp
import com.huijui.nova.design.HuiAccent
import com.huijui.nova.design.HuiMaterial
import com.huijui.nova.design.HuiMotionLevel
import com.huijui.nova.design.HuiNovaTheme
import com.huijui.nova.design.rememberHuiThemeState
import com.huijui.nova.motion.HuiAnimationHost
import com.huijui.nova.wallpaper.HuiMediaKind
import com.huijui.nova.wallpaper.HuiMediaSpec
import com.huijui.nova.wallpaper.HuiWallpaper

/**
 * Nova 独立界面宿主。
 *
 * 这是一个和主界面完全并列的 Activity：不继承 BakaSU 的 BaseActivity，
 * 不用 MaterialTheme，不读管理器的主题状态，整棵树只由 com.huijui.nova 决定。
 *
 * 状态用 ordinal（Int）存，而不是直接存 enum：Bundle 往返对 Int 是零风险的。
 * 壁纸选择额外落到 SharedPreferences，因为系统级壁纸服务在另一个进程里要读它。
 */
class NovaActivity : ComponentActivity() {

    private companion object {
        /** 与 HuiColors.bgDeep 对齐，冷启动第一帧就是深色，不闪白。 */
        const val NOVA_BACKGROUND = 0xFF07070B.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(NOVA_BACKGROUND))
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            val context = LocalContext.current
            val stored = remember { NovaWallpaperStore.load(context) }
            val ui = remember { NovaSettingsStore.load(context) }

            var accentIndex by rememberSaveable { mutableIntStateOf(ui.accent.ordinal) }
            var materialIndex by rememberSaveable { mutableIntStateOf(ui.material.ordinal) }
            var motionIndex by rememberSaveable { mutableIntStateOf(ui.motion.ordinal) }
            // 深浅色以 themeMode 为准：否则重启后会出现「选了黑色但 dark=false」的错位
            val systemNight = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            var dark by rememberSaveable {
                mutableStateOf(
                    when (ui.themeMode) {
                        com.huijui.nova.design.NovaThemeMode.Dark -> true
                        com.huijui.nova.design.NovaThemeMode.Light -> false
                        com.huijui.nova.design.NovaThemeMode.System -> systemNight
                    },
                )
            }
            var uiModeIndex by rememberSaveable { mutableIntStateOf(ui.uiMode.ordinal) }
            var glassIndex by rememberSaveable { mutableIntStateOf(ui.glass.ordinal) }
            var themeModeIndex by rememberSaveable { mutableIntStateOf(ui.themeMode.ordinal) }
            var wallpaperIndex by rememberSaveable { mutableIntStateOf(stored.preset.ordinal) }
            // 首次进入：默认就是初音配色的动态壁纸
            var media by remember { mutableStateOf(stored.media) }

            // 首次进入：把内置的动态壁纸解到本地并设为默认（IO 线程，不卡首帧）
            LaunchedEffect(Unit) {
                if (!NovaWallpaperStore.isInitialized(context)) {
                    val builtin = withContext(Dispatchers.IO) { NovaBuiltinWallpaper.ensureExtracted(context) }
                    if (builtin != null) {
                        media = builtin
                        NovaWallpaperStore.saveMedia(context, builtin.uri, builtin.kind)
                    }
                    NovaWallpaperStore.markInitialized(context)
                }
            }

            val accent = HuiAccent.entries[accentIndex.coerceIn(0, HuiAccent.entries.lastIndex)]
            val material = HuiMaterial.entries[materialIndex.coerceIn(0, HuiMaterial.entries.lastIndex)]
            val motion = HuiMotionLevel.entries[motionIndex.coerceIn(0, HuiMotionLevel.entries.lastIndex)]
            val wallpaper = HuiWallpaper.entries[wallpaperIndex.coerceIn(0, HuiWallpaper.entries.lastIndex)]

            val picker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri: Uri? ->
                if (uri != null) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    val kind = HuiMediaKind.fromMime(context.contentResolver.getType(uri))
                    val spec = HuiMediaSpec(uri = uri.toString(), kind = kind)
                    media = spec
                    NovaWallpaperStore.saveMedia(context, spec.uri, spec.kind)
                }
            }

            val state = rememberHuiThemeState(
                accent = accent,
                dark = dark,
                material = material,
                motion = motion,
            )

            HuiNovaTheme(state) {
                HuiAnimationHost {
                    NovaApp(
                        theme = state,
                        onAccentChange = {
                            accentIndex = it.ordinal
                            NovaSettingsStore.save(context, NovaSettingsStore.Ui(it, material, motion, dark))
                        },
                        onMaterialChange = {
                            materialIndex = it.ordinal
                            NovaSettingsStore.save(context, NovaSettingsStore.Ui(accent, it, motion, dark))
                        },
                        onMotionChange = {
                            motionIndex = it.ordinal
                            NovaSettingsStore.save(context, NovaSettingsStore.Ui(accent, material, it, dark))
                        },
                        uiMode = com.huijui.nova.design.NovaUiMode.entries[uiModeIndex],
                        onUiModeChange = {
                            uiModeIndex = it.ordinal
                            NovaSettingsStore.save(
                                context,
                                NovaSettingsStore.Ui(
                                    accent, material, motion, dark, uiMode = it,
                                    glass = com.huijui.nova.design.NovaGlass.entries[glassIndex],
                                    themeMode = com.huijui.nova.design.NovaThemeMode.entries[themeModeIndex],
                                ),
                            )
                        },
                        glass = com.huijui.nova.design.NovaGlass.entries[glassIndex],
                        onGlassChange = {
                            glassIndex = it.ordinal
                            NovaSettingsStore.save(
                                context,
                                NovaSettingsStore.Ui(
                                    accent, material, motion, dark,
                                    uiMode = com.huijui.nova.design.NovaUiMode.entries[uiModeIndex],
                                    glass = it,
                                    themeMode = com.huijui.nova.design.NovaThemeMode.entries[themeModeIndex],
                                ),
                            )
                        },
                        themeMode = com.huijui.nova.design.NovaThemeMode.entries[themeModeIndex],
                        onThemeModeChange = {
                            themeModeIndex = it.ordinal
                            dark = when (it) {
                                com.huijui.nova.design.NovaThemeMode.Dark -> true
                                com.huijui.nova.design.NovaThemeMode.Light -> false
                                com.huijui.nova.design.NovaThemeMode.System ->
                                    (context.resources.configuration.uiMode and
                                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                            }
                            NovaSettingsStore.save(
                                context,
                                NovaSettingsStore.Ui(
                                    accent, material, motion, dark,
                                    uiMode = com.huijui.nova.design.NovaUiMode.entries[uiModeIndex],
                                    glass = com.huijui.nova.design.NovaGlass.entries[glassIndex],
                                    themeMode = it,
                                ),
                            )
                        },
                        onDarkChange = {
                            dark = it
                            NovaSettingsStore.save(context, NovaSettingsStore.Ui(accent, material, motion, it))
                        },
                        wallpaper = wallpaper,
                        onWallpaperChange = {
                            wallpaperIndex = it.ordinal
                            media = null
                            NovaWallpaperStore.clearMedia(context)
                            NovaWallpaperStore.savePreset(context, it)
                        },
                        media = media,
                        onPickMedia = { picker.launch(arrayOf("image/*", "video/*")) },
                        onClearMedia = {
                            media = null
                            NovaWallpaperStore.clearMedia(context)
                        },
                        onClose = { finish() },
                    )
                }
            }
        }
    }
}
