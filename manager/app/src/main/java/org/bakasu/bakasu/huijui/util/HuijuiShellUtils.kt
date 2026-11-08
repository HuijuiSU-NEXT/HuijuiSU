package org.bakasu.bakasu.huijui.util

import com.topjohnwu.superuser.Shell

/**
 * HuiJuiSU 公共 Root Shell 执行工具。
 *
 * 重要：这里必须走 libsu 的 [Shell]，**不要**直接用 `ProcessBuilder("su", ...)`。
 *
 * 原因：KernelSU 系的内核只对「被认可的管理器进程」放行 su，
 * 应用进程直接 exec `su` 会拿到 `EACCES (Permission denied)`；
 * libsu 走的是管理器自己建立的 root shell（ksud debug su），不受这个限制。
 *
 * 之前「一键隐藏 / Evil 自查 / 元模块 / 春秋修复」这些卡片全部提示没有 root，
 * 就是因为它们走的是 ProcessBuilder 那条路。
 */
object HuijuiShellUtils {

    /**
     * 以 Root 权限执行命令，返回 stdout + stderr 合并输出。
     *
     * @param cmd 完整 shell 命令
     * @param timeoutSeconds 预留给调用方说明用；需要硬超时请用 `timeout N` 包住命令
     * @param trim 是否对输出做 trim
     */
    fun runAsRoot(
        cmd: String,
        timeoutSeconds: Long = 30,
        trim: Boolean = false,
    ): String = try {
        val result = Shell.cmd(cmd).exec()
        val out = buildString {
            result.out.forEach { appendLine(it) }
            result.err.forEach { appendLine(it) }
        }
        if (trim) out.trim() else out
    } catch (t: Throwable) {
        t.message ?: ""
    }

    /** 命令是否执行成功（退出码 0）。 */
    fun runAsRootOk(cmd: String): Boolean = try {
        Shell.cmd(cmd).exec().isSuccess
    } catch (t: Throwable) {
        false
    }
}
