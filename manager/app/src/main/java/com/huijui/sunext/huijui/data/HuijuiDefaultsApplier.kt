package com.huijui.sunext.huijui.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.huijui.sunext.huijui.util.HuijuiShellUtils
import com.huijui.sunext.huijui.data.HuijuiHiddenDefaults
import com.huijui.sunext.huijui.data.HuijuiDefaultsApplier

/**
 * HuiJuiSU NEXT 内置隐藏默认值应用器
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

            // 1. 应用 sus_path 列表
            var pathSuccess = 0
            HuijuiHiddenDefaults.SUS_PATHS.forEach { path ->
                val out = runKsud("sus_path add '$path'")
                if (out.isBlank() || !out.contains("error", ignoreCase = true)) {
                    pathSuccess++
                    Log.d(TAG, "  ✓ sus_path: $path")
                } else {
                    Log.w(TAG, "  ✗ sus_path: $path -> $out")
                }
            }
            Log.i(TAG, "  sus_path: $pathSuccess/${HuijuiHiddenDefaults.SUS_PATHS.size} 成功")

            // 2. 应用 sus_kstat 列表
            var kstatSuccess = 0
            HuijuiHiddenDefaults.SUS_KSTATS.forEach { path ->
                val out = runKsud("sus_kstat add '$path' normal")
                if (out.isBlank() || !out.contains("error", ignoreCase = true)) {
                    kstatSuccess++
                    Log.d(TAG, "  ✓ sus_kstat: $path")
                } else {
                    Log.w(TAG, "  ✗ sus_kstat: $path -> $out")
                }
            }
            Log.i(TAG, "  sus_kstat: $kstatSuccess/${HuijuiHiddenDefaults.SUS_KSTATS.size} 成功")

            // 3. 开启挂载点隐藏
            if (HuijuiHiddenDefaults.ENABLE_SUS_MOUNT) {
                val out = runKsud("hide_sus_mnts_for_non_su_procs add")
                Log.i(TAG, if (out.isBlank() || !out.contains("error", true))
                    "  ✓ 挂载点隐藏已开启" else "  ✗ 挂载点隐藏：$out")
            }

            // 4. 开启 AVC 日志伪装
            if (HuijuiHiddenDefaults.ENABLE_AVC_LOG_SPOOFING) {
                val out = runKsud("avc_log_spoofing add")
                Log.i(TAG, if (out.isBlank() || !out.contains("error", true))
                    "  ✓ AVC 日志伪装已开启" else "  ✗ AVC 日志伪装：$out")
            }

            // 5. 保存配置
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
