package org.bakasu.bakasu.data.file

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import org.apache.commons.compress.archivers.zip.ZipFile

/** Reads selected entries without decompressing unrelated files in the installation ZIP. */
internal fun <T> withInstallArchive(
    context: Context,
    uri: Uri,
    read: (ZipFile) -> T): T {
    val archive = File.createTempFile("install-archive-", ".zip", context.cacheDir)
    try {
        // 修复点1：增加严密的异常捕获，防止高版本权限过期导致崩溃
        val input = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: SecurityException) {
            throw IllegalStateException("权限已过期，请重新选择模块文件", e)
        } catch (e: Exception) {
            throw IllegalStateException("无法打开所选文件，请确认文件未被删除", e)
        } ?: throw FileNotFoundException("无法读取文件流: $uri")

        input.use { source ->
            archive.outputStream().use { target -> 
                // 修复点2：使用缓冲流加速拷贝，防止大文件读取卡死
                source.buffered().copyTo(target.buffered()) 
            }
        }
        
        return ZipFile.builder()
            .setFile(archive)
            .setIgnoreLocalFileHeader(true)
            .get()
            .use(read)
    } catch (e: Exception) {
        // 打日志防身
        Log.e("HuiJuiSU", "withInstallArchive error", e)
        throw e
    } finally {
        // 确保临时文件物理销毁
        if (archive.exists()) {
            archive.delete()
        }
    }
}
