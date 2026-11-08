package org.bakasu.bakasu.huijui.feature

import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import java.io.File
import java.util.zip.ZipFile

/**
 * 真·热刷入：借助内置元模块 Hybrid Mount 的 runtime 接口，
 * 让新装的模块在**不重启**的情况下立即挂载生效。
 *
 * Hybrid Mount 提供：
 * ```
 * hybrid-mount runtime status|prepare-reboot|load ID|unload ID|reload ID
 * ```
 *
 * 注意两个容易误判的点（之前「永远显示未加载元模块」就是这两个）：
 *
 * 1. KernelSU 安装模块后先落到 `/data/adb/modules_update/`，**重启才会进 `/data/adb/modules/`**。
 *    只查 `modules/` 会把「已安装待重启」误判成「没装」。
 * 2. 安装后二进制在模块根目录 `hybrid-mount`（`customize.sh` 会把 `binaries/` 删掉），
 *    不是 `hybrid-mount-arm64` 那个名字。
 */
object HotModule {

    private const val META_ID = "hybrid_mount"
    private const val MODULES = "/data/adb/modules"
    private const val UPDATES = "/data/adb/modules_update"

    enum class MetaState { READY, NEEDS_REBOOT, MISSING }

    data class Result(val ok: Boolean, val message: String)

    /** 当前元模块状态。 */
    fun metaState(): MetaState {
        // 已生效：模块目录存在、未被禁用、二进制可执行
        val active = "$MODULES/$META_ID"
        if (File("$active/module.prop").isFile && !File("$active/disable").exists()) {
            if (File("$active/hybrid-mount").isFile) return MetaState.READY
            return MetaState.NEEDS_REBOOT
        }
        // 已安装待重启：落在 modules_update
        if (File("$UPDATES/$META_ID/module.prop").isFile) return MetaState.NEEDS_REBOOT
        return MetaState.MISSING
    }

    fun metaMessage(): String = when (metaState()) {
        MetaState.READY -> "元模块已生效"
        MetaState.NEEDS_REBOOT -> "元模块已安装，重启后生效（重启前无法热刷入）"
        MetaState.MISSING -> "未检测到元模块 Hybrid Mount"
    }

    /** 从模块 zip 里读出 id（module.prop 的 id= 行）。 */
    fun moduleIdOf(zip: File): String? = runCatching {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry("module.prop") ?: return@use null
            zf.getInputStream(entry).bufferedReader().readLines()
                .firstOrNull { it.startsWith("id=") }
                ?.removePrefix("id=")?.trim()?.takeIf { it.isNotEmpty() }
        }
    }.getOrNull()

    /** 按当前架构选择可执行文件（安装后位于模块根目录）。 */
    private fun binary(): String? {
        val dir = "$MODULES/$META_ID"
        listOf(
            "$dir/hybrid-mount",
            "$dir/binaries/hybrid-mount-arm64",
            "$dir/binaries/hybrid-mount-armv7",
            "$dir/binaries/hybrid-mount-x86_64",
            "$dir/binaries/hybrid-mount-riscv64",
        ).firstOrNull { File(it).isFile }?.let { return it }

        val arch = runCatching {
            java.io.File("/proc/cpuinfo").readText()
        }.getOrDefault("")
        val name = when {
            arch.contains("aarch64") || File("/system/lib64").isDirectory -> "arm64"
            arch.contains("x86_64") -> "x86_64"
            arch.contains("riscv") -> "riscv64"
            else -> "armv7"
        }
        return listOf("$dir/binaries/hybrid-mount-$name", "$UPDATES/$META_ID/binaries/hybrid-mount-$name")
            .firstOrNull { File(it).isFile }
    }

    /** 运行 hybrid-mount runtime <args...>。 */
    fun runtime(vararg args: String): String {
        val bin = binary() ?: return "元模块未安装"
        val cmd = buildString {
            append("'").append(bin).append("' runtime ")
            args.forEach { append("'").append(it).append("' ") }
            append("2>&1")
        }
        return HuijuiShellUtils.runAsRoot(cmd, timeoutSeconds = 60, trim = true)
    }

    /**
     * 热激活刚安装的模块。
     */
    fun activate(moduleId: String): Result = when (metaState()) {
        MetaState.MISSING ->
            Result(false, "未检测到 Hybrid Mount 元模块：先安装元模块并重启")
        MetaState.NEEDS_REBOOT ->
            Result(false, "元模块已安装但还没生效：重启一次后即可热刷入")
        MetaState.READY -> {
            val out = runtime("load", moduleId)
            val lower = out.lowercase()
            when {
                lower.contains("reboot required") || lower.contains("full reboot") ||
                    lower.contains("mixed backends") -> Result(false, "需重启：该模块不是纯 VFS 计划")
                lower.contains("not found") -> Result(false, "热激活失败：模块未安装到位")
                lower.contains("panicked") || lower.contains("error") ->
                    Result(false, "热激活失败：${out.take(80)}")
                else -> Result(true, "已热激活（无需重启）")
            }
        }
    }

    /** 运行时状态（用于工具卡片）。 */
    fun status(): String {
        val state = metaMessage()
        if (metaState() != MetaState.READY) return state
        val out = runtime("status")
        return if (out.isBlank()) state else out.lineSequence().take(3).joinToString(" ").take(120)
    }
}
