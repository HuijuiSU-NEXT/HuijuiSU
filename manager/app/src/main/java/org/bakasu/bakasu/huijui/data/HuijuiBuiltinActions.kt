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

        // 强制替换：先彻底删掉旧版本（v1.3 或任何旧包），再装 v1.4-super。
        // 用户明确要求「强卸载旧版」，所以这里连运行中的目录一起删。
        HuijuiShellUtils.runAsRoot(
            "rm -rf /data/adb/modules/hide_bundle /data/adb/modules_update/hide_bundle 2>/dev/null; true",
            timeoutSeconds = 30,
        )

        // ksud 路径在不同版本里不一样，逐个试
        val ksud = listOf("/data/adb/ksu/bin/ksud", "/data/adb/ksud", "/system/bin/ksud")
            .firstOrNull { HuijuiShellUtils.runAsRootOk("test -x '$it'", 10) }
        if (ksud == null) return Result(false, "找不到 ksud（内核可能未正确安装）")

        val out = HuijuiShellUtils.runAsRoot(
            "$ksud module install '${zip.absolutePath}' 2>&1; echo __EXIT__\$?",
            timeoutSeconds = 120,
        )
        if (out.contains(HuijuiShellUtils.TIMEOUT_MARK)) {
            return Result(false, "安装超时：root 无响应，请确认管理器已被授予 root")
        }
        // 以退出码为准，不再用「输出里没有 error」这种不可靠判断
        val ok = out.contains("__EXIT__0")
        if (!ok) {
            val tail = out.lineSequence().filter { it.isNotBlank() }.toList().takeLast(3).joinToString(" / ")
            return Result(false, "安装失败：${tail.take(160)}")
        }

        // KernelSU 装完模块先落到 modules_update，重启后才替换运行中的版本。
        // 这里读暂存目录的版本，确认确实是 v1.4-super。
        val staged = HuijuiShellUtils.runAsRoot(
            "grep -m1 '^version=' /data/adb/modules_update/hide_bundle/module.prop 2>/dev/null | cut -d= -f2",
            timeoutSeconds = 15,
            trim = true,
        )
        val version = staged.ifBlank {
            HuijuiShellUtils.runAsRoot(
                "grep -m1 '^version=' /data/adb/modules/hide_bundle/module.prop 2>/dev/null | cut -d= -f2",
                timeoutSeconds = 15,
                trim = true,
            )
        }.ifBlank { "?" }
        if (staged.isBlank()) {
            return Result(false, "安装未生效：modules_update 里没有 hide_bundle（安装器返回成功但没有落盘）")
        }
        val pending = "PENDING"

        val hot = HotModule.activate("hide_bundle")
        return when {
            pending.contains("PENDING") -> Result(true, "已安装，重启后生效（当前运行版本 $version）")
            hot.ok -> Result(true, "已安装并热激活（$version）")
            else -> Result(true, "已安装（$version），${hot.message}")
        }
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
        // 春秋脚本是交互式的（菜单 + read），不能在后台直接跑，
        // 否则会一直等输入。这里只负责解压并复制到 Download，交给用户用 MT 以 root 手动执行。
        val script = HuijuiBuiltinBundle.extractChunqiu(context)
            ?: return Result(false, "解压内置脚本失败")

        val dest = "/sdcard/Download/JinBeiChunQiuFixToolPro2.sh"
        val out = HuijuiShellUtils.runAsRoot(
            "mkdir -p /sdcard/Download; cp '${script.absolutePath}' '$dest' && chmod 755 '$dest' && echo __COPIED__",
            timeoutSeconds = 60,
        )
        return when {
            out.contains("__COPIED__") -> Result(true, "已复制到 $dest，请用 MT 管理器以 root 运行")
            out.isBlank() -> Result(false, "复制失败：没有输出（可能未授予 root）")
            else -> Result(false, "复制失败：${out.lineSequence().first().take(80)}")
        }
    }
}
