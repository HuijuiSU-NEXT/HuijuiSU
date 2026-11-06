package com.huijui.nova.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.huijui.nova.design.HuiColors
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.motion.LocalHuiAnimationsActive
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * 动态壁纸预设。
 *
 * 全部是程序化绘制（不含视频解码），代价可控、可随时暂停；
 * 玻璃卡片采样的是同一块画布，换壁纸不需要改任何卡片代码。
 */
enum class HuiWallpaper(
    val label: String,
    val description: String,
    /** 播放帧率上限，越低越省电；纯色为 0（不播）。 */
    val fps: Int,
) {
    Miku("初音", "双马尾流光 · 青绿与洋红的极光底", 12),
    Aurora("极光", "三团高饱和光斑缓慢漂移", 4),
    Mesh("流体渐变", "多层渐变叠加，缓慢呼吸变形", 8),
    Flow("流线", "上百条流线沿正弦场漂移", 12),
    Particle("星尘", "细密粒子缓慢上升，带视差", 10),
    Solid("纯色", "零动画的静态底，最省电", 0),
    ;

    val animated: Boolean get() = fps > 0

    companion object {
        /** 默认壁纸：初音配色。可以用 media 覆盖成本地图片/GIF/视频。 */
        val Default: HuiWallpaper = Miku
    }
}

/**
 * 壁纸相位。所有预设共用同一个低频状态：
 * 每帧只更新一个 Float，绘制只在 Canvas 失效时发生，不会触发重组。
 */
@Composable
private fun rememberWallpaperPhase(wallpaper: HuiWallpaper): androidx.compose.runtime.State<Float> {
    val active = LocalHuiAnimationsActive.current && wallpaper.animated
    val rememberUpdatedPhase = remember(wallpaper) { mutableFloatStateOf(0f) }
    val phase = rememberUpdatedPhase
    LaunchedEffect(active, wallpaper) {
        if (!active) {
            phase.floatValue = 0f
            return@LaunchedEffect
        }
        val start = System.nanoTime()
        val intervalMs = (1000 / wallpaper.fps).coerceAtLeast(16)
        while (true) {
            phase.floatValue = ((System.nanoTime() - start) / 1_000_000_000.0).toFloat()
            delay(intervalMs.toLong())
        }
    }
    return rememberUpdatedPhase
}

@Composable
fun HuiWallpaperCanvas(
    wallpaper: HuiWallpaper,
    modifier: Modifier = Modifier,
    colors: HuiColors = LocalHuiColors.current,
) {
    // 注意：这里拿的是 State，真正的读取发生在 Canvas 的绘制 lambda 里。
    // 直接把 Float 读进组合会让壁纸每帧触发一次重组（12fps 就是每秒 12 次整屏重组）。
    val phase = rememberWallpaperPhase(wallpaper)
    Canvas(modifier) {
        drawWallpaper(wallpaper, colors, phase.value)
    }
}

/** 给壁纸选择器用的小预览：固定相位，不跑动画。 */
@Composable
fun HuiWallpaperPreview(
    wallpaper: HuiWallpaper,
    modifier: Modifier = Modifier,
    colors: HuiColors = LocalHuiColors.current,
) {
    Canvas(modifier) {
        drawWallpaper(wallpaper, colors, previewPhase(wallpaper))
    }
}

private fun previewPhase(wallpaper: HuiWallpaper): Float = when (wallpaper) {
    HuiWallpaper.Miku -> 2.1f
    HuiWallpaper.Aurora -> 0.4f
    HuiWallpaper.Mesh -> 0.8f
    HuiWallpaper.Flow -> 1.2f
    HuiWallpaper.Particle -> 2.4f
    HuiWallpaper.Solid -> 0f
}

