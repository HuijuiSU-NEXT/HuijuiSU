@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huijui.sunext.ui.theme
import com.huijui.sunext.R

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
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

class ThemeConfig(
    private val defaultSeedColor: () -> Int,
) {
    // 主题状态
    var customBackgroundUri by mutableStateOf<Uri?>(null)
    var backgroundDim by mutableFloatStateOf(0f)
    var forceDarkMode by mutableStateOf<Boolean?>(null)
    var seedColor by mutableIntStateOf(defaultSeedColor())
    var useDynamicColor by mutableStateOf(false)
    var useBuiltinMonoFont by mutableStateOf(false)
    var monetCompatSeedColor by mutableIntStateOf(defaultSeedColor())
    var dynamicColorSpec by mutableStateOf(ColorSpec.SpecVersion.SPEC_2021)
    var dynamicPaletteStyle by mutableStateOf(PaletteStyle.TonalSpot)

    // 背景状态
    var backgroundImageLoaded by mutableStateOf(false)
    var isThemeChanging by mutableStateOf(false)
    var isHighContrastMode by mutableStateOf(false)
    var isEnableBlur by mutableStateOf(false)
    var isEnableBlurExp by mutableStateOf(false)
    var isUseBackgroundSeedColor by mutableStateOf(false)
    var bottomBarStyle by mutableStateOf(BottomBarStyle.MATERIAL3_EXPRESSIVE)

    fun resetBackgroundState() {
        backgroundImageLoaded = false
        isThemeChanging = true
    }

    fun reset() {
        customBackgroundUri = null
        forceDarkMode = null
        seedColor = defaultSeedColor()
        useDynamicColor = false
        monetCompatSeedColor = defaultSeedColor()
        dynamicColorSpec = ColorSpec.SpecVersion.SPEC_2021
        dynamicPaletteStyle = PaletteStyle.TonalSpot
        backgroundImageLoaded = false
        isThemeChanging = false
    }
}

private val BuiltinspaceFontFamily = FontFamily(
    Font(com.huijui.sunext.R.font.jetbrains_mono),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun monospaceFontFamily(): FontFamily {
    val themeConfig = koinInject<ThemeConfig>()
    return if (themeConfig.useBuiltinMonoFont) {
        BuiltinspaceFontFamily
    } else {
        FontFamily.Monospace
    }
}

@Stable
class BackgroundRenderState {
    var imagePainter: AsyncImagePainter? by mutableStateOf(null)
    var imageBitmap: ImageBitmap? by mutableStateOf(null)
    var blurImageBitmap: ImageBitmap? by mutableStateOf(null)
    var blurViewportSize by mutableStateOf(IntSize(0, 0))
    var blurFrameTick by mutableIntStateOf(0)
    var seedColor by mutableIntStateOf(0)
}

val LocalBackgroundRenderState = compositionLocalOf<BackgroundRenderState> {
    error("BackgroundRenderState is not provided")
}

