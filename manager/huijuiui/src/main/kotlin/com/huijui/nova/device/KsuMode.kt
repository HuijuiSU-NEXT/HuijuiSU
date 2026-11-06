package com.huijui.nova.device

import com.huijui.nova.hide.HideBundle

/**
 * KernelSU 的工作模式探测。
 *
 * 常见的三种形态：
 * - **LKM**：内核里没有内置 KSU，靠 `kernelsu.ko` 加载（`/sys/module/kernelsu` 存在）
 * - **GKI**：KSU 直接编进 GKI 内核镜像，不出现独立 module
 * - **非 GKI / 内核补丁**：老内核手工打补丁，`/data/adb/ksu` 有但 module 与 GKI 都不匹配
 *
 * 判不出来就如实显示 unknown，不猜。
 */
enum class KsuMode(val label: String, val desc: String) {
    NotWorking("未工作", "没有检测到 KernelSU 在运行"),
    Lkm("LKM 模式", "内核模块加载（kernelsu.ko）"),
    Gki("GKI 内置", "KernelSU 编译进内核镜像"),
    Legacy("非 GKI / 补丁", "旧内核手工打补丁"),
    Unknown("未知", "有 ksud 但识别不出模式"),
}

data class KsuStatus(
    val mode: KsuMode = KsuMode.NotWorking,
    val root: Boolean = false,
    val kernel: String = "",
    val ksud: String = "",
    val detail: String = "",
) {
    val working: Boolean get() = mode != KsuMode.NotWorking && mode != KsuMode.Unknown
}

object KsuModeDetector {

    suspend fun detect(): KsuStatus {
        val script = """
            echo "ROOT=${'$'}([ "${'$'}(id -u)" = "0" ] && echo 1 || echo 0)"
            echo "KERNEL=${'$'}(uname -r 2>/dev/null)"
            echo "KSUD=${'$'}(command -v ksud 2>/dev/null || echo '')"
            [ -d /data/adb/ksu ] && echo "KSU_DIR=1" || echo "KSU_DIR=0"
            [ -d /sys/module/kernelsu ] && echo "MODULE=1" || echo "MODULE=0"
            [ -f /data/adb/ksud ] && echo "KSUD_BIN=1" || echo "KSUD_BIN=0"
            [ -d /data/adb/modules ] && echo "MODULES=1" || echo "MODULES=0"
            cat /data/adb/ksu/version 2>/dev/null | head -1
        """.trimIndent()

        val out = HideBundle.run(script).output
        fun flag(key: String) = out.firstOrNull { it.startsWith("$key=") }?.substringAfter("=")
        val root = flag("ROOT") == "1"
        val kernel = flag("KERNEL").orEmpty()
        val ksud = flag("KSUD").orEmpty()
        val ksuDir = flag("KSU_DIR") == "1"
        val moduleLoaded = flag("MODULE") == "1"
        val ksudBin = flag("KSUD_BIN") == "1"
        val version = out.lastOrNull { it.matches(Regex("^\\d+.*")) }.orEmpty()

        val mode = when {
            !ksuDir && !ksudBin && ksud.isBlank() -> KsuMode.NotWorking
            moduleLoaded -> KsuMode.Lkm
            ksuDir && ksudBin -> KsuMode.Gki
            ksuDir -> KsuMode.Legacy
            else -> KsuMode.Unknown
        }

        val detail = buildString {
            if (version.isNotBlank()) append("v").append(version).append(" · ")
            append(if (ksuDir) "ksu 目录已装" else "无 ksu 目录")
            append(" · ")
            append(if (moduleLoaded) "kernelsu 模块已加载" else "无 kernelsu 模块")
        }

        return KsuStatus(
            mode = mode,
            root = root,
            kernel = kernel,
            ksud = ksud.ifBlank { if (ksudBin) "/data/adb/ksud" else "" },
            detail = detail,
        )
    }
}
