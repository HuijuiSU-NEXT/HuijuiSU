package com.huijui.nova.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 音效参数直接决定「听着像 UI」还是「听着像报错」。
 * 频段放在人耳最敏感的 300Hz–1.5kHz，时长控制在 300ms 内，避免连续点击时糊成一片。
 */
class HuiSfxTest {

    @Test
    fun `every cue stays in the ui frequency band`() {
        Sfx.entries.forEach { sfx ->
            assertTrue("${sfx.name} freq=${sfx.freq}", sfx.freq in 200f..2000f)
        }
    }

    @Test
    fun `tap cues are short enough to feel responsive`() {
        assertTrue(Sfx.Tap.durationMs <= 80)
        assertTrue(Sfx.Confirm.durationMs <= 150)
        assertTrue(Sfx.Back.durationMs <= 120)
    }

    @Test
    fun `volumes stay below clipping`() {
        Sfx.entries.forEach { sfx ->
            assertTrue("${sfx.name} volume=${sfx.volume}", sfx.volume in 0.05f..0.6f)
        }
    }

    @Test
    fun `launch cue is the longest and lowest`() {
        val launch = Sfx.Launch
        assertEquals(Sfx.entries.maxOf { it.durationMs }, launch.durationMs)
        assertEquals(Sfx.entries.minOf { it.freq }, launch.freq)
    }
}
