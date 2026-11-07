package com.huijui.nova.patches

import com.huijui.nova.hide.HideBundle

/**
 * 内置补丁。
 *
 * 每条 = 一条明确命令 + 可读解释 + 风险等级 + 所属分组，由管理器直接执行。
 * 全部走 resetprop（临时属性），**重启自动恢复**，不写分区。
 */
object Patches {

    enum class Risk { Safe, Caution, Dangerous }

    enum class Group(val label: String) {
        Build("系统属性"),
        Boot("引导状态"),
        Debug("调试与环境"),
        Extra("其他"),
    }

    data class Patch(
        val id: String,
        val name: String,
        val desc: String,
        val command: String,
        val risk: Risk,
        val group: Group,
        val target: String,
        val probe: String,
    )

    val builtin: List<Patch> = listOf(
        // ---- 系统属性 ----
        Patch(
            id = "hide_debuggable",
            name = "隐藏可调试标记",
            desc = "ro.debuggable 置 0、ro.secure 置 1，规避「可调试构建」检测",
            command = "resetprop ro.debuggable 0; resetprop ro.secure 1; getprop ro.debuggable",
            risk = Risk.Safe,
            group = Group.Build,
            target = "ro.debuggable / ro.secure",
            probe = "getprop ro.debuggable",
        ),
        Patch(
            id = "hide_build_tags",
            name = "还原正式构建标签",
            desc = "ro.build.tags 置 release-keys，隐藏 test-keys（自编译 / 改包特征）",
            command = "resetprop ro.build.tags release-keys; getprop ro.build.tags",
            risk = Risk.Safe,
            group = Group.Build,
            target = "ro.build.tags",
            probe = "getprop ro.build.tags",
        ),
        Patch(
            id = "hide_build_type",
            name = "构建类型改为 user",
            desc = "ro.build.type 置 user，隐藏 userdebug（工程机 / 调试包特征）",
            command = "resetprop ro.build.type user; getprop ro.build.type",
            risk = Risk.Safe,
            group = Group.Build,
            target = "ro.build.type",
            probe = "getprop ro.build.type",
        ),
        Patch(
            id = "hide_build_user",
            name = "归一化构建者信息",
            desc = "ro.build.user / ro.build.host 改为通用值，避免暴露编译环境",
            command = "resetprop ro.build.user android-build; resetprop ro.build.host build-host; getprop ro.build.user",
            risk = Risk.Safe,
            group = Group.Build,
            target = "ro.build.user / ro.build.host",
            probe = "getprop ro.build.user",
        ),

        // ---- 引导状态 ----
        Patch(
            id = "hide_boot_state",
            name = "伪装已上锁 BL",
            desc = "verified boot 置 green、flash.locked 置 1，规避解锁检测",
            command = "resetprop ro.boot.verifiedbootstate green; resetprop ro.boot.flash.locked 1; getprop ro.boot.verifiedbootstate",
            risk = Risk.Caution,
            group = Group.Boot,
            target = "ro.boot.verifiedbootstate / ro.boot.flash.locked",
            probe = "getprop ro.boot.verifiedbootstate",
        ),
        Patch(
            id = "hide_verity",
            name = "伪装 dm-verity 正常",
            desc = "ro.boot.veritymode 置 enforcing，隐藏关闭校验的痕迹",
            command = "resetprop ro.boot.veritymode enforcing; getprop ro.boot.veritymode",
            risk = Risk.Caution,
            group = Group.Boot,
            target = "ro.boot.veritymode",
            probe = "getprop ro.boot.veritymode",
        ),
        Patch(
            id = "hide_vbmeta",
            name = "伪装 vbmeta 已锁定",
            desc = "ro.boot.vbmeta.device_state 置 locked，配合 BL 伪装",
            command = "resetprop ro.boot.vbmeta.device_state locked; getprop ro.boot.vbmeta.device_state",
            risk = Risk.Caution,
            group = Group.Boot,
            target = "ro.boot.vbmeta.device_state",
            probe = "getprop ro.boot.vbmeta.device_state",
        ),

        // ---- 调试与环境 ----
        Patch(
            id = "hide_adb_root",
            name = "关闭 adb root 标记",
            desc = "service.adb.root 置 0，避免被判定为调试环境",
            command = "resetprop service.adb.root 0; getprop service.adb.root",
            risk = Risk.Safe,
            group = Group.Debug,
            target = "service.adb.root",
            probe = "getprop service.adb.root",
        ),
        Patch(
            id = "hide_qemu",
            name = "抹掉模拟器特征",
            desc = "ro.kernel.qemu 置空、ro.hardware 归一，避免被识别为模拟器",
            command = "resetprop ro.kernel.qemu \"\"; resetprop ro.hardware qcom; getprop ro.hardware",
            risk = Risk.Caution,
            group = Group.Debug,
            target = "ro.kernel.qemu / ro.hardware",
            probe = "getprop ro.hardware",
        ),
        Patch(
            id = "hide_secure",
            name = "恢复 ro.secure",
            desc = "ro.secure 置 1，部分检测会用它判断是否被改过",
            command = "resetprop ro.secure 1; getprop ro.secure",
            risk = Risk.Safe,
            group = Group.Debug,
            target = "ro.secure",
            probe = "getprop ro.secure",
        ),

        // ---- 其他 ----
        Patch(
            id = "reboot_now",
            name = "重启使补丁失效",
            desc = "所有 resetprop 修改重启即恢复；这里直接重启设备",
            command = "reboot",
            risk = Risk.Dangerous,
            group = Group.Extra,
            target = "设备重启",
            probe = "echo -",
        ),
        Patch(
            id = "list_applied",
            name = "查看已修改属性",
            desc = "列出上文涉及的关键属性当前值，确认补丁是否还在",
            command = "for p in ro.debuggable ro.secure ro.build.tags ro.build.type " +
                "ro.boot.verifiedbootstate ro.boot.veritymode service.adb.root ro.hardware; " +
                "do echo \"${'$'}p = ${'$'}(getprop ${'$'}p)\"; done",
            risk = Risk.Safe,
            group = Group.Extra,
            target = "只读检查",
            probe = "echo -",
        ),
    )

    data class Result(val ok: Boolean, val output: List<String>)

    suspend fun apply(patch: Patch): Result {
        val result = HideBundle.run(patch.command)
        return Result(result.ok, result.output)
    }

    /** 读取当前值（用于显示「已应用 / 未应用」）。 */
    suspend fun currentValue(patch: Patch): String {
        val out = HideBundle.run(patch.probe).output.firstOrNull()?.trim().orEmpty()
        return when {
            out.startsWith("执行失败") -> "不可读（无 Root）"
            out.isBlank() -> "-"
            else -> out
        }
    }
}
