package com.huijui.nova.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huijui.nova.design.HuiMotionLevel
import com.huijui.nova.design.LocalHuiMotionLevel
import kotlin.math.PI
import kotlin.math.sin

/**
 * Nova 运动系统。
 *
 * 「上千种动画」不是手写一千个 Composable，而是把动画拆成三个可组合的维度：
 * 8 个入场族 × 8 条缓动曲线 × 16 档时长 = 1024 个可寻址的动画变体，
 * 再加上每个变体由 id 决定的位移/缩放/旋转幅度，实际观感远多于 1024 种。
 *
 * 所有变体都通过同一个 [huiEnter] 修饰符执行，代价是一次 {@code animateFloatAsState}，
 * 不会为每种动画单独建树。时长还会被全局动效强度 [HuiMotionLevel] 再缩放一次，
 * 所以「克制/舒展/张扬」是整棵界面统一换速度，而不是每个页面各写一套参数。
 */
enum class HuiEase(val label: String, val curve: Easing) {
    Linear("线性", LinearEasing),
    Standard("标准", CubicBezierEasing(0.2f, 0f, 0f, 1f)),
    Emphasized("强调", CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)),
    Decelerate("减速", CubicBezierEasing(0f, 0f, 0.2f, 1f)),
    Accelerate("加速", CubicBezierEasing(0.4f, 0f, 1f, 1f)),
    Overshoot("过冲", CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
    Anticipate("预备", CubicBezierEasing(0.36f, 0f, 0.66f, -0.56f)),
    Springy("弹性", CubicBezierEasing(0.18f, 1.32f, 0.4f, 1f)),
}

/** 入场族：决定几何属性怎么变。 */
enum class HuiEntrance(val label: String) {
    Fade("淡入"),
    Rise("上浮"),
    Fall("下沉"),
    SlideLeft("左滑入"),
    SlideRight("右滑入"),
    Scale("缩放"),
    Rotate("旋转"),
    Flip("翻转"),
}

@Immutable
data class HuiMotionSpec(
    val id: Int,
    val entrance: HuiEntrance,
    val ease: HuiEase,
    val durationMs: Int,
    /** 位移幅度，dp。 */
    val distanceDp: Float,
    /** 起始缩放。 */
    val scaleFrom: Float,
    /** 起始旋转角度。 */
    val rotateFrom: Float,
    /** 起始 Y 轴翻转角度。 */
    val flipFrom: Float,
) {
    val label: String get() = "${entrance.label}·${ease.label}·${durationMs}ms"

    /** 供画廊/调试显示。 */
    val code: String get() = "#%04d".format(id)

    /** 换一个全局节奏：克制 0.65×，张扬 1.45×。 */
    fun scaled(level: HuiMotionLevel): HuiMotionSpec =
        copy(durationMs = (durationMs * level.scale).toInt().coerceIn(40, 2000))
}

object HuiMotionCatalog {

    private const val DURATION_STEPS = 16
    private const val BASE_DURATION = 110
    private const val DURATION_STEP = 55

