package com.huijui.nova.wallpaper

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import java.io.File

/** 本地媒体壁纸的类型。 */
enum class HuiMediaKind(val label: String) {
    Image("图片"),
    Gif("动图 GIF"),
    Video("视频 MP4"),
    ;

    companion object {
        fun fromMime(mime: String?): HuiMediaKind {
            val normalized = mime?.lowercase() ?: return Image
            return when {
                normalized.contains("gif") -> Gif
                normalized.startsWith("video/") -> Video
                else -> Image
            }
        }

        fun fromPath(path: String): HuiMediaKind = when {
            path.endsWith(".gif", true) -> Gif
            path.endsWith(".mp4", true) || path.endsWith(".webm", true) -> Video
            else -> Image
        }
    }
}

/** 本地媒体壁纸的描述。uri 用字符串存，方便进 SharedPreferences。 */
data class HuiMediaSpec(
    val uri: String,
    val kind: HuiMediaKind,
    /**
     * 低分辨率预览（可选）。视频壁纸铺满全屏时应用内要放大好几倍，
     * 用原始视频在弱 GPU 上会卡死——留一份小的专门给应用内背景用。
     */
    val previewUri: String? = null,
)

/**
 * 图片 / GIF 壁纸。
 *
 * 关键点是「画进 Compose 的画布」：这样它会被 [HuiBackdropHost] 的 GraphicsLayer
 * 录制下来，玻璃卡片照样能采样——图片和动图壁纸不需要牺牲玻璃效果。
 *
 * GIF 用 ImageDecoder 解成 AnimatedImageDrawable，通过它的回调驱动重绘；
 * 只在前台可见时播放，退后台立刻 stop。
 */
@Composable
fun HuiDrawableWallpaper(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable: Drawable? = remember(uri) {
        runCatching { decodeMediaDrawable(context, Uri.parse(uri)) }.getOrNull()
    }

    var tick by remember(uri) { mutableIntStateOf(0) }

    DisposableEffect(drawable) {
        if (drawable is Animatable) drawable.start()
        val previous = drawable?.callback
        drawable?.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                // 只推进重绘计数，不触发重组
                tick++
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, whenTime: Long) {
                previous?.scheduleDrawable(who, what, whenTime)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                previous?.unscheduleDrawable(who, what)
            }
        }
        onDispose {
            if (drawable is Animatable) runCatching { drawable.stop() }
            drawable?.callback = previous
        }
    }

    Canvas(modifier) {
        val d = drawable ?: return@Canvas
        // 读一下 tick：GIF 每帧推进时让画布失效
        @Suppress("UNUSED_EXPRESSION")
        tick

        val iw = d.intrinsicWidth.takeIf { it > 0 } ?: size.width.toInt()
        val ih = d.intrinsicHeight.takeIf { it > 0 } ?: size.height.toInt()
        val scale = maxOf(size.width / iw, size.height / ih)
        val w = iw * scale
        val h = ih * scale
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        drawIntoCanvas { canvas ->
            d.setBounds(left.toInt(), top.toInt(), (left + w).toInt(), (top + h).toInt())
            d.draw(canvas.nativeCanvas)
        }
    }
}

/**
 * 视频壁纸。
 *
 * 视频走 TextureView（系统合成层），没法被 Compose 的 GraphicsLayer 录进去，
 * 所以视频壁纸下玻璃卡片会自动退化成「纯色遮罩」——[HuiBackdropHost] 不提供
 * 背景图层时 [com.huijui.nova.material.HuiSurface] 本来就会走这个分支。
 */
@Composable
fun HuiVideoWallpaper(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val owner = LocalLifecycleOwner.current
    var view by remember { mutableStateOf<VideoView?>(null) }

    // 退到后台就暂停解码：视频壁纸不暂停的话，息屏后还在持续吃电
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> runCatching { view?.pause() }
                Lifecycle.Event.ON_RESUME -> runCatching { view?.start() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // 竖屏视频按比例会留上下黑边，放大 1.3 倍裁切铺满
    Box(modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = 1.26f; scaleY = 1.26f },
            factory = { ctx ->
                VideoView(ctx).apply {
                    setMediaController(null as MediaController?)
                    setVideoURI(Uri.parse(uri))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                    view = this
                }
            },
            onRelease = { released ->
                runCatching { released.stopPlayback() }
                if (view === released) view = null
            },
        )
    }
}

/** 给选择器用的首帧缩略图：视频用 MediaMetadataRetriever，图片走 ImageDecoder 的静态解码。 */
fun mediaDisplayName(uri: String): String = runCatching {
    val parsed = Uri.parse(uri)
    parsed.lastPathSegment?.substringAfterLast('/') ?: File(parsed.path ?: "").name
}.getOrNull().orEmpty().ifBlank { "本地媒体" }


/**
 * 统一解码入口：API 28+ 用 ImageDecoder（会自动拿到 AnimatedImageDrawable，GIF 能播），
 * 28 以下退回 BitmapFactory —— GIF 会退化成首帧，但不会崩。
 */
/** 解码上限：够铺满 1440p 屏幕，又不会把 4K 原图解进内存。 */
private const val MAX_DECODE_EDGE = 1440

fun decodeMediaDrawable(context: android.content.Context, uri: Uri): Drawable? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // 大图先降采样再解码：4K 照片直接整张解出来会白白吃掉几十 MB 内存
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_DECODE_EDGE) {
                decoder.setTargetSampleSize(
                    kotlin.math.ceil(longest / MAX_DECODE_EDGE.toDouble()).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let { bmp -> BitmapDrawable(context.resources, bmp) }
        }
    }
}
