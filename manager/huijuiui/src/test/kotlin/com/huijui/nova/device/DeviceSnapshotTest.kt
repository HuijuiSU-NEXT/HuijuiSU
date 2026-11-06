package com.huijui.nova.device

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 设备摘要是首页 Hero 卡的副标题来源，格式错了会直接显示在首屏上。
 */
class DeviceSnapshotTest {

    private fun snapshot(
        kernel: String = "6.1.75-android14",
        release: String = "14",
        ramMb: Int = 12288,
    ) = DeviceSnapshot(
        model = "2410DPN6CC",
        manufacturer = "Xiaomi",
        androidRelease = release,
        sdkInt = 34,
        kernel = kernel,
        abi = "arm64-v8a",
        totalRamMb = ramMb,
        cores = 8,
    )

    @Test
    fun `summary contains manufacturer model and api`() {
        assertEquals(
            "Xiaomi 2410DPN6CC · Android 14 (API 34)",
            snapshot().summary,
        )
    }

    @Test
    fun `kmi keeps android release and kernel major minor`() {
        assertEquals("android14-6.1", snapshot().kmi)
        assertEquals("android13-5.15", snapshot(kernel = "5.15.94-android13", release = "13").kmi)
        assertEquals("android16-6.12", snapshot(kernel = "6.12.30-android16", release = "16").kmi)
    }

    @Test
    fun `kmi tolerates a kernel string without dash`() {
        assertEquals("android14-6.1", snapshot(kernel = "6.1.75").kmi)
    }

    @Test
    fun `ram label switches between mb and gb`() {
        assertEquals("12.0 GB", snapshot(ramMb = 12288).ramLabel)
        assertEquals("8.0 GB", snapshot(ramMb = 8192).ramLabel)
        assertEquals("512 MB", snapshot(ramMb = 512).ramLabel)
    }
}
