package com.huijui.sunext.ui.theme

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

@Composable
fun Modifier.blurSource(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this

    return LocalBlurState.current?.let {
        this.then(Modifier.layerBackdrop(it))
    } ?: this
}

/**
 * Render blur when backdrop available
 * Falls back to redrawing the app background below the content so translucent bars do not show
 * scrolling content through them when blur is disabled or unavailable.
 * @param compensateHorizontalOverscroll reverse horizontal stretch in the sampled background
 * @param compensateVerticalOverscroll reverse vertical stretch in the sampled background
 * @param useFixedSurfaceBoundsForOverscroll use this surface's bounds with the published stretch
 * amount; fixed navigation surfaces use this to stay visually stationary outside the scroller
 * @return modified modifier
 */
@Composable
fun Modifier.blurEffect(
    compensateHorizontalOverscroll: Boolean = true,
    compensateVerticalOverscroll: Boolean = false,
    useFixedSurfaceBoundsForOverscroll: Boolean = false,
): Modifier {
    val themeConfig = koinInject<ThemeConfig>()
    val cardConfig = koinInject<CardConfig>()
    if (!themeConfig.isEnableBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return renderBackgroundFallback(
            compensateHorizontalOverscroll = compensateHorizontalOverscroll,
            compensateVerticalOverscroll = compensateVerticalOverscroll,
            useFixedSurfaceBoundsForOverscroll = useFixedSurfaceBoundsForOverscroll,
        )
    }

    val renderState = LocalBackgroundRenderState.current
    val backgroundAnchor = LocalBackgroundBlurAnchor.current
    val stretchOverscrollState = LocalStretchOverscrollCompensationState.current
    var coordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    return LocalBlurState.current?.let { backdrop ->
        // 0.8f like haze, for material design without custom background enable
        val blurTintAlpha = if (cardConfig.isCustomBackgroundEnabled) {
            cardConfig.cardAlpha
        } else {
            0.8f
        }

        val blendColor =
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = blurTintAlpha)

        this.then(
            Modifier
                .onGloballyPositioned { newCoordinates ->
                    coordinates = newCoordinates.takeIf { it.isAttached }
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        renderState.blurFrameTick

                        textureBlurEffect(
                            blurRadiusX = 25f,
                            colors = BlurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = blendColor),
                                ),
                            ),
                        )

                        val boundsInBackground =
                            coordinates?.boundsInBackgroundNow(backgroundAnchor)
                        val stretchCompensation = stretchOverscrollState
                            ?.resolveBitmapCompensation(
                                backgroundCoordinates = backgroundAnchor,
                                compensateHorizontal = compensateHorizontalOverscroll,
                                compensateVertical = compensateVerticalOverscroll,
                                compensationCoordinates = coordinates
                                    .takeIf { useFixedSurfaceBoundsForOverscroll },
                            )

                        if (boundsInBackground != null && stretchCompensation != null) {
                            stretchOverscrollCompensationEffect(
                                boundsInBackground = boundsInBackground,
                                compensation = stretchCompensation,
                            )
                        }
                    },
                ),
        )
    } ?: renderBackgroundFallback(
        compensateHorizontalOverscroll = compensateHorizontalOverscroll,
        compensateVerticalOverscroll = compensateVerticalOverscroll,
        useFixedSurfaceBoundsForOverscroll = useFixedSurfaceBoundsForOverscroll,
    )
}