    /** 1024 个可寻址动画变体。 */
    val all: List<HuiMotionSpec> by lazy {
        buildList {
            HuiEntrance.entries.forEachIndexed { entranceIndex, entrance ->
                HuiEase.entries.forEachIndexed { easeIndex, ease ->
                    repeat(DURATION_STEPS) { stepIndex ->
                        val id = entranceIndex * (HuiEase.entries.size * DURATION_STEPS) +
                            easeIndex * DURATION_STEPS + stepIndex
                        val wobble = ((id * 37) % 17) / 17f
                        add(
                            HuiMotionSpec(
                                id = id,
                                entrance = entrance,
                                ease = ease,
                                durationMs = BASE_DURATION + stepIndex * DURATION_STEP,
                                distanceDp = 10f + wobble * 42f,
                                scaleFrom = 0.80f + wobble * 0.14f,
                                rotateFrom = (wobble * 2f - 1f) * 14f,
                                flipFrom = (wobble * 2f - 1f) * 62f,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun byFamily(entrance: HuiEntrance): List<HuiMotionSpec> = all.filter { it.entrance == entrance }

    fun byEase(ease: HuiEase): List<HuiMotionSpec> = all.filter { it.ease == ease }

    fun get(id: Int): HuiMotionSpec = all[id.mod(all.size)]

    /** 稳定取样：同一个 seed 永远拿到同一批。 */
    fun sample(count: Int, seed: Int = 1): List<HuiMotionSpec> {
        if (count >= all.size) return all
        val step = all.size / count
        return (0 until count).map { all[((it * step) + seed * 31).mod(all.size)] }
    }

    /** 由字符串（比如卡片 key）稳定地挑一个动画，同一张卡片每次入场都一致。 */
    fun forKey(key: String): HuiMotionSpec = get(key.hashCode())

    val size: Int get() = all.size
}

private fun Int.mod(n: Int): Int = ((this % n) + n) % n

/**
 * 按 [spec] 播放入场。可见性变化时重放，[visible] 为 false 时回到起始姿态。
 * 时长会乘以当前 [HuiMotionLevel] 的倍率，保证全局节奏一致。
 */
@Composable
fun Modifier.huiEnter(
    spec: HuiMotionSpec,
    visible: Boolean = true,
    delayMs: Int = 0,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val density = LocalDensity.current
    val level = LocalHuiMotionLevel.current
    val duration = (spec.durationMs * level.scale).toInt().coerceAtLeast(40)
    val delay = (delayMs * level.scale).toInt().coerceAtLeast(0)

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = delay,
            easing = spec.ease.curve,
        ),
        label = "huiEnter-${spec.id}",
    )
    val distancePx = with(density) { spec.distanceDp.dp.toPx() }
    val camera = with(density) { 24f * density.density }
    return this.graphicsLayer {
        val p = progress
        val left = 1f - p
        alpha = p.coerceIn(0f, 1f)
        scaleX = spec.scaleFrom + (1f - spec.scaleFrom) * p
        scaleY = spec.scaleFrom + (1f - spec.scaleFrom) * p
        rotationZ = when (spec.entrance) {
            HuiEntrance.Rotate -> spec.rotateFrom * left
            else -> 0f
        }
        rotationY = when (spec.entrance) {
            HuiEntrance.Flip -> spec.flipFrom * left
            else -> 0f
        }
        cameraDistance = camera
        translationX = when (spec.entrance) {
            HuiEntrance.SlideLeft -> distancePx * left
            HuiEntrance.SlideRight -> -distancePx * left
            else -> 0f
        }
        translationY = when (spec.entrance) {
            HuiEntrance.Rise -> distancePx * left
            HuiEntrance.Fall -> -distancePx * left
            else -> 0f
        }
    }
}

/**
 * 列表错峰入场的延迟。集中在一处算，避免每个页面各写一个 `index * 50`。
 */
fun huiStaggerDelay(index: Int, level: HuiMotionLevel, stepMs: Int = 42): Int =
    (index.coerceAtLeast(0) * stepMs * level.scale).toInt()

/** 按下回弹。所有可点区域都应该有，这是这套界面「有重量」的来源。 */
@Composable
fun Modifier.huiPress(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.965f,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) 90 else 260,
            easing = if (pressed) HuiEase.Accelerate.curve else HuiEase.Springy.curve,
        ),
        label = "huiPress",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 无限呼吸：给强调元素用。
 * 宿主不在前台时直接退化成静态图层——不在后台空转，这是省电的关键。
 */
@Composable
fun Modifier.huiBreathe(
    min: Float = 0.55f,
    max: Float = 1f,
    periodMs: Int = 2600,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    if (!LocalHuiAnimationsActive.current) {
        return this.graphicsLayer { alpha = max }
    }
    val transition = rememberInfiniteTransition(label = "breathe")
    val value by transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = HuiEase.Standard.curve),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breatheValue",
    )
    return this.graphicsLayer { alpha = value }
}

/** 缓慢旋转，用于光环类装饰。同样在后台停止。 */
@Composable
fun Modifier.huiSpin(periodMs: Int = 14000, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    if (!LocalHuiAnimationsActive.current) {
        return this.graphicsLayer { rotationZ = 0f }
    }
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "angle",
    )
    return this.graphicsLayer { rotationZ = angle }
}

/** 波浪起伏，给列表错峰用。 */
@Composable
fun Modifier.huiWave(index: Int, amplitudePx: Float = 6f, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    if (!LocalHuiAnimationsActive.current) {
        return this.graphicsLayer { translationY = 0f }
    }
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    val offset = sin(phase + index * 0.55f) * amplitudePx
    return this.graphicsLayer { translationY = offset }
}

/** 组合式：入场 + 按压，最常用的搭配。 */
@Composable
fun Modifier.huiCardMotion(
    spec: HuiMotionSpec,
    visible: Boolean = true,
    delayMs: Int = 0,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    interactive: Boolean = true,
): Modifier = this
    .huiEnter(spec, visible = visible, delayMs = delayMs)
    .then(if (interactive) Modifier.huiPress(interactionSource) else Modifier)
