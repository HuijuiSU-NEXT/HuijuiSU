package com.huijui.sunext.huijui.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.huijui.sunext.huijui.util.HuijuiShellUtils

/**
 * HuiJuiSU NEXT 应用管理器数据层
 *
 * 封装 pm / am 命令，提供冻结、解冻、强停、清数据、卸载（当前用户）操作。
 */
object HuijuiAppManagerRepository {

    private const val TAG = "HuijuiAppManager"

    suspend fun freeze(pkg: String): Result<Unit> =
        runCommand("pm disable-user --user 0 $pkg", "冻结 $pkg")

    suspend fun unfreeze(pkg: String): Result<Unit> =
        runCommand("pm enable $pkg", "解冻 $pkg")

    suspend fun forceStop(pkg: String): Result<Unit> =
        runCommand("am force-stop $pkg", "强停 $pkg")

    suspend fun clearCache(pkg: String): Result<Unit> =
        runCommand("pm clear $pkg", "清数据 $pkg")

    suspend fun uninstallForUser(pkg: String): Result<Unit> =
        runCommand("pm uninstall --user 0 $pkg", "卸载 $pkg")

    suspend fun isFrozen(pkg: String): Boolean = withContext(Dispatchers.IO) {
        val out = HuijuiShellUtils.runAsRoot(
            "pm list packages -d | grep -w $pkg",
            timeoutSeconds = 10,
            trim = true,
        )
        out.isNotBlank() && out.contains(pkg)
    }

    private suspend fun runCommand(cmd: String, label: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val out = HuijuiShellUtils.runAsRoot(cmd, timeoutSeconds = 30, trim = true)
                val failed = out.contains("Error", true) ||
                    out.contains("Failure", true) ||
                    out.contains("Exception", true) ||
                    out.contains("denied", true) ||
                    out.contains("not found", true)
                if (failed) {
                    Log.w(TAG, "$label 失败: $out")
                    Result.failure(Exception(out.take(120)))
                } else {
                    Log.i(TAG, "$label 成功")
                    Result.success(Unit)
                }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
}
