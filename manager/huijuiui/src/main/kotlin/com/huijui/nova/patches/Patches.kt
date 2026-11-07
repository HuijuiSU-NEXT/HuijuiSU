package com.huijui.nova.patches

import com.huijui.nova.hide.HideBundle

/**
 * 内置补丁。
 *
 * 每个补丁都是「一条明确命令 + 可读的解释 + 风险等级」，由管理器直接执行，
 * 用户不需要背 resetprop 的参数。这是和「只给清单」的管理器最大的差别。
 *
 * 全部走 resetprop，**重启即恢复**，不会写死到分区里。
 */
object Patches {

    enum class Risk { Safe, Caution, Dangerous }

    data class Patch(
        val id: String,
        val name: String,
        val desc: String,
        val command: String,
        val risk: Risk,
        val target: String,
    )

    /** resetprop 优先用 ksud 自带的，退回 PATH 里的。 */
    private const val RP = "(command -v resetprop >/dev/null 2>&1 && echo resetprop || echo ksud-resetprop)"

    val builtin: List<Patch> = listOf(
        Patch(
            id = "hide_debuggable",
            name = "隐藏可调试标记",
            desc = "把 ro.debuggable 置 0、ro.secure 置 1，规避「可调试构建」检测",
            command = "resetprop ro.debuggable 0; resetprop ro.secure 1; getprop ro.debuggable; getprop ro.secure",
            risk = Risk.Safe,
            target = "ro.debuggable / ro.secure",
        ),
        Patch(
            id = "hide_build_tags",
            name = "还原正式构建标签",
            desc = "ro.build.tags 置 release-keys，隐藏 test-keys（自编译/改包特征）",
            command = "resetprop ro.build.tags release-keys; getprop ro.build.tags",
            risk = Risk.Safe,
            target = "ro.build.tags",
        ),
        Patch(
            id = "hide_boot_state",
            name = "伪装已上锁 BL",
            desc = "verified boot 置 green、flash.locked 置 1，规避解锁检测",
            command = "resetprop ro.boot.verifiedbootstate green; resetprop ro.boot.flash.locked 1; getprop ro.boot.verifiedbootstate",
            risk = Risk.Caution,
            target = "ro.boot.verifiedbootstate / ro.boot.flash.locked",
        ),
        Patch(
            id = "hide_adb_root",
            name = "关闭 adb root 标记",
            desc = "service.adb.root 置 0，避免被判定为调试环境",
            command = "resetprop service.adb.root 0; getprop service.adb.root",
            risk = Risk.Safe,
            target = "service.adb.root",
        ),
        Patch(
            id = "hide_qemu",
            name = "抹掉模拟器特征",
            desc = "ro.kernel.qemu / ro.hardware 置空，避免被识别为模拟器",
            command = "resetprop ro.kernel.qemu \"\"; resetprop ro.hardware qcom; getprop ro.hardware",
            risk = Risk.Caution,
            target = "ro.kernel.qemu / ro.hardware",
        ),
        Patch(
            id = "restore_all",
            name = "撤销全部补丁",
            desc = "重启后所有 resetprop 修改自动失效；这里给出立即重启入口",
            command = "echo '重启即可恢复所有临时属性修改'",
            risk = Risk.Safe,
            target = "resetprop",
        ),
    )

    data class Result(val ok: Boolean, val output: List<String>)

    suspend fun apply(patch: Patch): Result {
        val result = HideBundle.run(patch.command)
        return Result(result.ok, result.output)
    }

    /** 读取某个补丁当前的值（用于显示「已应用/未应用」）。 */
    suspend fun currentValue(patch: Patch): String {
        val probe = when (patch.id) {
            "hide_debuggable" -> "getprop ro.debuggable"
            "hide_build_tags" -> "getprop ro.build.tags"
            "hide_boot_state" -> "getprop ro.boot.verifiedbootstate"
            "hide_adb_root" -> "getprop service.adb.root"
            "hide_qemu" -> "getprop ro.hardware"
            else -> "echo -"
        }
        val out = HideBundle.run(probe).output.firstOrNull()?.trim().orEmpty()
        return when {
            out.startsWith("执行失败") -> "不可读（无 Root）"
            out.isBlank() -> "-"
            else -> out
        }
    }
}
