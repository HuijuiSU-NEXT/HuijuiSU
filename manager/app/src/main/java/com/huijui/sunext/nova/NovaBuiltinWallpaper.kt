package com.huijui.sunext.nova

import android.content.Context
import com.huijui.nova.wallpaper.HuiMediaKind
import com.huijui.nova.wallpaper.HuiMediaSpec
import java.io.File

/**
 * 内置动态壁纸。
 *
 * 视频放在 assets 里，首次进入 Nova 时解到 filesDir 再交给 MediaPlayer / VideoView ——
 * 原因是 assets 里的 mp4 不能直接用 `file://` 或 `content://` 打开，
 * 而 MediaPlayer 的 AssetFileDescriptor 入口在 VideoView 上用不了。
 * 解出来一次之后就复用，不再重复拷贝。
 */
object NovaBuiltinWallpaper {

    private const val ASSET_PATH = "nova/miku_star.mp4"
    private const val FILE_NAME = "nova_wallpaper_miku.mp4"
    private const val PREVIEW_ASSET = "nova/miku_star_preview.mp4"
    private const val PREVIEW_FILE = "nova_wallpaper_miku_preview.mp4"

    /** 内置壁纸是否随包提供。 */
    fun available(context: Context): Boolean = runCatching {
        context.assets.openFd(ASSET_PATH).use { true }
    }.getOrDefault(false)

    /**
     * 确保内置视频已经解到本地，返回对应的媒体描述；没有内置素材时返回 null。
     */
    fun ensureExtracted(context: Context): HuiMediaSpec? {
        if (!available(context)) return null
        val target = File(context.filesDir, FILE_NAME)
        if (!target.exists() || target.length() == 0L) {
            runCatching {
                context.assets.open(ASSET_PATH).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }.onFailure {
                runCatching { target.delete() }
                return null
            }
        }
        if (!target.exists() || target.length() == 0L) return null

        // 预览档：只给应用内背景用，系统壁纸仍然用原始画质
        val preview = File(context.filesDir, PREVIEW_FILE)
        if (!preview.exists() || preview.length() == 0L) {
            runCatching {
                context.assets.open(PREVIEW_ASSET).use { input ->
                    preview.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        val previewUri = if (preview.exists() && preview.length() > 0L) {
            "file://${preview.absolutePath}"
        } else {
            null
        }
        return HuiMediaSpec(
            uri = "file://${target.absolutePath}",
            kind = HuiMediaKind.Video,
            previewUri = previewUri,
        )
    }
}
