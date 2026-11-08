package org.bakasu.bakasu.huijui.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import org.bakasu.bakasu.huijui.data.HuijuiHiddenDefaults
import org.bakasu.bakasu.huijui.data.HuijuiDefaultsApplier

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

            // 先把随 APK 内置的资源部署到 /data/adb/huijui（模块包 + 自查脚本）
            HuijuiBuiltinBundle.deployIfNeeded(context)

            Log.i(TAG, "开始应用默认隐藏规则 v${HuijuiHiddenDefaults.DEFAULTS_VERSION}")

            // 1~4 合并成一次 su 调用：
            // 之前每条规则一个 su 进程（十几条 = 十几个进程），启动时明显卡顿；
            // 现在拼成一个脚本一次执行，启动开销从 O(n) 降到 O(1)。
            val script = buildString {
                appendLine("K=/data/adb/ksud")
                HuijuiHiddenDefaults.SUS_PATHS.forEach { appendLine("\$K sus_path add '$it' 2>/dev/null") }
                HuijuiHiddenDefaults.SUS_KSTATS.forEach { appendLine("\$K sus_kstat add '$it' normal 2>/dev/null") }
                if (HuijuiHiddenDefaults.ENABLE_SUS_MOUNT) {
                    appendLine("\$K hide_sus_mnts_for_non_su_procs add 2>/dev/null")
                }
                if (HuijuiHiddenDefaults.ENABLE_AVC_LOG_SPOOFING) {
                    appendLine("\$K avc_log_spoofing add 2>/dev/null")
                }
                appendLine("\$K feature save 2>/dev/null")
                append("echo HUijuiDone")
            }
            val out = runShell(script)
            Log.i(TAG, "隐藏规则一次应用完成：${out.contains("HUijuiDone")}")

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
