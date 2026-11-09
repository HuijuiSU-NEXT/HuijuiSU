package org.bakasu.bakasu.huijui.util

import com.topjohnwu.superuser.Shell
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * HuiJuiSU Root 执行工具。
 *
 * 这里是踩坑最多的地方，记录一下结论：
 *
 * 1. 只走 libsu 的 Shell 会在部分设备上**永久阻塞**（su 授权链路异常时），
 *    表现为界面一直「正在安装…」。所以必须多条路径 + 硬超时。
 * 2. 只走 `ProcessBuilder("su")` 在另一些设备上会 EACCES（内核只放行管理器进程）。
 * 3. 因此这里按顺序尝试：
 *      A. `su -c`（最快，已授权时直接成功）
 *      B. `ksud debug su -c`（管理器自带的 root 通道）
 *      C. libsu Shell（最后兜底）
 *    每条路径都有独立超时，全部失败返回 TIMEOUT_MARK。
 */
object HuijuiShellUtils {

    const val TIMEOUT_MARK = "__TIMEOUT__"
    const val NO_ROOT_MARK = "__NO_ROOT__"

    private val pool = Executors.newSingleThreadExecutor { r ->
        Thread(r, "huijui-su").apply { isDaemon = true }
    }

    data class Attempt(val name: String, val output: String)

    private fun execProcess(argv: List<String>, timeoutSeconds: Long): String = try {
        val p = ProcessBuilder(argv).redirectErrorStream(true).start()
        // 关键：不关 stdin，命令只要读一次输入就会永久等待（春秋脚本就踩了这个坑）
        runCatching { p.outputStream.close() }
        val reader = p.inputStream.bufferedReader()
        val sb = StringBuilder()
        val done = pool.submit<Boolean> {
            reader.useLines { lines -> lines.forEach { sb.appendLine(it) } }
            true
        }
        val finished = try {
            done.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (t: TimeoutException) {
            done.cancel(true)
            runCatching { p.destroyForcibly() }
            false
        }
        if (!finished) TIMEOUT_MARK else sb.toString()
    } catch (t: Throwable) {
        "EXEC_FAIL:${t.javaClass.simpleName}:${t.message}"
    }

    private fun execLibsu(cmd: String, timeoutSeconds: Long): String {
        val task = pool.submit<String> {
            val r = Shell.cmd(cmd).exec()
            buildString { r.out.forEach { appendLine(it) }; r.err.forEach { appendLine(it) } }
        }
        return try {
            task.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (t: TimeoutException) {
            task.cancel(true)
            TIMEOUT_MARK
        } catch (t: Throwable) {
            "EXEC_FAIL:${t.javaClass.simpleName}"
        }
    }

    private fun looksBad(out: String): Boolean =
        out.startsWith("EXEC_FAIL") || out.contains("Permission denied") ||
            out.contains("not found") || out.contains(TIMEOUT_MARK)

    /**
     * 依次尝试三条路径，返回 [Attempt]。
     */
    /** 本应用是否已被内核授予 root。未授权时不要发起 su 调用，否则会长时间卡住。 */
    fun isGranted(): Boolean = try {
        Shell.isAppGrantedRoot() == true
    } catch (t: Throwable) {
        false
    }

    // 不同设备上 su 的位置不一样，逐个试。
    // 注意：优先用 `su -c`（直接起 sh），避免 libsu 的 app_process 引导——
    // 那一步会加载 base.art，root 进程读不到 oat 时会报
    // "Invalid image header"，然后 libsu 永久等待。
    private val suPaths = listOf(
        "/system/bin/su",
        "/debug_ramdisk/su",
        "/data/adb/ksu/bin/su",
        "su",
    )

    fun probe(cmd: String, timeoutSeconds: Long = 20): Attempt {
        // 未授权直接返回，避免 libsu / su 卡在等待授权
        if (!isGranted()) return Attempt("none", NO_ROOT_MARK)

        val tried = mutableListOf<String>()
        for (path in suPaths) {
            val r = execProcess(listOf(path, "-c", cmd), timeoutSeconds)
            tried += r
            if (!looksBad(r)) return Attempt("su:$path", r)
        }

        val ksud = execProcess(listOf("/data/adb/ksu/bin/ksud", "debug", "su", "-c", cmd), timeoutSeconds)
        tried += ksud
        if (!looksBad(ksud)) return Attempt("ksud", ksud)

        val libsu = execLibsu(cmd, timeoutSeconds)
        tried += libsu
        if (!looksBad(libsu)) return Attempt("libsu", libsu)

        // 全失败：把最有信息量的结果带回去
        val best = tried.firstOrNull { it != TIMEOUT_MARK && it.isNotBlank() } ?: TIMEOUT_MARK
        return Attempt("none", best)
    }

    fun runAsRoot(cmd: String, timeoutSeconds: Long = 30, trim: Boolean = false): String {
        val out = probe(cmd, timeoutSeconds).output
        return if (trim) out.trim() else out
    }

    fun runAsRootOk(cmd: String, timeoutSeconds: Long = 30): Boolean {
        val r = runAsRoot(cmd, timeoutSeconds, trim = true)
        return r.isNotBlank() && !looksBad(r)
    }

    /** 给 UI 用的诊断信息：哪条路径可用。 */
    fun diagnose(): String {
        val out = StringBuilder()
        out.appendLine("granted : " + if (isGranted()) "true" else "false")
        val a = execProcess(listOf("su", "-c", "id"), 8)
        out.appendLine("su      : " + if (looksBad(a)) a.take(60) else "OK")
        val b = execProcess(listOf("/data/adb/ksu/bin/ksud", "debug", "su", "-c", "id"), 8)
        out.appendLine("ksud    : " + if (looksBad(b)) b.take(60) else "OK")
        val c = execLibsu("id", 8)
        out.appendLine("libsu   : " + if (looksBad(c)) c.take(60) else "OK")
        return out.toString().trim()
    }
}
