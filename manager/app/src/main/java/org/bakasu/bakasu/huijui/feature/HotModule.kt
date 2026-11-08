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
 * 约束（来自它自己的实现）：
 * - 纯 VFS 文件计划的模块可以热加载
 * - 混合后端（Magic / Overlay）的模块仍然需要重启
 * - 元模块本身没装好时，热加载不可用
 */
object HotModule {

    private const val META_DIR = "/data/adb/modules/hybrid_mount"
    private const val RUNTIME_DIR = "/data/adb/hybrid-mount"

    data class Result(val ok: Boolean, val message: String)

    /** 从模块 zip 里读出 id（module.prop 的 id= 行）。 */
    fun moduleIdOf(zip: File): String? = runCatching {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry("module.prop") ?: return@use null
            zf.getInputStream(entry).bufferedReader().readLines()
                .firstOrNull { it.startsWith("id=") }
                ?.removePrefix("id=")?.trim()?.takeIf { it.isNotEmpty() }
        }
    }.getOrNull()

    /** 元模块是否已就绪。 */
    fun metaReady(): Boolean = File("$META_DIR/module.prop").isFile &&
        !File("$META_DIR/disable").exists()

    /** 按当前架构选择 hybrid-mount 可执行文件。 */
    private fun binary(): String {
        val runtime = File("$META_DIR/hybrid-mount")
        if (runtime.isFile) return runtime.absolutePath
        val arch = runCatching {
            ProcessBuilder("uname", "-m").start().inputStream.bufferedReader().readText().trim()
        }.getOrDefault("")
        val name = when {
            arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
            arch.startsWith("armv7") || arch.contains("armv8l") -> "armv7"
            arch.contains("x86_64") -> "x86_64"
            arch.contains("riscv64") -> "riscv64"
            else -> "arm64"
        }
        return "$META_DIR/binaries/hybrid-mount-$name"
    }

    /** 运行 hybrid-mount runtime <args...>，返回输出。 */
    fun runtime(vararg args: String): String {
        val bin = binary()
        if (!File(bin).isFile) return "元模块未就绪"
        val cmd = buildString {
            append("'").append(bin).append("' runtime ")
            args.forEach { append("'").append(it).append("' ") }
            append("2>&1")
        }
        return HuijuiShellUtils.runAsRoot(cmd, timeoutSeconds = 60, trim = true)
    }

    /**
     * 热激活刚安装的模块。
     *
     * @return ok=true 表示已在不重启的前提下生效
     */
    fun activate(moduleId: String): Result {
        if (!metaReady()) {
            return Result(false, "元模块未安装：装好 Hybrid Mount 后即可热激活")
        }
        val out = runtime("load", moduleId)
        val lower = out.lowercase()
        return when {
            lower.contains("reboot required") || lower.contains("full reboot") ||
                lower.contains("mixed backends") -> Result(false, "需重启：该模块不是纯 VFS 计划")
            lower.contains("not found") || lower.contains("installed module not found") ->
                Result(false, "热激活失败：模块未安装到位")
            lower.contains("error") || lower.contains("panicked") -> Result(false, "热激活失败：$out")
            else -> Result(true, "已热激活（无需重启）")
        }
    }

    /** 运行时状态（用于设置页/诊断展示）。 */
    fun status(): String = runtime("status")
}
