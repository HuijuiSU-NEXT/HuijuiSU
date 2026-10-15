package org.bakasu.bakasu.huijui.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import org.bakasu.bakasu.huijui.data.HuijuiHiddenDefaults
import org.bakasu.bakasu.huijui.data.HuijuiDefaultsApplier

/**
 * HuiJuiSU Next 内置隐藏默认值应用器
 *
 * 在 App 启动时静默执行，把默认隐藏规则应用到内核。
 * 用 SharedPreferences 记录已应用版本，只在版本变化时重新应用。
 */
object HuijuiDefaultsApplier {

    private const val TAG = "HuijuiDefaults"
    private const val PREFS_NAME = "huijui_hidden_defaults"
    private const val KEY_APPLIED_VERSION = "applied_version"

    /**
     * 检查并应用默认规则。幂等操作，多次调用不会重复添加。
     */
    suspend fun applyIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val appliedVersion = prefs.getInt(KEY_APPLIED_VERSION, 0)

            if (appliedVersion >= HuijuiHiddenDefaults.DEFAULTS_VERSION) {
                Log.d(TAG, "默认规则已应用 (v$appliedVersion)，跳过")
                return@withContext
            }

            // 检查 root 是否可用（未安装内核时静默跳过）
            if (!isRootAvailable()) {
                Log.d(TAG, "root 不可用，跳过默认规则")
                return@withContext
            }

            Log.i(TAG, "开始应用默认隐藏规则 v${HuijuiHiddenDefaults.DEFAULTS_VERSION}")

            // 应用 sus_path 列表
            var success = 0
            HuijuiHiddenDefaults.SUS_PATHS.forEach { path ->
                val out = runKsud("sus_path add '$path'")
                if (out.isBlank() || !out.contains("error", ignoreCase = true)) {
                    success++
                    Log.d(TAG, "  ✓ sus_path: $path")
                } else {
                    Log.w(TAG, "  ✗ sus_path: $path -> $out")
                }
            }
            Log.i(TAG, "  sus_path: $success/${HuijuiHiddenDefaults.SUS_PATHS.size} 成功")

            // 保存配置
            runKsud("feature save")

            // 记录已应用版本
            prefs.edit()
                .putInt(KEY_APPLIED_VERSION, HuijuiHiddenDefaults.DEFAULTS_VERSION)
                .apply()
            Log.i(TAG, "默认规则应用完成，版本 v${HuijuiHiddenDefaults.DEFAULTS_VERSION}")
        } catch (t: Throwable) {
            Log.w(TAG, "应用默认规则失败：${t.message}")
        }
    }

    /**
     * 强制重新应用（用户主动触发时调用）。
     */
    suspend fun forceReapply(context: Context) = withContext(Dispatchers.IO) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_APPLIED_VERSION, 0)
            .apply()
        applyIfNeeded(context)
    }

    private fun isRootAvailable(): Boolean = try {
        val out = runShell("id")
        out.contains("uid=0")
    } catch (t: Throwable) {
        false
    }

    private fun runKsud(args: String): String =
        runShell("/data/adb/ksud $args")

    private fun runShell(cmd: String): String =
        HuijuiShellUtils.runAsRoot(cmd, timeoutSeconds = 30, trim = true)
}