@Composable
private fun Modifier.renderBackgroundFallback(
    compensateHorizontalOverscroll: Boolean,
    compensateVerticalOverscroll: Boolean,
    useFixedSurfaceBoundsForOverscroll: Boolean,
): Modifier {
    val themeConfig = koinInject<ThemeConfig>()
    val renderState = LocalBackgroundRenderState.current
    var coordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val dimColor = backgroundColor.copy(alpha = themeConfig.backgroundDim)
    val backgroundBitmap = renderState.imageBitmap
    val backgroundAnchor = LocalBackgroundBlurAnchor.current
    val pagerPage = LocalPagerPage.current
    val pagerState = if (pagerPage != null) LocalPagerState.current else null
    val layoutDirection = LocalLayoutDirection.current
    val stretchOverscrollState = LocalStretchOverscrollCompensationState.current
    val stretchRenderer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StretchBackgroundRenderer()
        } else {
            null
        }
    }

    return this
        .onGloballyPositioned { newCoordinates ->
            coordinates = newCoordinates.takeIf { it.isAttached }
        }
        .drawWithContent {
            renderState.blurFrameTick

            val boundsInBackground = coordinates?.boundsInBackgroundNow(backgroundAnchor)
                ?: coordinates?.localBoundsInWindowNow()
            val viewportSize = backgroundAnchor
                ?.takeIf { it.isAttached && it.size.width > 0 && it.size.height > 0 }
                ?.size
                ?: renderState.blurViewportSize
            val currentPagerState = pagerState?.takeIf { state ->
                pagerPage != null && pagerPage in 0 until state.pageCount
            }
            val hasPagerPage = currentPagerState != null
            val pagerViewportSize = currentPagerState?.layoutInfo?.viewportSize?.takeIf {
                it.width > 0 && it.height > 0
            }
            val pagerViewportWidth = pagerViewportSize?.width ?: viewportSize.width
            val pageOffset = if (currentPagerState != null && pagerPage != null) {
                currentPagerState.getOffsetDistanceInPages(pagerPage)
            } else {
                0f
            }
            val physicalPageOffset = pageOffset * pagerViewportWidth *
                if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
            val offsetBoundsInBackground = boundsInBackground?.let { bounds ->
                if (hasPagerPage) {
                    val leadingNavigationWidth =
                        (viewportSize.width - pagerViewportWidth).coerceAtLeast(0)
                    val pagerViewportLeft = if (layoutDirection == LayoutDirection.Ltr) {
                        leadingNavigationWidth.toFloat()
                    } else {
                        0f
                    }

                    // Equivalent to translating the backdrop painter by -physicalPageOffset:
                    // crop from the page offset and account for a leading landscape navigation rail
                    // so the background remains fixed while the page moves.
                    Rect(
                        left = pagerViewportLeft + physicalPageOffset,
                        top = bounds.top,
                        right = pagerViewportLeft + physicalPageOffset + bounds.width,
                        bottom = bounds.bottom,
                    )
                } else {
                    bounds
                }
            }
            val bitmapBounds = if (backgroundBitmap != null && offsetBoundsInBackground != null) {
                offsetBoundsInBackground.mapToBitmapBounds(
                    bitmap = backgroundBitmap,
                    viewportSize = viewportSize,
                )
            } else {
                null
            }
            // Fixed navigation surfaces are outside the pager's stretch RenderNode. Their fallback
            // draws the original, unstretched bitmap, so applying the published inverse mapping
            // here would introduce movement instead of cancelling it.
            val stretchCompensation = if (useFixedSurfaceBoundsForOverscroll) {
                null
            } else {
                stretchOverscrollState
                    ?.resolveBitmapCompensation(
                        backgroundCoordinates = backgroundAnchor,
                        compensateHorizontal = compensateHorizontalOverscroll,
                        compensateVertical = compensateVerticalOverscroll,
                    )
                    ?.mapToBitmapCoordinates(
                        bitmap = backgroundBitmap,
                        viewportSize = viewportSize,
                    )
            }

            if (backgroundBitmap != null && bitmapBounds != null) {
                if (stretchCompensation != null) {
                    // in fact no need check sdk int. but make lint happy
                    if (stretchRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stretchRenderer.draw(
                            scope = this,
                            bitmap = backgroundBitmap,
                            boundsInBackground = bitmapBounds,
                            compensation = stretchCompensation,
                        )
                    } else {
                        drawStretchCompensatedBitmap(
                            bitmap = backgroundBitmap,
                            boundsInBackground = bitmapBounds,
                            compensation = stretchCompensation,
                        )
                    }
                } else {
                    drawBitmapIntersection(
                        bitmap = backgroundBitmap,
                        boundsInBackground = bitmapBounds,
                    )
                }
                drawRect(color = dimColor)
            } else {
                drawRect(color = backgroundColor)
            }

            drawContent()
        }
}

private fun Rect.mapToBitmapBounds(
    bitmap: ImageBitmap,
    viewportSize: IntSize,
): Rect? {
    if (
        bitmap.width <= 0 ||
        bitmap.height <= 0 ||
        viewportSize.width <= 0 ||
        viewportSize.height <= 0
    ) {
        return null
    }

    val scale = maxOf(
        viewportSize.width / bitmap.width.toFloat(),
        viewportSize.height / bitmap.height.toFloat(),
    )
    val renderedWidth = bitmap.width * scale
    val renderedHeight = bitmap.height * scale
    val renderedLeft = (viewportSize.width - renderedWidth) / 2f
    val renderedTop = (viewportSize.height - renderedHeight) / 2f

    return Rect(
        left = (left - renderedLeft) / scale,
        top = (top - renderedTop) / scale,
        right = (right - renderedLeft) / scale,
        bottom = (bottom - renderedTop) / scale,
    )
}

