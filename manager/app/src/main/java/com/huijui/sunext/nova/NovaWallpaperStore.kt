package com.huijui.sunext.nova

import android.content.Context
import com.huijui.nova.wallpaper.HuiMediaKind
import com.huijui.nova.wallpaper.HuiMediaSpec
import com.huijui.nova.wallpaper.HuiWallpaper

/**
 * Nova 壁纸选择的持久化。
 *
 * 为什么要落到 SharedPreferences：系统级动态壁纸服务（NovaWallpaperService）
 * 跑在另一个进程、另一个生命周期里，只能通过同一个存储拿到用户选了什么。
 */
object NovaWallpaperStore {

    private const val PREFS = "nova_wallpaper"
    private const val KEY_PRESET = "preset"
    private const val KEY_MEDIA_URI = "media_uri"
    private const val KEY_MEDIA_KIND = "media_kind"
    private const val KEY_INITIALIZED = "media_initialized"

    data class Selection(
        val preset: HuiWallpaper,
        val media: HuiMediaSpec?,
    )

    fun load(context: Context): Selection {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val preset = HuiWallpaper.entries.getOrElse(prefs.getInt(KEY_PRESET, HuiWallpaper.Default.ordinal)) {
            HuiWallpaper.Default
        }
        val uri = prefs.getString(KEY_MEDIA_URI, null)
        val kindName = prefs.getString(KEY_MEDIA_KIND, null)
        val media = if (!uri.isNullOrBlank()) {
            val kind = runCatching { HuiMediaKind.valueOf(kindName ?: "") }
                .getOrElse { HuiMediaKind.fromPath(uri) }
            HuiMediaSpec(uri = uri, kind = kind)
        } else {
            null
        }
        return Selection(preset, media)
    }

    /** 内置壁纸是否已经处理过（解包 + 落库）。只处理一次，之后用户清除也不会再冒出来。 */
    fun isInitialized(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_INITIALIZED, false)

    fun markInitialized(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INITIALIZED, true)
            .apply()
    }

    fun savePreset(context: Context, preset: HuiWallpaper) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PRESET, preset.ordinal)
            .apply()
    }

    fun saveMedia(context: Context, uri: String, kind: HuiMediaKind) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEDIA_URI, uri)
            .putString(KEY_MEDIA_KIND, kind.name)
            .apply()
    }

    fun clearMedia(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MEDIA_URI)
            .remove(KEY_MEDIA_KIND)
            .apply()
    }
}
