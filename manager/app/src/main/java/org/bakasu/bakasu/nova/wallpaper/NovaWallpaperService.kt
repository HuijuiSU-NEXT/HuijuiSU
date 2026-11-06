package org.bakasu.bakasu.nova.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.huijui.nova.wallpaper.HuiMediaKind
import com.huijui.nova.wallpaper.HuiWallpaper
import com.huijui.nova.wallpaper.decodeMediaDrawable
import org.bakasu.bakasu.nova.NovaWallpaperStore
import kotlin.math.cos
import kotlin.math.sin

/**
 * 系统级动态壁纸。
 *
 * 三种渲染通道，都复用 Nova 的配色：
 * - 程序化极光：android.graphics 直接画渐变；
 * - 图片 / GIF：ImageDecoder 解成 Drawable 逐帧画到 Surface；
 * - MP4 / WebM：MediaPlayer 直接输出到壁纸 Surface（系统负责解码与合成，最省电）。
 *
 * 省电约束：
 * 1. 只有 `onVisibilityChanged(true)` 才启动，桌面不可见立刻停；
 * 2. 程序化/图片通道锁 30fps，视频交给系统解码器；
 * 3. 不在每帧分配位图或 Paint。
 */
class NovaWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = NovaEngine()

    private inner class NovaEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val selection: NovaWallpaperStore.Selection by lazy {
            NovaWallpaperStore.load(this@NovaWallpaperService)
        }

        private var visible = false
        private var startNanos = 0L
        private var drawable: Drawable? = null
        private var player: MediaPlayer? = null

        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        private val drawablePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val frame = object : Runnable {
            override fun run() {
                if (!visible) return
                if (isVideo()) {
                    // 视频由 MediaPlayer 输出，不需要 CPU 逐帧绘制
                    return
                }
                drawFrame()
                handler.postDelayed(this, FRAME_DELAY_MS)
            }
        }

        private fun isVideo() = selection.media?.kind == HuiMediaKind.Video

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(frame)
            if (isVisible) {
                if (startNanos == 0L) startNanos = System.nanoTime()
                ensureDrawable()
                if (isVideo()) startVideo() else handler.post(frame)
            } else {
                stopVideo()
                (drawable as? Animatable)?.let { runCatching { it.stop() } }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            stopVideo()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            stopVideo()
            drawable = null
            super.onDestroy()
        }

        /* ---------------- 视频 ---------------- */

        private fun startVideo() {
            if (player != null) return
            val uri = selection.media?.uri ?: return
            player = runCatching {
                MediaPlayer().apply {
                    setDataSource(this@NovaWallpaperService, Uri.parse(uri))
                    setSurface(surfaceHolder.surface)
                    isLooping = true
                    setVolume(0f, 0f)
                    setOnPreparedListener { it.start() }
                    prepareAsync()
                }
            }.getOrNull()
        }

        private fun stopVideo() {
            player?.let { mp ->
                runCatching { mp.stop() }
                runCatching { mp.release() }
            }
            player = null
        }

        /* ---------------- 图片 / GIF ---------------- */

        private fun ensureDrawable() {
            if (drawable != null) return
            val media = selection.media ?: return
            if (media.kind == HuiMediaKind.Video) return
            drawable = runCatching {
                decodeMediaDrawable(this@NovaWallpaperService, Uri.parse(media.uri))
            }.getOrNull()
            (drawable as? Animatable)?.let { runCatching { it.start() } }
        }

        /* ---------------- 逐帧绘制 ---------------- */

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val media = selection.media
                    when {
                        media != null && media.kind != HuiMediaKind.Video -> drawMedia(canvas)
                        else -> drawAurora(canvas)
                    }
                }
            } catch (_: Throwable) {
                // 表面失效时静默退出，不能把系统桌面搞崩
            } finally {
                if (canvas != null) runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }

        private fun drawMedia(canvas: Canvas) {
            val d = drawable
            canvas.drawColor(Color.BLACK)
            if (d == null) return
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val iw = d.intrinsicWidth.takeIf { it > 0 } ?: width.toInt()
            val ih = d.intrinsicHeight.takeIf { it > 0 } ?: height.toInt()
            val scale = maxOf(width / iw, height / ih)
            val w = iw * scale
            val h = ih * scale
            d.setBounds(
                ((width - w) / 2f).toInt(),
                ((height - h) / 2f).toInt(),
                ((width + w) / 2f).toInt(),
                ((height + h) / 2f).toInt(),
            )
            d.draw(canvas)
        }

        private fun drawAurora(canvas: Canvas) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            if (width <= 0f || height <= 0f) return

            val seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
            val miku = selection.preset == HuiWallpaper.Miku
            val deep = if (miku) MIKU_DEEP else BG_DEEP
            val mid = if (miku) MIKU_MID else BG
            val c1 = if (miku) MIKU_TEAL else ACCENT
            val c2 = if (miku) MIKU_PINK else ACCENT_ALT
            val c3 = if (miku) MIKU_CYAN else ACCENT_GLOW

            backgroundPaint.shader = LinearGradient(
                0f,
                0f,
                0f,
                height,
                intArrayOf(deep, mid, deep),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(Rect(0, 0, width.toInt(), height.toInt()), backgroundPaint)

            glow(
                canvas,
                width * (0.16f + 0.05f * sin(seconds * 0.12).toFloat()),
                height * (0.10f + 0.03f * cos(seconds * 0.09).toFloat()),
                width * 0.95f,
                c1,
                0.34f,
            )
            glow(
                canvas,
                width * (0.92f + 0.04f * cos(seconds * 0.10).toFloat()),
                height * (0.34f + 0.04f * sin(seconds * 0.07).toFloat()),
                width * 0.80f,
                c2,
                0.26f,
            )
            glow(
                canvas,
                width * (0.42f + 0.06f * sin(seconds * 0.14).toFloat()),
                height * 0.92f,
                width * 0.90f,
                c3,
                0.18f,
            )
        }

        private fun glow(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Float) {
            blobPaint.shader = RadialGradient(
                cx,
                cy,
                radius.coerceAtLeast(1f),
                intArrayOf(
                    withAlpha(color, alpha),
                    withAlpha(color, alpha * 0.35f),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, blobPaint)
        }

        private fun withAlpha(color: Int, alpha: Float): Int =
            Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private companion object {
        const val FRAME_DELAY_MS = 33L
        val BG_DEEP = Color.parseColor("#030305")
        val BG = Color.parseColor("#07070B")
        val ACCENT = Color.parseColor("#9C8CFF")
        val ACCENT_ALT = Color.parseColor("#57E0FF")
        val ACCENT_GLOW = Color.parseColor("#B9A8FF")
        val MIKU_DEEP = Color.parseColor("#04191B")
        val MIKU_MID = Color.parseColor("#07282B")
        val MIKU_TEAL = Color.parseColor("#39C5BB")
        val MIKU_PINK = Color.parseColor("#E12885")
        val MIKU_CYAN = Color.parseColor("#7DF3EA")
    }
}