@Composable
fun Modifier.renderBackgroundBlur(
    tintColor: Color? = null,
): Modifier {
    val themeConfig = koinInject<ThemeConfig>()
    val cardConfig = koinInject<CardConfig>()
    val renderState = LocalBackgroundRenderState.current
    if (!themeConfig.isEnableBlurExp) return this

    var coordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    val tintColor = (tintColor ?: MaterialTheme.colorScheme.surfaceBright).copy(
        alpha = cardConfig.cardAlpha,
    )
    val backgroundBlurAnchor = LocalBackgroundBlurAnchor.current
    val stretchOverscrollState = LocalStretchOverscrollCompensationState.current
    val stretchRenderer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StretchBackgroundRenderer()
        } else {
            null
        }
    }

    return this
        .onGloballyPositioned { newCoordinates ->
            coordinates = newCoordinates.takeIf { it.isAttached }
        }
        .drawWithContent {
            renderState.blurFrameTick

            val currentBitmap = renderState.blurImageBitmap
            val currentBoundsInBackground = coordinates?.boundsInBackgroundNow(backgroundBlurAnchor)
            val stretchCompensation = stretchOverscrollState
                ?.resolveBitmapCompensation(backgroundBlurAnchor)

            if (
                currentBitmap != null &&
                currentBoundsInBackground != null &&
                currentBoundsInBackground.width > 0f &&
                currentBoundsInBackground.height > 0f
            ) {
                if (stretchCompensation != null) {
                    // in fact no need check sdk int. but make lint happy
                    if (stretchRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stretchRenderer.draw(
                            scope = this,
                            bitmap = currentBitmap,
                            boundsInBackground = currentBoundsInBackground,
                            compensation = stretchCompensation,
                        )
                    } else {
                        drawStretchCompensatedBitmap(
                            bitmap = currentBitmap,
                            boundsInBackground = currentBoundsInBackground,
                            compensation = stretchCompensation,
                        )
                    }
                } else {
                    drawBitmapIntersection(
                        bitmap = currentBitmap,
                        boundsInBackground = currentBoundsInBackground,
                    )
                }

                drawRect(
                    color = tintColor,
                    blendMode = BlendMode.SrcOver,
                )
            }

            drawContent()
        }
}

private fun LayoutCoordinates.boundsInBackgroundNow(
    backgroundCoordinates: LayoutCoordinates?,
): Rect? {
    if (!isAttached || size.width <= 0 || size.height <= 0) return null

    return backgroundCoordinates
        ?.takeIf { it.isAttached && it.size.width > 0 && it.size.height > 0 }
        ?.let { boundsInCoordinatesNow(it) }
        ?: localBoundsInWindowNow()
}

private fun LayoutCoordinates.boundsInCoordinatesNow(
    targetCoordinates: LayoutCoordinates,
): Rect? {
    fun localToTarget(point: Offset): Offset {
        val screenPoint = localToScreen(point)
        if (!screenPoint.isUsable()) return Offset.Unspecified

        return targetCoordinates.screenToLocal(screenPoint)
    }

    val width = size.width.toFloat()
    val height = size.height.toFloat()

    return boundsFromCorners(
        topLeft = localToTarget(Offset.Zero),
        topRight = localToTarget(Offset(width, 0f)),
        bottomLeft = localToTarget(Offset(0f, height)),
        bottomRight = localToTarget(Offset(width, height)),
    )
}

private fun LayoutCoordinates.localBoundsInWindowNow(): Rect? {
    val width = size.width.toFloat()
    val height = size.height.toFloat()

    return boundsFromCorners(
        topLeft = localToWindow(Offset.Zero),
        topRight = localToWindow(Offset(width, 0f)),
        bottomLeft = localToWindow(Offset(0f, height)),
        bottomRight = localToWindow(Offset(width, height)),
    )
}

private fun boundsFromCorners(
    topLeft: Offset,
    topRight: Offset,
    bottomLeft: Offset,
    bottomRight: Offset,
): Rect? {
    if (
        !topLeft.isUsable() ||
        !topRight.isUsable() ||
        !bottomLeft.isUsable() ||
        !bottomRight.isUsable()
    ) {
        return null
    }

    return Rect(
        left = minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x),
        top = minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y),
        right = maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x),
        bottom = maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y),
    )
}

private data class StretchBitmapCompensation(
    val horizontalAmount: Float,
    val horizontalOrigin: Float,
    val horizontalSize: Float,
    val verticalAmount: Float,
    val verticalOrigin: Float,
    val verticalSize: Float,
)

private fun StretchBitmapCompensation.mapToBitmapCoordinates(
    bitmap: ImageBitmap?,
    viewportSize: IntSize,
): StretchBitmapCompensation? {
    if (
        bitmap == null ||
        bitmap.width <= 0 ||
        bitmap.height <= 0 ||
        viewportSize.width <= 0 ||
        viewportSize.height <= 0
    ) {
        return null
    }

    val scale = maxOf(
        viewportSize.width / bitmap.width.toFloat(),
        viewportSize.height / bitmap.height.toFloat(),
    )
    val renderedLeft = (viewportSize.width - bitmap.width * scale) / 2f
    val renderedTop = (viewportSize.height - bitmap.height * scale) / 2f

    return StretchBitmapCompensation(
        horizontalAmount = horizontalAmount,
        horizontalOrigin = (horizontalOrigin - renderedLeft) / scale,
        horizontalSize = horizontalSize / scale,
        verticalAmount = verticalAmount,
        verticalOrigin = (verticalOrigin - renderedTop) / scale,
        verticalSize = verticalSize / scale,
    )
}

