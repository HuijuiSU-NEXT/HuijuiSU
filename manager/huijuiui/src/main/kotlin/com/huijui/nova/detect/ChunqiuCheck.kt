package com.huijui.nova.detect

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader

/**
 * 春秋 Native Check 检测点复刻（内置）。
 *
 * 脚本本身原样打进 APK 的 `assets/detect/chunqiu_check.sh`，
 * 运行时解到 filesDir 再用 `su -c sh` 执行——不在 Kotlin 里重写一遍检测逻辑，
 * 作者更新脚本只要换 assets 里的文件即可。
 */
object ChunqiuCheck {

    const val ASSET = "detect/chunqiu_check.sh"
    private const val FILE_NAME = "chunqiu_check.sh"

    /** 一行输出归类。 */
    enum class Kind { Section, Found, Ok, Note, Plain }

    data class Line(val kind: Kind, val text: String, val section: String)

    data class Report(
        val lines: List<Line> = emptyList(),
        val hits: Int = 0,
        val groups: Int = 0,
        val exitCode: Int = -1,
        val raw: String = "",
    ) {
        val found: List<Line> get() = lines.filter { it.kind == Kind.Found }
    }

    private fun ensureScript(context: Context): File? = runCatching {
        val target = File(context.filesDir, FILE_NAME)
        val needsCopy = !target.exists() || target.length() == 0L
        if (needsCopy) {
            context.assets.open(ASSET).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        target.setExecutable(true)
        target
    }.getOrNull()

    /**
     * 执行检测。
     *
     * @param quiet 只列命中项（脚本自带 --quiet）
     * @param pid   指定被检测进程（可选）
     * @param onLine 流式回调，界面可以边跑边显示
     */
    suspend fun run(
        context: Context,
        quiet: Boolean = false,
        pid: String? = null,
        onLine: ((Line) -> Unit)? = null,
    ): Report = withContext(Dispatchers.IO) {
        val script = ensureScript(context) ?: return@withContext Report(
            lines = listOf(Line(Kind.Plain, "内置脚本解压失败", "")),
        )

        val args = buildString {
            append("sh '").append(script.absolutePath).append("'")
            if (quiet) append(" --quiet")
            if (!pid.isNullOrBlank()) append(" --pid ").append(pid)
        }

        // 先试 root；失败就退回普通 shell —— 脚本本身支持非 root 运行（结果打折但能跑）
        suspend fun exec(cmd: List<String>): Report {
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val out = StringBuilder()
            val parsed = mutableListOf<Line>()
            var section = ""
            var hits = 0
            var groups = 0

            InputStreamReader(process.inputStream).buffered().forEachLine { rawLine ->
                out.appendLine(rawLine)
                val line = rawLine.trimEnd()
                val item = when {
                    line.matches(Regex("^\\[\\d{2}] .*")) -> {
                        groups += 1
                        section = line.substringAfter("] ").trim()
                        Line(Kind.Section, section, section)
                    }
                    line.trimStart().startsWith("[FOUND]") -> {
                        hits += 1
                        Line(Kind.Found, line.trimStart().removePrefix("[FOUND]").trim(), section)
                    }
                    line.trimStart().startsWith("[ ok  ]") ->
                        Line(Kind.Ok, line.trimStart().removePrefix("[ ok  ]").trim(), section)
                    line.trimStart().startsWith("[ note]") ->
                        Line(Kind.Note, line.trimStart().removePrefix("[ note]").trim(), section)
                    else -> Line(Kind.Plain, line, section)
                }
                parsed += item
                onLine?.invoke(item)
            }

            val code = process.waitFor()
            return Report(lines = parsed, hits = hits, groups = groups, exitCode = code, raw = out.toString())
        }

        runCatching {
            val rooted = runCatching { exec(listOf("su", "-c", args)) }.getOrNull()
            if (rooted != null && rooted.groups > 0) return@withContext rooted
            exec(listOf("sh", "-c", args))
        }.getOrElse { error ->
            Report(
                lines = listOf(Line(Kind.Plain, "执行失败：${error.message}", "")),
                raw = "",
            )
        }
    }

    /** 结论行（脚本最后几行）。 */
    fun Report.verdict(): String {
        val tail = lines.filter { it.kind == Kind.Plain }.takeLast(4).joinToString(" ") { it.text }
        return tail.ifBlank { if (hits == 0) "没有命中任何检测点" else "命中 $hits 项" }
    }
}