private fun DrawScope.drawWallpaper(wallpaper: HuiWallpaper, colors: HuiColors, phase: Float) {
    when (wallpaper) {
        HuiWallpaper.Miku -> drawMiku(phase)
        HuiWallpaper.Aurora -> drawAurora(colors, phase)
        HuiWallpaper.Mesh -> drawMesh(colors, phase)
        HuiWallpaper.Flow -> drawFlow(colors, phase)
        HuiWallpaper.Particle -> drawParticles(colors, phase)
        HuiWallpaper.Solid -> drawRect(colors.bgDeep)
    }
}

/* ------------------------------------------------------------------ */
/*  初音配色：青绿 #39C5BB / 洋红 #E12885，原创程序化绘制                */
/* ------------------------------------------------------------------ */

private val MIKU_TEAL = Color(0xFF39C5BB)
private val MIKU_CYAN = Color(0xFF7DF3EA)
private val MIKU_PINK = Color(0xFFE12885)
private val MIKU_DEEP = Color(0xFF04191B)
private val MIKU_MID = Color(0xFF07282B)
private val ribbonPath = Path()

private fun DrawScope.drawMiku(phase: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(MIKU_DEEP, MIKU_MID, MIKU_DEEP)))

    // 两道「双马尾」流光：左右镜像的正弦长带，缓慢摆动
    for (side in intArrayOf(-1, 1)) {
        ribbonPath.reset()
        val baseX = w * (0.5f + side * 0.30f)
        ribbonPath.moveTo(baseX, -h * 0.06f)
        var t = 0f
        while (t <= 1f) {
            val y = h * t
            val sway = sin(phase * 0.55f + t * 4.2f + side) * w * 0.06f * (0.3f + t)
            val pinch = w * 0.02f * sin(t * 7f + phase * 0.3f)
            ribbonPath.lineTo(baseX + sway + side * pinch, y)
            t += 0.06f
        }
        drawPath(
            path = ribbonPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    MIKU_CYAN.copy(alpha = 0.45f),
                    MIKU_TEAL.copy(alpha = 0.30f),
                    MIKU_PINK.copy(alpha = 0.22f),
                ),
                start = Offset(baseX, 0f),
                end = Offset(baseX, h),
            ),
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round),
            blendMode = BlendMode.Plus,
        )
    }

    // 顶部青绿光 + 底部洋红光，把双马尾「托」起来
    blob(w * (0.30f + 0.03f * sin(phase * 0.2f)), h * 0.12f, w * 0.75f, MIKU_TEAL.copy(alpha = 0.26f))
    blob(w * (0.72f + 0.03f * cos(phase * 0.18f)), h * 0.86f, w * 0.70f, MIKU_PINK.copy(alpha = 0.18f))

    // 稀疏音符状光点（纯几何，不画具体形象）
    repeat(18) { index ->
        val hx = ((index * 67) % 89) / 89f
        val depth = 0.3f + ((index * 31) % 11) / 11f * 0.7f
        val y = ((0.18f + hx * 0.7f + phase * 0.02f * depth) % 1f)
        val x = hx + sin(phase * 0.25f + index) * 0.015f
        drawCircle(
            color = if (index % 3 == 0) MIKU_PINK.copy(alpha = 0.45f) else MIKU_CYAN.copy(alpha = 0.35f),
            radius = (0.9f + depth * 1.8f).dp.toPx(),
            center = Offset(w * x, h * y),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  其它预设                                                           */
/* ------------------------------------------------------------------ */

private val meshCorners = arrayOf(
    floatArrayOf(0.1f, 0.08f),
    floatArrayOf(0.9f, 0.2f),
    floatArrayOf(0.15f, 0.9f),
    floatArrayOf(0.85f, 0.85f),
)

private fun DrawScope.baseGradient(colors: HuiColors) {
    drawRect(Brush.verticalGradient(listOf(colors.bgDeep, colors.bg, colors.bgDeep)))
}

private fun DrawScope.blob(cx: Float, cy: Float, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = color.alpha * 0.35f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius,
        ),
        radius = radius,
        center = Offset(cx, cy),
        blendMode = BlendMode.Plus,
    )
}