private fun StretchOverscrollCompensationState.resolveBitmapCompensation(
    backgroundCoordinates: LayoutCoordinates?,
    compensateHorizontal: Boolean = true,
    compensateVertical: Boolean = true,
    compensationCoordinates: LayoutCoordinates? = null,
): StretchBitmapCompensation? {
    val compensationBounds = compensationCoordinates
        ?.boundsInBackgroundNow(backgroundCoordinates)
    val horizontalAxis = horizontal
        ?.takeIf { compensateHorizontal && it.amount != 0f }
    val verticalAxis = vertical
        ?.takeIf { compensateVertical && it.amount != 0f }
    val horizontalBounds = horizontalAxis?.let { axis ->
        compensationBounds
            ?.takeIf { it.width > 0f }
            ?: axis.coordinates
                .boundsInBackgroundNow(backgroundCoordinates)
                ?.takeIf { it.width > 0f }
    }
    val verticalBounds = verticalAxis?.let { axis ->
        compensationBounds
            ?.takeIf { it.height > 0f }
            ?: axis.coordinates
                .boundsInBackgroundNow(backgroundCoordinates)
                ?.takeIf { it.height > 0f }
    }

    if (horizontalBounds == null && verticalBounds == null) return null

    return StretchBitmapCompensation(
        horizontalAmount = if (horizontalBounds != null) horizontalAxis.amount else 0f,
        horizontalOrigin = horizontalBounds?.left ?: 0f,
        horizontalSize = horizontalBounds?.width ?: 1f,
        verticalAmount = if (verticalBounds != null) verticalAxis.amount else 0f,
        verticalOrigin = verticalBounds?.top ?: 0f,
        verticalSize = verticalBounds?.height ?: 1f,
    )
}

private fun BackdropEffectScope.stretchOverscrollCompensationEffect(
    boundsInBackground: Rect,
    compensation: StretchBitmapCompensation,
) {
    val inputScale = downscaleFactor.coerceAtLeast(1).toFloat()
    val inputPadding = padding

    runtimeShaderEffect(
        key = "StretchOverscrollCompensation",
        shaderString = STRETCH_BACKGROUND_SHADER,
        uniformShaderName = "background",
    ) {
        setFloatUniform(
            "cardOrigin",
            boundsInBackground.left - inputPadding,
            boundsInBackground.top - inputPadding,
        )
        setFloatUniform("cardScale", inputScale, inputScale)
        setFloatUniform(
            "inputOrigin",
            (-boundsInBackground.left + inputPadding) / inputScale,
            (-boundsInBackground.top + inputPadding) / inputScale,
        )
        setFloatUniform("inputScale", 1f / inputScale, 1f / inputScale)
        setFloatUniform(
            "stretchAmount",
            compensation.horizontalAmount,
            compensation.verticalAmount,
        )
        setFloatUniform(
            "stretchOrigin",
            compensation.horizontalOrigin,
            compensation.verticalOrigin,
        )
        setFloatUniform(
            "stretchSize",
            compensation.horizontalSize,
            compensation.verticalSize,
        )
    }
}

private data class BitmapDrawSegment(
    val srcStart: Int,
    val srcEnd: Int,
    val dstStart: Int,
    val dstEnd: Int,
)

private fun buildBitmapDrawSegments(
    sourceStart: Float,
    sourceEnd: Float,
    bitmapSize: Int,
    destinationSize: Float,
): List<BitmapDrawSegment> {
    val sourceSpan = sourceEnd - sourceStart
    val destinationEnd = ceil(destinationSize).toInt()

    if (bitmapSize <= 0 || sourceSpan <= 0f || destinationEnd <= 0) {
        return emptyList()
    }

    fun mapToDestination(value: Float): Float = (value - sourceStart) / sourceSpan * destinationSize

    val segments = mutableListOf<BitmapDrawSegment>()

    // Emulate Shader.TileMode.CLAMP for areas that are above / left of the bitmap.
    if (sourceStart < 0f) {
        val dstEnd = ceil(mapToDestination(0f).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)

        if (dstEnd > 0) {
            segments += BitmapDrawSegment(
                srcStart = 0,
                srcEnd = 1,
                dstStart = 0,
                dstEnd = dstEnd,
            )
        }
    }

    val clippedStart = sourceStart.coerceIn(0f, bitmapSize.toFloat())
    val clippedEnd = sourceEnd.coerceIn(0f, bitmapSize.toFloat())

    if (clippedEnd > clippedStart) {
        val srcStart = floor(clippedStart).toInt().coerceIn(0, bitmapSize - 1)
        val srcEnd = ceil(clippedEnd).toInt().coerceIn(srcStart + 1, bitmapSize)
        val dstStart = floor(mapToDestination(clippedStart).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)
        val dstEnd = ceil(mapToDestination(clippedEnd).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)

        if (dstEnd > dstStart) {
            segments += BitmapDrawSegment(
                srcStart = srcStart,
                srcEnd = srcEnd,
                dstStart = dstStart,
                dstEnd = dstEnd,
            )
        }
    }

    // Emulate Shader.TileMode.CLAMP for areas that are below / right of the bitmap.
    if (sourceEnd > bitmapSize.toFloat()) {
        val dstStart = floor(mapToDestination(bitmapSize.toFloat()).coerceIn(0f, destinationSize))
            .toInt()
            .coerceIn(0, destinationEnd)

        if (destinationEnd > dstStart) {
            segments += BitmapDrawSegment(
                srcStart = bitmapSize - 1,
                srcEnd = bitmapSize,
                dstStart = dstStart,
                dstEnd = destinationEnd,
            )
        }
    }

    // The whole destination is outside one side of the source bitmap.
    if (segments.isEmpty()) {
        val src = if (sourceEnd <= 0f) 0 else bitmapSize - 1
        segments += BitmapDrawSegment(
            srcStart = src,
            srcEnd = src + 1,
            dstStart = 0,
            dstEnd = destinationEnd,
        )
    }

    return segments
}

