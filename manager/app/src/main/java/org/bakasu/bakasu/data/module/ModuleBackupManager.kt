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
            val srcPath = "/data/adb/modules/$moduleId"
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destFile = File(getBackupDir(context), "${moduleId}_${timeStamp}.zip")
            val cmd = "cd /data/adb/modules && zip -r '${destFile.absolutePath}' '$moduleId'"
            val result = Shell.cmd(cmd).exec()

            if (result.isSuccess && destFile.exists()) Result.success(destFile)
            else Result.failure(Exception(result.err.joinToString("\n")))
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