private fun DrawScope.drawAurora(colors: HuiColors, phase: Float) {
    baseGradient(colors)
    val w = size.width
    val h = size.height
    blob(
        w * (0.16f + 0.05f * sin(phase * 0.12f)),
        h * (0.10f + 0.03f * cos(phase * 0.09f)),
        w * 0.95f,
        colors.accent.copy(alpha = 0.34f),
    )
    blob(
        w * (0.92f + 0.04f * cos(phase * 0.10f)),
        h * (0.34f + 0.04f * sin(phase * 0.07f)),
        w * 0.80f,
        colors.accentAlt.copy(alpha = 0.26f),
    )
    blob(
        w * (0.42f + 0.06f * sin(phase * 0.14f)),
        h * 0.92f,
        w * 0.90f,
        colors.accentGlow.copy(alpha = 0.18f),
    )
}

private fun DrawScope.drawMesh(colors: HuiColors, phase: Float) {
    baseGradient(colors)
    val w = size.width
    val h = size.height
    val palette = arrayOf(colors.accent, colors.accentAlt, colors.accentGlow, colors.accent)
    meshCorners.forEachIndexed { index, corner ->
        val wobble = sin(phase * (0.10f + index * 0.03f) + index) * 0.06f
        blob(
            cx = w * (corner[0] + wobble),
            cy = h * (corner[1] - wobble),
            radius = w * (0.75f + 0.08f * cos(phase * 0.08f + index)),
            color = palette[index].copy(alpha = 0.18f),
        )
    }
}

private fun DrawScope.drawFlow(colors: HuiColors, phase: Float) {
    baseGradient(colors)
    val w = size.width
    val h = size.height
    val lines = 46
    repeat(lines) { index ->
        val t = index / lines.toFloat()
        val y0 = h * t + sin(phase * 0.35f + index * 0.6f) * h * 0.035f
        val y1 = y0 + sin(phase * 0.22f + index * 0.9f) * h * 0.05f
        val alpha = 0.05f + 0.10f * (0.5f + 0.5f * sin(index * 1.7f + phase * 0.5f))
        drawLine(
            color = if (index % 3 == 0) colors.accentAlt.copy(alpha = alpha) else colors.accent.copy(alpha = alpha * 0.8f),
            start = Offset(0f, y0),
            end = Offset(w, y1),
            strokeWidth = (0.6f + (index % 4) * 0.35f).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    blob(w * 0.5f, -h * 0.05f, w * 0.8f, colors.accent.copy(alpha = 0.22f))
    blob(w * 0.5f, h * 1.05f, w * 0.8f, colors.accentAlt.copy(alpha = 0.18f))
}

private val particleHashes = FloatArray(90 * 2).also { arr ->
    for (i in 0 until 90) {
        arr[i * 2] = ((i * 73) % 97) / 97f
        arr[i * 2 + 1] = ((i * 41) % 89) / 89f
    }
}

private fun DrawScope.drawParticles(colors: HuiColors, phase: Float) {
    baseGradient(colors)
    val w = size.width
    val h = size.height
    repeat(90) { index ->
        val hx = particleHashes[index * 2]
        val hy = particleHashes[index * 2 + 1]
        val depth = 0.35f + ((index * 29) % 13) / 13f * 0.65f
        val speed = 0.012f + depth * 0.02f
        val y = ((hy - phase * speed) % 1f + 1f) % 1f
        val x = (hx + sin(phase * 0.25f + index) * 0.02f * depth + 1f) % 1f
        val color = if (index % 5 == 0) colors.accentAlt else colors.accent
        drawCircle(
            color = color.copy(alpha = 0.10f + depth * 0.28f),
            radius = (0.8f + depth * 2.2f).dp.toPx(),
            center = Offset(w * x, h * y),
        )
    }
    blob(w * 0.2f, h * 0.15f, w * 0.7f, colors.accent.copy(alpha = 0.16f))
    blob(w * 0.85f, h * 0.8f, w * 0.6f, colors.accentAlt.copy(alpha = 0.12f))
}