private fun ContentDrawScope.drawBitmapIntersection(
    bitmap: ImageBitmap,
    boundsInBackground: Rect,
) {
    val bitmapWidth = bitmap.width
    val bitmapHeight = bitmap.height

    if (
        bitmapWidth <= 0 ||
        bitmapHeight <= 0 ||
        size.width <= 0f ||
        size.height <= 0f ||
        boundsInBackground.width <= 0f ||
        boundsInBackground.height <= 0f
    ) {
        return
    }

    val xSegments = buildBitmapDrawSegments(
        sourceStart = boundsInBackground.left,
        sourceEnd = boundsInBackground.right,
        bitmapSize = bitmapWidth,
        destinationSize = size.width,
    )
    val ySegments = buildBitmapDrawSegments(
        sourceStart = boundsInBackground.top,
        sourceEnd = boundsInBackground.bottom,
        bitmapSize = bitmapHeight,
        destinationSize = size.height,
    )

    for (xSegment in xSegments) {
        for (ySegment in ySegments) {
            val srcWidth = xSegment.srcEnd - xSegment.srcStart
            val srcHeight = ySegment.srcEnd - ySegment.srcStart
            val dstWidth = xSegment.dstEnd - xSegment.dstStart
            val dstHeight = ySegment.dstEnd - ySegment.dstStart

            if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) {
                continue
            }

            drawImage(
                image = bitmap,
                srcOffset = IntOffset(xSegment.srcStart, ySegment.srcStart),
                srcSize = IntSize(srcWidth, srcHeight),
                dstOffset = IntOffset(xSegment.dstStart, ySegment.dstStart),
                dstSize = IntSize(dstWidth, dstHeight),
                blendMode = BlendMode.SrcOver,
            )
        }
    }
}

