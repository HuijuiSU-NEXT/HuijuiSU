package com.huijui.nova.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import com.huijui.nova.wallpaper.HuiDrawableWallpaper
import com.huijui.nova.wallpaper.HuiMediaKind
import com.huijui.nova.wallpaper.HuiMediaSpec
import com.huijui.nova.wallpaper.HuiVideoWallpaper
import com.huijui.nova.wallpaper.HuiWallpaper
import com.huijui.nova.wallpaper.HuiWallpaperCanvas

/**
 * 背景层。玻璃材质需要「可被采样的背景」才有意义，所以 Nova 的背景不是
 * 一张静态图，而是可被记录成 GraphicsLayer 的壁纸画布。
 */
val LocalHuiBackdropLayer = staticCompositionLocalOf<GraphicsLayer?> { null }

/**
 * 背景宿主：把壁纸绘制记进一个 GraphicsLayer，玻璃卡片再从这个 layer 里
 * 取「背后那一块」去模糊/折射。这样玻璃才是真的玻璃，而不是贴一层半透明白。
 *
 * [wallpaper] 支持五种程序化壁纸（极光/流体/流线/星尘/纯色），切换时只换
 * 画布内容，所有卡片不用改一行。
 */
@Composable
fun HuiBackdropHost(
    modifier: Modifier = Modifier,
    wallpaper: HuiWallpaper = HuiWallpaper.Aurora,
    media: HuiMediaSpec? = null,
    /** 自定义背景（不传则用壁纸）。玻璃卡片采样的就是这一层。 */
    background: (@Composable () -> Unit)? = null,
    /** 壁纸上方的一层压暗（0..1）。视频/亮图壁纸必须靠它保证可读性。 */
    scrim: Float = 0.06f,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val isVideo = media != null && media.kind == HuiMediaKind.Video

    Box(modifier = modifier.fillMaxSize()) {
        // 视频交给系统壁纸层（NovaWallpaperService）：满屏、且不在应用进程里解码，
        // 不会和 Compose 抢 CPU。窗口内再放一个 VideoView 会导致滑动掉帧甚至 ANR。
        if (isVideo && media != null) {
            HuiVideoWallpaper(media.uri, Modifier.fillMaxSize())
        }

        Box(
            modifier = if (isVideo) {
                // 视频模式下玻璃采不到背景，录制整棵树只是白烧 CPU
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        layer.record(
                            density = this,
                            layoutDirection = layoutDirection,
                            size = IntSize(size.width.toInt(), size.height.toInt()),
                        ) {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(layer)
                    }
            },
        ) {
            when {
                background != null -> background()
                // 视频已经在上面的顶层画过了，这里不再画（避免两个 VideoView）
                isVideo -> Unit
                media != null -> HuiDrawableWallpaper(media.uri, Modifier.fillMaxSize())
                else -> HuiWallpaperCanvas(wallpaper, Modifier.fillMaxSize())
            }
        }

        // 压暗层：视频要在录制层外面，否则会被录成黑色
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color(0xFF05070D).copy(alpha = scrim * 0.75f),
                            Color(0xFF05070D).copy(alpha = scrim),
                            Color(0xFF05070D).copy(alpha = scrim * 2.4f),
                        ),
                    ),
                ),
        )

        CompositionLocalProvider(LocalHuiBackdropLayer provides if (isVideo) null else layer) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}
