
package com.huijui.sunext.ui.theme

import com.huijui.sunext.R
import androidx.compose.ui.text.font.FontWeight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.huijui.sunext.data.AppSettingsRepository
import com.huijui.sunext.data.theme.MonetCompatColorSource
import com.huijui.sunext.data.theme.ThemeRepository
import com.huijui.sunext.ui.overscroll.StretchOverscrollCompensationState
import com.huijui.sunext.ui.util.LocalBackgroundBlurAnchor
import com.huijui.sunext.ui.util.LocalBlurState
import com.huijui.sunext.ui.util.LocalPagerPage
import com.huijui.sunext.ui.util.LocalPagerState
import com.huijui.sunext.ui.util.LocalStretchOverscrollCompensationState
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect
import top.yukonga.miuix.kmp.blur.textureBlurEffect

@Stable

class BackgroundManager(
    private val settings: AppSettingsRepository,
    private val config: ThemeConfig,
    private val cardConfig: CardConfig,
) {
    private val tag = "BackgroundManager"

    fun saveBackgroundDim(dim: Float) {
        config.backgroundDim = dim
        settings.putFloat("background_dim", dim)
    }

    fun saveEnableBlur(enable: Boolean) {
        config.isEnableBlur = enable
        settings.putBoolean("enable_blur", enable)
    }

    fun saveEnableBlurExp(enable: Boolean) {
        config.isEnableBlurExp = enable
        settings.putBoolean("enable_blur_exp", enable)
    }

    fun saveBottomBarStyle(style: BottomBarStyle) {
        config.bottomBarStyle = style
        settings.putInt("bottom_bar_style", style.ordinal)
    }

    fun saveUseBackgroundSeedColor(enable: Boolean) {
        config.isUseBackgroundSeedColor = enable
        settings.putBoolean("use_background_seed_color", enable)
    }

    fun saveEnableHighContrastMode(enable: Boolean) {
        config.isHighContrastMode = enable
        settings.putBoolean("high_contrast_mode", enable)
    }

    suspend fun saveAndApplyCustomBackground(
        context: Context,
        uri: Uri,
    ): Boolean {
        val appContext = context.applicationContext
        return try {
            val finalUri = withContext(Dispatchers.IO) {
                copyImageToInternalStorage(appContext, uri)
            } ?: return false

            saveBackgroundUri(finalUri)
            config.customBackgroundUri = finalUri
            cardConfig.updateBackground(true)
            withContext(Dispatchers.IO) {
                clearBackgroundBlurCache(appContext)
            }
            config.backgroundImageLoaded = false

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "保存背景失败: ${e.message}", e)
            false
        }
    }

    fun clearCustomBackground(context: Context) {
        saveBackgroundUri(null)
        config.customBackgroundUri = null
        cardConfig.updateBackground(false)
        clearBackgroundBlurCache(context)
        config.backgroundImageLoaded = false
    }

    fun loadCustomBackground() {
        val prefs = settings
        val uriString = prefs.getString("custom_background", null)

        val newUri = uriString?.toUri()

        if (config.customBackgroundUri?.toString() != newUri?.toString()) {
            Log.d(tag, "加载自定义背景: $uriString")
            config.customBackgroundUri = newUri
            config.backgroundImageLoaded = false
            cardConfig.updateBackground(newUri != null)
        }

        config.backgroundDim = prefs.getFloat("background_dim", 0f).coerceIn(0f, 1f)
        config.isEnableBlurExp = prefs.getBoolean("enable_blur_exp", true)
        config.isUseBackgroundSeedColor = prefs.getBoolean("use_background_seed_color", false)
        config.isHighContrastMode = prefs.getBoolean("high_contrast_mode", true)
    }

    private fun saveBackgroundUri(uri: Uri?) {
        settings.putString("custom_background", uri?.toString())
    }

    fun clearBackgroundBlurCache(context: Context) {
        runCatching {
            backgroundBlurCacheFile(context).delete()
            legacyBackgroundBlurCacheDir(context).deleteRecursively()
        }.onFailure {
            Log.w(tag, "Failed to clear background blur cache: ${it.message}")
        }
    }

    private fun copyImageToInternalStorage(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "custom_background.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            inputStream.close()

            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(tag, "复制图片失败: ${e.message}", e)
            null
        }
    }
}

