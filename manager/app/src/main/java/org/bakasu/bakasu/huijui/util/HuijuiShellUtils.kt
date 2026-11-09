package org.bakasu.bakasu.huijui.util

import com.topjohnwu.superuser.Shell
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * HuiJuiSU 公共 Root Shell 执行工具。
 *
 * 设计要点（都是踩过坑之后加的）：
 *
 * 1. 走 libsu 的 [Shell]，不要直接 `ProcessBuilder("su")` —— 应用进程 exec su 会被内核拒绝。
 * 2. **必须带超时**。libsu 的 `exec()` 在内核无响应、su 授权弹窗未处理时会永久阻塞，
 *    表现为界面一直「正在安装…」停不下来。这里用独立线程 + Future 超时兜底。
 * 3. 超时后返回 `TIMEOUT`，调用方据此提示用户，而不是无限等待。
 */
object HuijuiShellUtils {

    const val TIMEOUT_MARK = "__TIMEOUT__"

    // 单线程执行：KernelSU 对并发 su 请求会排队甚至互相阻塞，
    // 串行执行 + 超时，才不会出现多个卡片同时卡在「正在…」。
    private val pool = Executors.newSingleThreadExecutor { r ->
        Thread(r, "huijui-su").apply { isDaemon = true }
    }

    /**
     * 以 Root 权限执行命令，返回 stdout + stderr 合并输出。
     * 超时返回 [TIMEOUT_MARK]，执行异常返回异常信息。
     */
    fun runAsRoot(
        cmd: String,
        timeoutSeconds: Long = 30,
        trim: Boolean = false,
    ): String {
        val task = pool.submit<String> {
            val result = Shell.cmd(cmd).exec()
            buildString {
                result.out.forEach { appendLine(it) }
                result.err.forEach { appendLine(it) }
            }
        }
        val out = try {
            task.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (t: TimeoutException) {
            task.cancel(true)
            TIMEOUT_MARK
        } catch (t: Throwable) {
            t.cause?.message ?: t.message ?: ""
        }
        return if (trim) out.trim() else out
    }

    /** 命令是否执行成功（退出码 0），超时视为失败。 */
    fun runAsRootOk(cmd: String, timeoutSeconds: Long = 30): Boolean {
        val r = runAsRoot(cmd, timeoutSeconds, trim = true)
        return r.isNotBlank() && r != TIMEOUT_MARK && !r.contains("Permission denied")
    }
}
