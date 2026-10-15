package org.bakasu.bakasu.huijui.util

import java.util.concurrent.TimeUnit

/**
 * HuiJuiSU Next 公共 Root Shell 执行工具
 *
 * 统一 ProcessBuilder + su -c 的调用方式，避免在多个文件里重复实现。
 */
object HuijuiShellUtils {

    /**
     * 以 Root 权限执行命令，返回 stdout + stderr 合并输出。
     *
     * @param cmd 完整 shell 命令
     * @param timeoutSeconds 超时秒数，默认 30 秒
     * @param trim 是否对输出做 trim，默认 false
     * @return 命令输出；执行异常时返回 t.message 或空串
     */
    fun runAsRoot(
        cmd: String,
        timeoutSeconds: Long = 30,
        trim: Boolean = false,
    ): String = try {
        val p = ProcessBuilder("su", "-c", cmd)
            .redirectErrorStream(true)
            .start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (trim) out.trim() else out
    } catch (t: Throwable) {
        t.message ?: ""
    }
}
