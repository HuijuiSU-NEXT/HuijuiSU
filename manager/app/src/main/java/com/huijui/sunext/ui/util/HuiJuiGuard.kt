
package com.huijui.sunext.ui.util

import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns

object HuiJuiGuard {
    /**
     * 防呆校验：检查刷机文件后缀是否匹配
     */
    fun validateFlashFile(ctx: Context, uri: Uri, expectedType: String): Pair<Boolean, String> {
        var fileName = ""
        ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        if (fileName.isEmpty()) return Pair(false, "无法读取文件名，请检查文件权限。")

        val lowerName = fileName.lowercase()
        return when (expectedType) {
            "img" -> {
                if (lowerName.endsWith(".img") || lowerName.endsWith(".img.gz")) {
                    Pair(true, "校验通过")
                } else {
                    Pair(false, "防呆拦截：这看起来不是一个内核镜像(.img)文件！请勿乱刷，容易变砖！")
                }
            }
            "zip" -> {
                if (lowerName.endsWith(".zip")) {
                    Pair(true, "校验通过")
                } else {
                    Pair(false, "防呆拦截：这看起来不是一个模块(.zip)文件！")
                }
            }
            "ko" -> {
                if (lowerName.endsWith(".ko") || lowerName.endsWith(".ko.gz")) {
                    Pair(true, "校验通过")
                } else {
                    Pair(false, "防呆拦截：这看起来不是一个内核模块(.ko)文件！")
                }
            }
            else -> Pair(true, "未知类型，跳过校验")
        }
    }
}
