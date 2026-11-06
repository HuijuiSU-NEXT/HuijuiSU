package com.huijui.nova.motion

import com.huijui.nova.design.HuiAccent
import com.huijui.nova.design.HuiGlassQuality
import com.huijui.nova.design.HuiMaterial
import com.huijui.nova.design.HuiMotionLevel
import com.huijui.nova.design.HuiThemeState
import com.huijui.nova.design.huiColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 全局节奏（动效强度 → 时长倍率 / 玻璃档位 / 错峰延迟）的行为断言。
 * 这三件事直接决定观感一致性与功耗，不能靠肉眼保证。
 */
class HuiMotionScalingTest {

    @Test
    fun `calm scales durations down`() {
        val spec = HuiMotionCatalog.get(128)
        val scaled = spec.scaled(HuiMotionLevel.Calm)
        assertEquals((spec.durationMs * 0.65f).toInt(), scaled.durationMs)
        assertTrue(scaled.durationMs < spec.durationMs)
    }

    @Test
    fun `wild scales durations up`() {
        val spec = HuiMotionCatalog.get(128)
        val scaled = spec.scaled(HuiMotionLevel.Wild)
        assertEquals((spec.durationMs * 1.45f).toInt(), scaled.durationMs)
        assertTrue(scaled.durationMs > spec.durationMs)
    }

    @Test
    fun `scaled duration stays inside sane bounds`() {
        HuiMotionCatalog.all.forEach { spec ->
            HuiMotionLevel.entries.forEach { level ->
                val duration = spec.scaled(level).durationMs
                assertTrue("duration $duration", duration in 40..2000)
            }
        }
    }

    @Test
    fun `scaling keeps identity and geometry`() {
        val spec = HuiMotionCatalog.get(77)
        val scaled = spec.scaled(HuiMotionLevel.Wild)
        assertEquals(spec.id, scaled.id)
        assertEquals(spec.entrance, scaled.entrance)
        assertEquals(spec.ease, scaled.ease)
        assertEquals(spec.distanceDp, scaled.distanceDp, 0.0001f)
    }

    @Test
    fun `stagger delay follows the level tempo`() {
        assertEquals(126, huiStaggerDelay(3, HuiMotionLevel.Flow))
        assertEquals(182, huiStaggerDelay(3, HuiMotionLevel.Wild))
        assertEquals(81, huiStaggerDelay(3, HuiMotionLevel.Calm))
        assertEquals(0, huiStaggerDelay(-4, HuiMotionLevel.Flow))
    }

    @Test
    fun `glass quality follows motion level`() {
        HuiMotionLevel.entries.forEach { level ->
            val state = HuiThemeState(
                accent = HuiAccent.Aurora,
                dark = true,
                material = HuiMaterial.LiquidGlass,
                motion = level,
                colors = huiColors(HuiAccent.Aurora, true),
            )
            val expected = when (level) {
                HuiMotionLevel.Calm -> HuiGlassQuality.Low
                HuiMotionLevel.Flow -> HuiGlassQuality.Balanced
                HuiMotionLevel.Wild -> HuiGlassQuality.High
            }
            assertEquals(level.label, expected, state.glassQuality)
        }
    }
}
