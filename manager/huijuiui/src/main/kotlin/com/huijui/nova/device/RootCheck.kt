package com.huijui.nova.device

import com.huijui.nova.hide.HideBundle

/**
 * Root 能力检测。
 *
 * 所有依赖 root 的功能（模块 / 超级用户 / 体检 / 刷写 / 补丁 / SUSFS）都先问这里，
 * 避免「点了没反应」——拿不到 root 时界面必须明说原因。
 *
 * 三种状态：
 * - Granted：`su -c id` 返回 uid=0
 * - Denied：su 存在但被拒绝（管理器未被内核授权 / 用户拒绝）
 * - Unavailable：连 su 都执行不了（没装 KernelSU，或 SELinux 拒绝 exec）
 */
object RootCheck {

    enum class State { Unknown, Granted, Denied, Unavailable }

    @Volatile
    var state: State = State.Unknown
        private set

    @Volatile
    var detail: String = ""
        private set

    /** 人类可读的结论，UI 直接展示。 */
    val message: String
        get() = when (state) {
            State.Granted -> "已获得 root"
            State.Denied -> "root 被拒绝：请在 KernelSU 管理器里给本应用授权"
            State.Unavailable ->
                "无法调用 su：本应用未被内核识别为管理器，或设备没有 KernelSU"
            State.Unknown -> "正在检测 root…"
        }

    suspend fun check(): State {
        val r = HideBundle.run("id")
        val out = r.output.joinToString(" ").trim()
        state = when {
            out.contains("uid=0") -> State.Granted
            out.contains("Permission denied") || out.contains("exec") -> {
                detail = out.take(120)
                State.Unavailable
            }
            r.ok -> {
                detail = out.take(120)
                State.Denied
            }
            else -> {
                detail = out.take(120)
                State.Unavailable
            }
        }
        return state
    }
}