@Composable
fun KernelSUTheme(
    dpi: Int = 0,
    darkTheme: Boolean? = null,
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val themeConfig = koinInject<ThemeConfig>()
    val themeRepository = koinInject<ThemeRepository>()
    val backgroundManager = koinInject<BackgroundManager>()
    val cardConfig = koinInject<CardConfig>()
    val settings = koinInject<AppSettingsRepository>()
    val backgroundRenderState = remember { BackgroundRenderState() }
    val resolvedDarkTheme = darkTheme ?: isInDarkTheme(themeConfig.forceDarkMode)
    val resolvedDynamicColor = dynamicColor ?: themeConfig.useDynamicColor
    val context = LocalContext.current
    val systemIsDark = isSystemInDarkTheme()

    // 初始化主题
    ThemeInitializer(
        context = context,
        systemIsDark = systemIsDark,
        themeConfig = themeConfig,
        themeRepository = themeRepository,
        backgroundManager = backgroundManager,
        cardConfig = cardConfig,
        settings = settings,
    )

    // 创建颜色方案
    val colorScheme = createColorScheme(
        resolvedDarkTheme,
        resolvedDynamicColor,
        themeConfig,
        backgroundRenderState,
    )

    // 系统栏样式
    SystemBarController(resolvedDarkTheme)

    val systemDensity = LocalDensity.current

    val density = remember(systemDensity, dpi) {
        if (dpi <= 0f) {
            systemDensity
        } else {
            val targetDensity = dpi / 160f
            Density(density = targetDensity, fontScale = systemDensity.fontScale)
        }
    }

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalBackgroundRenderState provides backgroundRenderState,
    ) {
        MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
    ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BackgroundLayer(themeConfig, settings, backgroundRenderState)
                content()
            }
        }
    }
}

@Composable
private fun ThemeInitializer(
    context: Context,
    systemIsDark: Boolean,
    themeConfig: ThemeConfig,
    themeRepository: ThemeRepository,
    backgroundManager: BackgroundManager,
    cardConfig: CardConfig,
    settings: AppSettingsRepository,
) {
    // 处理系统主题变化
    LaunchedEffect(systemIsDark) {
        if (themeConfig.forceDarkMode == null) {
            cardConfig.apply {
                load()
                setThemeDefaults(systemIsDark)
                save()
            }
        }
    }

    // 初始加载配置
    LaunchedEffect(Unit) {
        // Composition can start before application startup has finished preloading
        // DataStore. Read it here first so the persisted background URI is never
        // mistaken for an unset value during process recreation.
        settings.preload()
        themeConfig.forceDarkMode = themeRepository.loadThemeMode()
        themeConfig.seedColor = themeRepository.loadSeedColor()
        themeConfig.useDynamicColor = themeRepository.loadDynamicColorState()
        themeConfig.dynamicColorSpec = themeRepository.loadDynamicColorSpec()
        themeConfig.dynamicPaletteStyle = themeRepository.loadDynamicPaletteStyle(
            themeConfig.dynamicColorSpec,
        )
        themeConfig.isEnableBlur = settings.getBoolean("enable_blur", true) // 默认开启，但用户可在设置关闭
        themeConfig.bottomBarStyle = BottomBarStyle.fromOrdinal(settings.getInt("bottom_bar_style", 0))
        cardConfig.load()
        backgroundManager.loadCustomBackground()
    }

    MonetCompatInitializer(themeConfig, themeRepository)
}

@Composable
private fun MonetCompatInitializer(themeConfig: ThemeConfig, themeRepository: ThemeRepository) {
    val source = koinInject<MonetCompatColorSource>()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(source, themeConfig) {
        source.colors.collect { color ->
            themeConfig.monetCompatSeedColor = color
            themeConfig.seedColor = themeRepository.loadSeedColor()
        }
    }
    LaunchedEffect(source, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Allow the cached theme to draw before starting wallpaper IPC and palette work.
            withFrameNanos { }
            withFrameNanos { }
            source.refresh()
        }
    }
}

@Composable
private fun BackgroundLayer(
    themeConfig: ThemeConfig,
    settings: AppSettingsRepository,
    renderState: BackgroundRenderState,
) {
    val backgroundUri = remember(themeConfig.customBackgroundUri) {
        themeConfig.customBackgroundUri
    }

    LaunchedEffect(themeConfig.customBackgroundUri) {
        if (themeConfig.customBackgroundUri == null) {
            renderState.imageBitmap = null
            renderState.imagePainter = null
            renderState.blurImageBitmap = null
            renderState.seedColor = 0
            settings.remove("cached_seed_color")
        }
    }

    val hasBackgroundBitmap = renderState.imageBitmap != null
    val hasBlurBitmap = renderState.blurImageBitmap != null
    val needsFallbackFrames = hasBackgroundBitmap &&
        (!themeConfig.isEnableBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    val needsBlurFrames =
        (themeConfig.isEnableBlurExp && hasBlurBitmap) ||
            (themeConfig.isEnableBlur && hasBackgroundBitmap)

    LaunchedEffect(needsFallbackFrames, needsBlurFrames) {
        if (!needsFallbackFrames && !needsBlurFrames) return@LaunchedEffect

        while (true) {
            withFrameNanos { }
            renderState.blurFrameTick = if (renderState.blurFrameTick == Int.MAX_VALUE) {
                0
            } else {
                renderState.blurFrameTick + 1
            }
        }
    }

    // 默认背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-2f)
            .onSizeChanged { size ->
                if (
                    size.width > 0 &&
                    size.height > 0 &&
                    renderState.blurViewportSize != size
                ) {
                    renderState.blurViewportSize = size
                    renderState.blurImageBitmap = null
                }
            }
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
            ),
    )

    // 自定义背景
    backgroundUri?.let { uri ->
        BackgroundInitializer(
            uri = uri,
            themeConfig = themeConfig,
            settings = settings,
            renderState = renderState,
        )
    }
}

