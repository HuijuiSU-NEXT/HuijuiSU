package com.huijui.nova.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 媒体类型判定是「能不能播」的前置条件：判错就会把 MP4 当图片塞给 ImageDecoder，
 * 或者把 GIF 当静态图。这里把 MIME 与后缀两条路径都钉死。
 */
class HuiMediaKindTest {

    @Test
    fun `gif mime maps to animated kind`() {
        assertEquals(HuiMediaKind.Gif, HuiMediaKind.fromMime("image/gif"))
        assertEquals(HuiMediaKind.Gif, HuiMediaKind.fromMime("image/GIF"))
    }

    @Test
    fun `video mimes map to video kind`() {
        assertEquals(HuiMediaKind.Video, HuiMediaKind.fromMime("video/mp4"))
        assertEquals(HuiMediaKind.Video, HuiMediaKind.fromMime("video/webm"))
        assertEquals(HuiMediaKind.Video, HuiMediaKind.fromMime("video/quicktime"))
    }

    @Test
    fun `other images map to image kind`() {
        assertEquals(HuiMediaKind.Image, HuiMediaKind.fromMime("image/png"))
        assertEquals(HuiMediaKind.Image, HuiMediaKind.fromMime("image/jpeg"))
        assertEquals(HuiMediaKind.Image, HuiMediaKind.fromMime("image/webp"))
        assertEquals(HuiMediaKind.Image, HuiMediaKind.fromMime(null))
    }

    @Test
    fun `path fallback handles suffixes`() {
        assertEquals(HuiMediaKind.Gif, HuiMediaKind.fromPath("/sdcard/a.GIF"))
        assertEquals(HuiMediaKind.Video, HuiMediaKind.fromPath("/sdcard/a.mp4"))
        assertEquals(HuiMediaKind.Video, HuiMediaKind.fromPath("/sdcard/a.webm"))
        assertEquals(HuiMediaKind.Image, HuiMediaKind.fromPath("/sdcard/a.webp"))
    }
}
