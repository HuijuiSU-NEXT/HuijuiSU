package org.bakasu.bakasu.data.module

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ModuleBackupManager {
    fun getBackupDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "huijui_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun backupModule(moduleId: String, context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val srcDir = File("/data/adb/modules/$moduleId")
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destFile = File(getBackupDir(context), "${moduleId}_${timeStamp}.zip")
            
            // 使用 Java 原生 ZipOutputStream 打包，彻底摆脱对 busybox/magisk zip 的依赖
            java.util.zip.ZipOutputStream(destFile.outputStream()).use { zos ->
                srcDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entryName = file.absolutePath.substring(srcDir.absolutePath.length + 1)
                        zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            if (destFile.exists() && destFile.length() > 0) Result.success(destFile)
            else Result.failure(Exception("打包失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cmd = "ksud module install '${file.absolutePath}'"
            val result = Shell.cmd(cmd).exec()
            if (result.isSuccess) Result.success(Unit)
            else Result.failure(Exception(result.err.joinToString("\n")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBackupList(context: Context): List<File> {
        return getBackupDir(context).listFiles { file -> file.extension == "zip" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteBackup(file: File): Boolean = file.delete()
}