internal const val BACKGROUND_BLUR_RADIUS = 45f

/**
 * Captures background content for blurEffect child nodes,
 * It will only work when blurState available
 * @return modified modifier
 */

@Composable
private fun generateTypography(themeConfig: ThemeConfig): androidx.compose.material3.Typography {
    val darkMode = isInDarkTheme(themeConfig.forceDarkMode)

    fun generateShadow(originalShadow: Shadow?): Shadow? {
        if (!themeConfig.isHighContrastMode) return originalShadow
        val shadow = originalShadow ?: Shadow(
            offset = Offset(1.5f, 1.5f),
            blurRadius = 0f,
        )
        return shadow.copy(
            color = if (darkMode) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
        )
    }

    fun TextStyle.applyShadow() = this.copy(shadow = generateShadow(shadow))
    val typography = MaterialTheme.typography

    return typography.copy(
        displayLarge = typography.displayLarge.applyShadow(),
        displayMedium = typography.displayMedium.applyShadow(),
        displaySmall = typography.displaySmall.applyShadow(),
        headlineLarge = typography.headlineLarge.applyShadow(),
        headlineMedium = typography.headlineMedium.applyShadow(),
        headlineSmall = typography.headlineSmall.applyShadow(),
        titleLarge = typography.titleLarge.applyShadow(),
        titleMedium = typography.titleMedium.applyShadow(),
        titleSmall = typography.titleSmall.applyShadow(),
        bodyLarge = typography.bodyLarge.applyShadow(),
        bodyMedium = typography.bodyMedium.applyShadow(),
        bodySmall = typography.bodySmall.applyShadow(),
        labelLarge = typography.labelLarge.applyShadow(),
        labelMedium = typography.labelMedium.applyShadow(),
        labelSmall = typography.labelSmall.applyShadow(),
        displayLargeEmphasized = typography.displayLargeEmphasized.applyShadow(),
        displayMediumEmphasized = typography.displayMediumEmphasized.applyShadow(),
        displaySmallEmphasized = typography.displaySmallEmphasized.applyShadow(),
        headlineLargeEmphasized = typography.headlineLargeEmphasized.applyShadow(),
        headlineMediumEmphasized = typography.headlineMediumEmphasized.applyShadow(),
        headlineSmallEmphasized = typography.headlineSmallEmphasized.applyShadow(),
        titleLargeEmphasized = typography.titleLargeEmphasized.applyShadow(),
        titleMediumEmphasized = typography.titleMediumEmphasized.applyShadow(),
        titleSmallEmphasized = typography.titleSmallEmphasized.applyShadow(),
        bodyLargeEmphasized = typography.bodyLargeEmphasized.applyShadow(),
        bodyMediumEmphasized = typography.bodyMediumEmphasized.applyShadow(),
        bodySmallEmphasized = typography.bodySmallEmphasized.applyShadow(),
        labelLargeEmphasized = typography.labelLargeEmphasized.applyShadow(),
        labelMediumEmphasized = typography.labelMediumEmphasized.applyShadow(),
        labelSmallEmphasized = typography.labelSmallEmphasized.applyShadow(),
    )
}

@Composable
private fun createColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    themeConfig: ThemeConfig,
    renderState: BackgroundRenderState,
): ColorScheme {
    val seedColor =
        when {
            dynamicColor && themeConfig.isUseBackgroundSeedColor && renderState.seedColor != 0 -> {
                renderState.seedColor
            }

            dynamicColor -> {
                themeConfig.monetCompatSeedColor
            }

            else -> {
                themeConfig.seedColor
            }
        }

    return dynamicColorScheme(
        seedColor = Color(seedColor),
        isDark = darkTheme,
        style = themeConfig.dynamicPaletteStyle,
        specVersion = themeConfig.dynamicColorSpec,
    )
}

@Composable
private fun SystemBarController(darkMode: Boolean) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ) { darkMode },
            navigationBarStyle = if (darkMode) {
                SystemBarStyle.dark(Color.Transparent.toArgb())
            } else {
                SystemBarStyle.light(
                    Color.Transparent.toArgb(),
                    Color.Transparent.toArgb(),
                )
            },
        )
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Boolean?): Boolean = when (themeMode) {
    // 强制深色
    true -> true

    // 强制浅色
    false -> false

    null -> isSystemInDarkTheme() // 跟随系统
}

val GeekFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, weight = FontWeight.Normal)
)
