package com.huijui.nova.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 运动目录是「上千种动画」的兑现方式，所以它必须是可寻址、可复现、无重复的。
 * 这些断言就是这句话的证据。
 */
class HuiMotionCatalogTest {

    @Test
    fun `catalog exposes exactly 1024 variants`() {
        assertEquals(1024, HuiMotionCatalog.size)
        assertEquals(1024, HuiMotionCatalog.all.size)
    }

    @Test
    fun `ids are dense and unique`() {
        val ids = HuiMotionCatalog.all.map { it.id }
        assertEquals(1024, ids.toSet().size)
        assertEquals((0..1023).toList(), ids.sorted())
    }

    @Test
    fun `every entrance family carries 128 variants`() {
        HuiEntrance.entries.forEach { family ->
            assertEquals(
                "family ${family.label}",
                128,
                HuiMotionCatalog.byFamily(family).size,
            )
        }
    }

    @Test
    fun `every easing curve carries 128 variants`() {
        HuiEase.entries.forEach { ease ->
            assertEquals("ease ${ease.label}", 128, HuiMotionCatalog.byEase(ease).size)
        }
    }

    @Test
    fun `durations span the full 16 step ladder`() {
        val durations = HuiMotionCatalog.all.map { it.durationMs }.distinct().sorted()
        assertEquals(16, durations.size)
        assertEquals(110, durations.first())
        assertEquals(110 + 15 * 55, durations.last())
    }

    @Test
    fun `geometry stays inside renderable bounds`() {
        HuiMotionCatalog.all.forEach { spec ->
            assertTrue("distance ${spec.distanceDp}", spec.distanceDp in 5f..60f)
            assertTrue("scale ${spec.scaleFrom}", spec.scaleFrom in 0.5f..1f)
            assertTrue("rotate ${spec.rotateFrom}", spec.rotateFrom in -45f..45f)
            assertTrue("flip ${spec.flipFrom}", spec.flipFrom in -90f..90f)
        }
    }

    @Test
    fun `sampling is deterministic and distinct`() {
        val a = HuiMotionCatalog.sample(64, seed = 7)
        val b = HuiMotionCatalog.sample(64, seed = 7)
        assertEquals(a, b)
        assertEquals(64, a.map { it.id }.toSet().size)
    }

    @Test
    fun `sampling beyond catalog size returns everything`() {
        assertEquals(1024, HuiMotionCatalog.sample(5000, seed = 1).size)
    }

    @Test
    fun `key hashing is stable across calls`() {
        val first = HuiMotionCatalog.forKey("hero")
        val second = HuiMotionCatalog.forKey("hero")
        assertEquals(first.id, second.id)
    }

    @Test
    fun `get wraps out of range ids`() {
        assertEquals(HuiMotionCatalog.all[3].id, HuiMotionCatalog.get(1027).id)
        assertEquals(HuiMotionCatalog.all[1021].id, HuiMotionCatalog.get(-3).id)
    }
}