private const val STRETCH_BACKGROUND_SHADER = """
    uniform shader background;
    uniform float2 cardOrigin;
    uniform float2 cardScale;
    uniform float2 inputOrigin;
    uniform float2 inputScale;
    uniform float2 stretchAmount;
    uniform float2 stretchOrigin;
    uniform float2 stretchSize;

    float reverseMapStart(float overscroll, float sourcePosition) {
        float numerator =
            (-sourcePosition * overscroll * overscroll) -
            (2.0 * sourcePosition * overscroll) -
            sourcePosition;
        float denominator =
            1.0 +
            (0.3 * overscroll) +
            (0.7 * sourcePosition * overscroll * overscroll) +
            (0.7 * sourcePosition * overscroll);
        return -(numerator / denominator);
    }

    float reverseMapEnd(float overscroll, float sourcePosition) {
        float numerator =
            (0.3 * overscroll * overscroll) -
            (0.3 * sourcePosition * overscroll * overscroll) +
            (1.3 * sourcePosition * overscroll) -
            overscroll -
            sourcePosition;
        float denominator =
            (0.7 * sourcePosition * overscroll * overscroll) -
            (0.7 * sourcePosition * overscroll) -
            (0.7 * overscroll * overscroll) +
            overscroll -
            1.0;
        return numerator / denominator;
    }

    float reverseStretch(float overscroll, float sourcePosition) {
        float distanceStretched = 1.0 / (1.0 + abs(overscroll));
        float distanceDiff = distanceStretched - 1.0;

        if (overscroll > 0.0) {
            float mappedPosition = reverseMapStart(overscroll, sourcePosition);
            if (mappedPosition <= 1.0) {
                return mappedPosition;
            } else if (mappedPosition >= distanceStretched) {
                return mappedPosition - distanceDiff;
            }
        }

        if (overscroll < 0.0) {
            float mappedPosition = reverseMapEnd(overscroll, sourcePosition);
            if (mappedPosition >= 0.0) {
                return mappedPosition;
            } else {
                return mappedPosition + distanceDiff;
            }
        }

        return sourcePosition;
    }

    float compensateAxis(float position, float amount, float origin, float extent) {
        if (amount == 0.0 || extent <= 0.0) {
            return position;
        }
        float normalized = (position - origin) / extent;
        return origin + reverseStretch(amount, normalized) * extent;
    }

    half4 main(float2 coord) {
        float2 position = cardOrigin + coord * cardScale;
        float2 samplePosition = float2(
            compensateAxis(position.x, stretchAmount.x, stretchOrigin.x, stretchSize.x),
            compensateAxis(position.y, stretchAmount.y, stretchOrigin.y, stretchSize.y)
        );
        return background.eval(inputOrigin + samplePosition * inputScale);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class StretchBackgroundRenderer {
    private val runtimeShader = RuntimeShader(STRETCH_BACKGROUND_SHADER)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = runtimeShader
    }
    private var inputBitmap: Bitmap? = null

    fun draw(
        scope: ContentDrawScope,
        bitmap: ImageBitmap,
        boundsInBackground: Rect,
        compensation: StretchBitmapCompensation,
    ) {
        val androidBitmap = bitmap.asAndroidBitmap()
        if (inputBitmap !== androidBitmap) {
            inputBitmap = androidBitmap
            runtimeShader.setInputShader(
                "background",
                BitmapShader(androidBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
            )
        }

        with(scope) {
            runtimeShader.setFloatUniform(
                "cardOrigin",
                boundsInBackground.left,
                boundsInBackground.top,
            )
            runtimeShader.setFloatUniform(
                "cardScale",
                boundsInBackground.width / size.width,
                boundsInBackground.height / size.height,
            )
            runtimeShader.setFloatUniform("inputOrigin", 0f, 0f)
            runtimeShader.setFloatUniform("inputScale", 1f, 1f)
            runtimeShader.setFloatUniform(
                "stretchAmount",
                compensation.horizontalAmount,
                compensation.verticalAmount,
            )
            runtimeShader.setFloatUniform(
                "stretchOrigin",
                compensation.horizontalOrigin,
                compensation.verticalOrigin,
            )
            runtimeShader.setFloatUniform(
                "stretchSize",
                compensation.horizontalSize,
                compensation.verticalSize,
            )
            drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}

private val stretchFallbackPaint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

private data class BitmapSourceInterval(
    val start: Int,
    val end: Int,
)

private fun bitmapSourceInterval(
    first: Float,
    second: Float,
    bitmapSize: Int,
): BitmapSourceInterval? {
    if (bitmapSize <= 0) return null

    val lower = minOf(first, second)
    val upper = maxOf(first, second)
    if (upper <= 0f) return BitmapSourceInterval(0, 1)
    if (lower >= bitmapSize) return BitmapSourceInterval(bitmapSize - 1, bitmapSize)

    val start = floor(lower.coerceAtLeast(0f)).toInt().coerceIn(0, bitmapSize - 1)
    val end = ceil(upper.coerceAtMost(bitmapSize.toFloat()))
        .toInt()
        .coerceIn(start + 1, bitmapSize)
    return BitmapSourceInterval(start, end)
}

private fun reverseStretchPosition(
    amount: Float,
    normalizedInput: Float,
): Float {
    if (amount == 0f) return normalizedInput

    val distanceStretched = 1f / (1f + abs(amount))
    val distanceDifference = distanceStretched - 1f

    if (amount > 0f) {
        val numerator =
            (-normalizedInput * amount * amount) -
                (2f * normalizedInput * amount) -
                normalizedInput
        val denominator =
            1f +
                (0.3f * amount) +
                (0.7f * normalizedInput * amount * amount) +
                (0.7f * normalizedInput * amount)
        val output = -(numerator / denominator)
        return if (output <= 1f) output else output - distanceDifference
    }

    val numerator =
        (0.3f * amount * amount) -
            (0.3f * normalizedInput * amount * amount) +
            (1.3f * normalizedInput * amount) -
            amount -
            normalizedInput
    val denominator =
        (0.7f * normalizedInput * amount * amount) -
            (0.7f * normalizedInput * amount) -
            (0.7f * amount * amount) +
            amount -
            1f
    val output = numerator / denominator
    return if (output >= 0f) output else output + distanceDifference
}

private fun compensateBackgroundPosition(
    position: Float,
    amount: Float,
    origin: Float,
    extent: Float,
): Float {
    if (amount == 0f || extent <= 0f) return position
    val normalized = (position - origin) / extent
    return origin + reverseStretchPosition(amount, normalized) * extent
}

private fun ContentDrawScope.drawStretchCompensatedBitmap(
    bitmap: ImageBitmap,
    boundsInBackground: Rect,
    compensation: StretchBitmapCompensation,
) {
    if (size.width <= 0f || size.height <= 0f) return

    // Android 12/12L have no RuntimeShader API. A fine mesh is visually continuous for the
    // already-blurred bitmap while preserving the same inverse mapping.
    val xCellCount = if (compensation.horizontalAmount == 0f) {
        1
    } else {
        ceil(size.width / 16f).toInt().coerceAtLeast(1)
    }
    val yCellCount = if (compensation.verticalAmount == 0f) {
        1
    } else {
        ceil(size.height / 16f).toInt().coerceAtLeast(1)
    }
    val androidBitmap = bitmap.asAndroidBitmap()
    val canvas = drawContext.canvas.nativeCanvas

    for (xIndex in 0 until xCellCount) {
        val destinationLeft = size.width * xIndex / xCellCount
        val destinationRight = size.width * (xIndex + 1) / xCellCount
        val backgroundLeft =
            boundsInBackground.left + boundsInBackground.width * destinationLeft / size.width
        val backgroundRight =
            boundsInBackground.left + boundsInBackground.width * destinationRight / size.width
        val sourceX = bitmapSourceInterval(
            first = compensateBackgroundPosition(
                position = backgroundLeft,
                amount = compensation.horizontalAmount,
                origin = compensation.horizontalOrigin,
                extent = compensation.horizontalSize,
            ),
            second = compensateBackgroundPosition(
                position = backgroundRight,
                amount = compensation.horizontalAmount,
                origin = compensation.horizontalOrigin,
                extent = compensation.horizontalSize,
            ),
            bitmapSize = bitmap.width,
        ) ?: continue

        for (yIndex in 0 until yCellCount) {
            val destinationTop = size.height * yIndex / yCellCount
            val destinationBottom = size.height * (yIndex + 1) / yCellCount
            val backgroundTop =
                boundsInBackground.top + boundsInBackground.height * destinationTop / size.height
            val backgroundBottom =
                boundsInBackground.top + boundsInBackground.height * destinationBottom / size.height
            val sourceY = bitmapSourceInterval(
                first = compensateBackgroundPosition(
                    position = backgroundTop,
                    amount = compensation.verticalAmount,
                    origin = compensation.verticalOrigin,
                    extent = compensation.verticalSize,
                ),
                second = compensateBackgroundPosition(
                    position = backgroundBottom,
                    amount = compensation.verticalAmount,
                    origin = compensation.verticalOrigin,
                    extent = compensation.verticalSize,
                ),
                bitmapSize = bitmap.height,
            ) ?: continue

            canvas.drawBitmap(
                androidBitmap,
                android.graphics.Rect(
                    sourceX.start,
                    sourceY.start,
                    sourceX.end,
                    sourceY.end,
                ),
                RectF(
                    destinationLeft,
                    destinationTop,
                    destinationRight,
                    destinationBottom,
                ),
                stretchFallbackPaint,
            )
        }
    }
}

private fun Offset.isUsable(): Boolean = x.isFinite() && y.isFinite()

private suspend fun Bitmap.extractSeedColor(
    maxColors: Int = 128,
    fallbackColorArgb: Int = -12417548,
): Int = withContext(Dispatchers.IO) {
    val scaledBitmap = this@extractSeedColor.scale(128, 128)

    val width = scaledBitmap.width
    val height = scaledBitmap.height
    val pixels = IntArray(width * height)
    scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val colorToCountMap: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)
    val sortedColors: List<Int> = Score.score(colorToCountMap, 10, fallbackColorArgb, true)

    if (scaledBitmap != this@extractSeedColor) {
        scaledBitmap.recycle()
    }

    sortedColors.firstOrNull() ?: fallbackColorArgb
}

private fun Bitmap.softwareFastBlur(radius: Int): Bitmap {
    if (radius < 1) return this

    val w = width
    val h = height
    val pix = IntArray(w * h)
    getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var p: Int
    var yp: Int
    var yi: Int
    val vmin = IntArray(w.coerceAtLeast(h))

    var divsum = (div + 1) shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    for (i in 0 until 256 * divsum) {
        dv[i] = i / divsum
    }

    var yw = 0
    yi = 0

    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int

    for (y in 0 until h) {
        bsum = 0
        gsum = 0
        rsum = 0
        boutsum = 0
        goutsum = 0
        routsum = 0
        binsum = 0
        ginsum = 0
        rinsum = 0
        for (i in -radius..radius) {
            p = pix[yi + wm.coerceAtMost(i.coerceAtLeast(0))]
            sir = stack[i + radius]
            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
        }
        stackpointer = radius

        for (x in 0 until w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (y == 0) vmin[x] = (x + radius + 1).coerceAtMost(wm)
            p = pix[yw + vmin[x]]

            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = p and 0x0000ff

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]

            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]

            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]

            yi++
        }
        yw += w
    }

    for (x in 0 until w) {
        bsum = 0
        gsum = 0
        rsum = 0
        boutsum = 0
        goutsum = 0
        routsum = 0
        binsum = 0
        ginsum = 0
        rinsum = 0
        yp = -radius * w
        for (i in -radius..radius) {
            yi = (yp.coerceAtLeast(0)) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) yp += w
        }
        yi = x
        stackpointer = radius
        for (y in 0 until h) {
            pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) vmin[y] = (y + r1).coerceAtMost(hm) * w
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
        }
    }

    val outputBitmap = createBitmap(w, h)
    outputBitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return outputBitmap
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Bitmap.blurBitmap(blurRadius: Float): Bitmap {
    val outputBitmap = createBitmap(width, height)
    val outputCanvas = Canvas(outputBitmap)

    if (outputCanvas.isHardwareAccelerated) {
        val renderNode = RenderNode("BlurEffectNode").apply {
            setPosition(0, 0, width, height)
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP,
                ),
            )
        }

        val recordingCanvas = renderNode.beginRecording()
        recordingCanvas.drawBitmap(this, 0f, 0f, null)
        renderNode.endRecording()

        outputCanvas.drawRenderNode(renderNode)
    } else {
        val radiusInt = blurRadius.toInt().coerceIn(1, 25)
        return this.softwareFastBlur(radiusInt)
    }

    return outputBitmap
}

@RequiresApi(Build.VERSION_CODES.S)
private suspend fun Bitmap.createBackgroundBlurImage(
    context: Context,
    viewportSize: IntSize,
    blurRadius: Float,
): ImageBitmap = withContext(Dispatchers.Default) {
    val cacheFile = backgroundBlurCacheFile(context)

    BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { cachedBitmap ->
        if (
            cachedBitmap.width == viewportSize.width &&
            cachedBitmap.height == viewportSize.height
        ) {
            return@withContext cachedBitmap.asImageBitmap()
        }

        cachedBitmap.recycle()
    }

    val blurSource = createBackgroundBlurSource(viewportSize)

    val blurredBitmap = try {
        blurSource.blurBitmap(blurRadius)
    } finally {
        if (blurSource !== this@createBackgroundBlurImage) {
            blurSource.recycle()
        }
    }

    saveBackgroundBlurCache(cacheFile, blurredBitmap)
    blurredBitmap.asImageBitmap()
}

private fun Bitmap.createBackgroundBlurSource(viewportSize: IntSize): Bitmap {
    val targetWidth = viewportSize.width
    val targetHeight = viewportSize.height

    if (
        targetWidth <= 0 ||
        targetHeight <= 0 ||
        (width == targetWidth && height == targetHeight)
    ) {
        return this
    }

    val scale = maxOf(
        targetWidth / width.toFloat(),
        targetHeight / height.toFloat(),
    )
    val scaledWidth = width * scale
    val scaledHeight = height * scale
    val left = (targetWidth - scaledWidth) / 2f
    val top = (targetHeight - scaledHeight) / 2f

    return createBitmap(targetWidth, targetHeight).also { outputBitmap ->
        Canvas(outputBitmap).drawBitmap(
            this,
            null,
            RectF(left, top, left + scaledWidth, top + scaledHeight),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    }
}

internal fun backgroundBlurCacheFile(context: Context): File = File(context.filesDir, "blured_custom_background.jpg")

internal fun legacyBackgroundBlurCacheDir(context: Context): File = File(context.filesDir, "background_blur_cache")

private fun saveBackgroundBlurCache(cacheFile: File, bitmap: Bitmap) {
    runCatching {
        cacheFile.parentFile?.mkdirs()
        val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
        FileOutputStream(tempFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        if (!tempFile.renameTo(cacheFile)) {
            tempFile.copyTo(cacheFile, overwrite = true)
            tempFile.delete()
        }
    }.onFailure {
        Log.w("ThemeSystem", "Failed to save background blur cache: ${it.message}")
    }
}


@Composable
internal fun BackgroundInitializer(
    uri: Uri,
    themeConfig: ThemeConfig,
    settings: AppSettingsRepository,
    renderState: BackgroundRenderState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dynamicColorFromSystem = themeConfig.monetCompatSeedColor

    val calcedCachedSeedColor =
        settings.getInt("cached_seed_color", dynamicColorFromSystem)

    LaunchedEffect(themeConfig.isEnableBlurExp, renderState.blurViewportSize) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            themeConfig.isEnableBlurExp &&
            renderState.blurViewportSize.width > 0 &&
            renderState.blurViewportSize.height > 0
        ) {
            renderState.imagePainter?.let {
                if (it.state !is AsyncImagePainter.State.Success) return@let

                val bitmap = (it.state as AsyncImagePainter.State.Success).result.drawable.toBitmap()
                renderState.blurImageBitmap = bitmap.createBackgroundBlurImage(
                    context = context,
                    viewportSize = renderState.blurViewportSize,
                    blurRadius = BACKGROUND_BLUR_RADIUS,
                )
            }
        } else {
            renderState.blurImageBitmap = null
        }
    }

    renderState.imagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .crossfade(true)
            .build(),
        onError = { error ->
            Log.e("ThemeSystem", "背景加载失败: ${error.result.throwable.message}")
            renderState.imageBitmap = null
            themeConfig.customBackgroundUri = null
        },
        onSuccess = {
            val bitmap = it.result.drawable.toBitmap()
            renderState.imageBitmap = bitmap.asImageBitmap()
            Log.d("ThemeSystem", "背景加载成功")
            themeConfig.backgroundImageLoaded = true
            themeConfig.isThemeChanging = false

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                themeConfig.isEnableBlurExp &&
                renderState.blurViewportSize.width > 0 &&
                renderState.blurViewportSize.height > 0
            ) {
                coroutineScope.launch {
                    renderState.blurImageBitmap = bitmap.createBackgroundBlurImage(
                        context = context,
                        viewportSize = renderState.blurViewportSize,
                        blurRadius = BACKGROUND_BLUR_RADIUS,
                    )
                }
            } else {
                renderState.blurImageBitmap = null
            }

            renderState.seedColor = calcedCachedSeedColor
            coroutineScope.launch {
                renderState.seedColor = bitmap.extractSeedColor(
                    fallbackColorArgb = calcedCachedSeedColor,
                )

                settings.putInt("cached_seed_color", renderState.seedColor)
            }
        },
    )
}

