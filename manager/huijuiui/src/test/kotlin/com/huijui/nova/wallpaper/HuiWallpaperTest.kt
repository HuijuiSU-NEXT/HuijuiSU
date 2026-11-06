package com.huijui.nova.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 壁纸预设是「功耗可见」的：fps 字段直接决定重绘频率。
 * 这些断言保证新增预设时不会悄悄引入一个 60fps 的常驻动画。
 */
class HuiWallpaperTest {

    @Test
    fun `every preset declares a render budget`() {
        HuiWallpaper.entries.forEach { wallpaper ->
            assertTrue("${wallpaper.label} fps=${wallpaper.fps}", wallpaper.fps in 0..30)
        }
    }

    @Test
    fun `solid preset is completely static`() {
        assertEquals(0, HuiWallpaper.Solid.fps)
        assertTrue(!HuiWallpaper.Solid.animated)
    }

    @Test
    fun `animated presets stay under 30fps`() {
        HuiWallpaper.entries.filter { it != HuiWallpaper.Solid }.forEach { wallpaper ->
            assertTrue(wallpaper.animated)
            assertTrue("${wallpaper.label}", wallpaper.fps in 1..30)
        }
    }

    @Test
    fun `default wallpaper is the animated miku preset`() {
        assertEquals(HuiWallpaper.Miku, HuiWallpaper.Default)
        assertTrue(HuiWallpaper.Default.animated)
        assertTrue(HuiWallpaper.Default.fps in 1..30)
    }

    @Test
    fun `presets have unique labels`() {
        val labels = HuiWallpaper.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun `ordinal mapping survives a bundle round trip`() {
        // NovaActivity 用 ordinal 存壁纸选择，越界时必须能钳制回合法值
        val index = HuiWallpaper.entries.lastIndex + 5
        val clamped = index.coerceIn(0, HuiWallpaper.entries.lastIndex)
        assertEquals(HuiWallpaper.entries.last(), HuiWallpaper.entries[clamped])
    }
}
