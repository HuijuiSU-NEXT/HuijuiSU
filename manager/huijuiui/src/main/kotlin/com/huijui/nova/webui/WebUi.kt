package com.huijui.nova.webui

import android.content.Context
import com.huijui.nova.hide.HideBundle
import java.io.File

/**
 * 模块 WebUI。
 *
 * 模块可以在自己的目录里放 `webroot/index.html`，管理器负责把它「搬」到应用可读的位置，
 * 再用 WebView 打开。直接让 WebView 读 /data/adb 是不行的（沙箱限制）。
 *
 * 做法：
 * 1. 用 root 把 `/data/adb/modules/<id>/webroot` 复制到应用缓存目录
 * 2. chmod 让应用可读
 * 3. WebView 加载 file:// 下的 index.html
 */
object WebUi {

    private fun cacheDir(context: Context, id: String): File =
        File(File(context.cacheDir, "webui"), id)

    /** 复制并返回入口文件；失败返回 null。 */
    suspend fun prepare(context: Context, id: String): File? {
        val dest = cacheDir(context, id)
        val destPath = dest.absolutePath
        // 缓存目录属于应用自己，root 复制后把权限放开
        HideBundle.runScript(
            """
            rm -rf "$destPath"
            mkdir -p "$destPath"
            cp -r /data/adb/modules/$id/webroot/. "$destPath"/ 2>/dev/null
            chmod -R 777 "$destPath" 2>/dev/null
            echo done
            """.trimIndent(),
        )
        val index = listOf("index.html", "index.htm").map { File(dest, it) }.firstOrNull { it.isFile }
            ?: return null
        return index
    }

    /** 本地存在性检查（不用 root）。 */
    fun cachedIndex(context: Context, id: String): File? =
        File(cacheDir(context, id), "index.html").takeIf { it.isFile }
}
