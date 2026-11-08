package org.bakasu.bakasu.huijui.data

import android.content.Context
import org.bakasu.bakasu.huijui.feature.HotModule
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import java.io.File

/**
 * 内置资源的「一键」动作。
 *
 * 资源随 APK 分发（见 [HuijuiBuiltinBundle]），这里只负责调用：
 * - 安装一键隐藏模块，并尝试热激活
 * - 运行 Evil Service 自查
 * - 查询元模块状态
 */
object HuijuiBuiltinActions {

    /** 安装内置的一键隐藏模块（hide_bundle v1.3），成功后会尝试热激活。 */
    suspend fun installHideBundle(context: Context): Result {
        HuijuiBuiltinBundle.deployIfNeeded(context)
        val zip = File(HuijuiBuiltinBundle.DEVICE_DIR, HuijuiBuiltinBundle.HIDE_ZIP_NAME)
        if (!HuijuiBuiltinBundle.remoteExists(zip.absolutePath)) {
            val why = HuijuiBuiltinBundle.lastError
            return Result(false, "内置隐藏模块未就位" + (if (why.isBlank()) "（部署失败，可能未授予 root）" else "：$why"))
        }

        val out = HuijuiShellUtils.runAsRoot(
            "/data/adb/ksud module install '${zip.absolutePath}' 2>&1",
            timeoutSeconds = 180,
        )
        val ok = out.contains("success", true) || out.contains("installed", true) ||
            !out.contains("error", true)
        if (!ok) return Result(false, "安装失败：${out.take(120)}")

        val hot = HotModule.activate("hide_bundle")
        return Result(
            true,
            if (hot.ok) "已安装并热激活" else "已安装，${hot.message}",
        )
    }

    /** 安装内置元模块 Hybrid Mount（其它模块的挂载基础）。 */
    suspend fun installMetaModule(context: Context): Result {
        HuijuiBuiltinBundle.deployIfNeeded(context)
        if (HotModule.metaState() == HotModule.MetaState.READY) return Result(true, "元模块已就位")
        val zip = File(HuijuiBuiltinBundle.DEVICE_DIR, HuijuiBuiltinBundle.HYBRID_ZIP_NAME)
        if (!HuijuiBuiltinBundle.remoteExists(zip.absolutePath)) {
            val why = HuijuiBuiltinBundle.lastError
            return Result(false, "内置元模块未就位" + (if (why.isBlank()) "（部署失败，可能未授予 root）" else "：$why"))
        }

        val out = HuijuiShellUtils.runAsRoot(
            "/data/adb/ksud module install '${zip.absolutePath}' 2>&1",
            timeoutSeconds = 180,
        )
        val ok = out.contains("success", true) || out.contains("installed", true) ||
            !out.contains("error", true)
        return Result(ok, if (ok) "元模块已安装，重启后生效" else "安装失败：${out.take(120)}")
    }

    /** 运行 Evil Service 自查，返回脚本输出（可能较长）。 */
    suspend fun runEvilCheck(context: Context): String =
        HuijuiBuiltinBundle.runEvilCheck(context)

    /** 元模块运行时状态。 */
    fun metaStatus(): String = HotModule.status().ifBlank { HotModule.metaMessage() }

    /** 按输出做一句人话总结，卡片只显示这一行。 */
    fun summarizeEvilCheck(output: String): String {
        if (output.isBlank()) return "没有输出（可能需要 root）"
        val hits = Regex("(?i)(异常|found|hit|检测到|warning|risk)").findAll(output).count()
        val clean = Regex("(?i)(正常|clean|pass|未发现)").findAll(output).count()
        return when {
            hits > 0 -> "发现 $hits 处可疑项，详见日志"
            clean > 0 -> "未发现异常"
            else -> "检查完成"
        }
    }

    data class Result(val ok: Boolean, val message: String)

    /**
     * 运行内置的春秋词条修复脚本。
     *
     * 脚本在 zip 里，先解压到 /data/adb/huijui/chunqiu/ 再执行。
     * 这是个会改系统属性的脚本，调用方必须先让用户确认。
     */
    suspend fun runChunqiuFix(context: Context): Result {
        HuijuiBuiltinBundle.deployIfNeeded(context)
        val zip = File(HuijuiBuiltinBundle.DEVICE_DIR, HuijuiBuiltinBundle.CHUNQIU_ZIP_NAME)
        if (!HuijuiBuiltinBundle.remoteExists(zip.absolutePath)) {
            val why = HuijuiBuiltinBundle.lastError
            return Result(false, "内置脚本未就位" + (if (why.isBlank()) "（部署失败，可能未授予 root）" else "：$why"))
        }

        val dir = "${HuijuiBuiltinBundle.DEVICE_DIR}/chunqiu"
        val script = "$dir/JinBeiChunQiuFixToolPro2.sh"
        val out = HuijuiShellUtils.runAsRoot(
            "rm -rf '$dir'; mkdir -p '$dir'; " +
                "unzip -o -q '${zip.absolutePath}' -d '$dir' 2>/dev/null || " +
                "(cd '$dir' && /data/adb/ksu/bin/busybox unzip -o -q '${zip.absolutePath}'); " +
                "S=${'$'}(find '$dir' -name '*.sh' | head -1); " +
                "[ -n \"${'$'}S\" ] || { echo NO_SCRIPT; exit 1; }; " +
                "sh \"${'$'}S\" >/dev/null 2>&1; echo DONE",
            timeoutSeconds = 120,
        )
        return when {
            out.contains("NO_SCRIPT") -> Result(false, "包里没有找到脚本")
            out.contains("DONE") -> Result(true, "修复脚本已执行")
            else -> Result(false, "执行失败：${out.take(120)}")
        }
    }
}
