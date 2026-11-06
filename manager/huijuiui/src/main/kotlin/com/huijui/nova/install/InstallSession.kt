package com.huijui.nova.install

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 刷入流程的「人话」模型。
 *
 * 原来这些流程只有一屏滚动的日志，用户看不到自己在第几步、还要多久、卡在哪。
 * 这里把它拆成固定的几个阶段，每个阶段有标题、说明、状态和耗时——UI 只读这个模型，
 * 原始日志退到「查看日志」里按需展开。
 */
enum class InstallMode(val label: String, val description: String) {
    Direct("直接安装", "把内核写进当前系统槽位"),
    PatchImage("修补镜像", "只生成修补后的镜像，不写盘"),
    Module("刷入模块", "把模块包解到 /data/adb/modules"),
}

enum class StageStatus { Pending, Running, Success, Failed, Skipped }

data class InstallStage(
    /** 稳定 key，方便测试与日志对照。 */
    val key: String,
    val title: String,
    val hint: String,
)

data class StageState(
    val stage: InstallStage,
    val status: StageStatus = StageStatus.Pending,
    val detail: String? = null,
    val elapsedMs: Long = 0L,
) {
    val isDone: Boolean get() = status == StageStatus.Success || status == StageStatus.Skipped
}

/** 需要用户做选择时的提示（音量键选择器的可点版本）。 */
data class InstallPrompt(
    val id: Int,
    val question: String,
    val options: List<String>,
    /** 音量键提示，例如「音量上 = 是 · 音量下 = 否 · 电源键 = 确认」。 */
    val keyHint: String,
    /** 时长上限（毫秒）。0 表示一直等。 */
    val timeoutMs: Long = 0L,
    val raw: String = "",
)

object InstallStages {

    val direct = listOf(
        InstallStage("env", "检查环境", "确认 Root 通道、分区表与剩余空间"),
        InstallStage("extract", "解包镜像", "从当前槽位提取 boot / init_boot"),
        InstallStage("patch", "修补内核", "写入内核与 init 钩子"),
        InstallStage("flash", "回写分区", "把修补结果写回目标分区"),
        InstallStage("verify", "校验", "核对哈希与分区大小"),
    )

    val patchImage = listOf(
        InstallStage("env", "检查环境", "确认 rootfs 与输出目录"),
        InstallStage("extract", "解包镜像", "读取待修补的镜像"),
        InstallStage("patch", "修补内核", "写入内核与 init 钩子"),
        InstallStage("verify", "校验", "核对哈希"),
        InstallStage("export", "导出结果", "写到 Download 目录"),
    )

    val module = listOf(
        InstallStage("inspect", "解析模块包", "读取 module.prop 与脚本"),
        InstallStage("check", "兼容性检查", "核对 KMI、架构与依赖"),
        InstallStage("unpack", "解包到 /data/adb", "写入模块目录"),
        InstallStage("scripts", "执行安装脚本", "customize.sh 与 sepolicy"),
        InstallStage("verify", "校验", "确认模块已被管理器识别"),
    )

    fun of(mode: InstallMode): List<InstallStage> = when (mode) {
        InstallMode.Direct -> direct
        InstallMode.PatchImage -> patchImage
        InstallMode.Module -> module
    }
}

/**
 * 一次刷入会话的可观察状态。
 *
 * 只做三件事：阶段推进、日志留存（环形）、等待用户选择。
 * 真实执行由外部 runner 驱动，UI 不关心命令怎么跑。
 */
@Stable
class InstallSessionState(val mode: InstallMode) {

    val stages: List<InstallStage> = InstallStages.of(mode)

    var stageStates: List<StageState> by mutableStateOf(stages.map { StageState(it) })
        private set

    var logs: List<String> by mutableStateOf(emptyList())
        private set

    var prompt: InstallPrompt? by mutableStateOf(null)
        private set

    var failure: String? by mutableStateOf(null)
        private set

    var cancelled: Boolean by mutableStateOf(false)
        private set

    val finished: Boolean
        get() = failure != null || cancelled || stageStates.all { it.isDone }

    val overallProgress: Float
        get() {
            if (stages.isEmpty()) return 0f
            val done = stageStates.count { it.isDone }
            val running = stageStates.count { it.status == StageStatus.Running }
            return ((done + running * 0.5f) / stages.size).coerceIn(0f, 1f)
        }

    val currentStage: StageState?
        get() = stageStates.firstOrNull { it.status == StageStatus.Running }

    fun start() {
        update(0) { it.copy(status = StageStatus.Running) }
        log("开始：${mode.label}")
    }

    fun running(index: Int, detail: String? = null) {
        update(index) { it.copy(status = StageStatus.Running, detail = detail) }
    }

    fun succeed(index: Int, detail: String? = null, elapsedMs: Long = 0L) {
        update(index) { it.copy(status = StageStatus.Success, detail = detail ?: it.detail, elapsedMs = elapsedMs) }
        log("完成：${stages[index].title}")
    }

    fun fail(message: String, index: Int? = null) {
        failure = message
        val target = index ?: stageStates.indexOfFirst { it.status == StageStatus.Running }
        if (target >= 0) {
            update(target) { it.copy(status = StageStatus.Failed, detail = message) }
        }
        log("失败：$message")
    }

    fun skip(index: Int, reason: String) {
        update(index) { it.copy(status = StageStatus.Skipped, detail = reason) }
    }

    fun cancel() {
        cancelled = true
        prompt = null
        log("已取消")
    }

    fun log(line: String) {
        // 只留最近 200 行，日志是诊断用的，不是给人从第一行读的
        logs = (logs + line).takeLast(MAX_LOG_LINES)
    }

    /** 收到音量键提示：转成可点的选择卡片。 */
    fun raisePrompt(prompt: InstallPrompt) {
        this.prompt = prompt
        log("需要选择：${prompt.question}")
    }

    /** 用户点了按钮（或按了音量键，由 runner 转成同一入口）。 */
    fun answerPrompt(optionIndex: Int) {
        val current = prompt ?: return
        log("选择：${current.options.getOrElse(optionIndex) { "?" }}")
        prompt = null
    }

    fun dismissPrompt() {
        prompt = null
    }

    private fun update(index: Int, block: (StageState) -> StageState) {
        if (index !in stageStates.indices) return
        stageStates = stageStates.toMutableList().also { it[index] = block(it[index]) }
    }

    private companion object {
        const val MAX_LOG_LINES = 200
    }
}

/**
 * 从 ksud / 安装脚本的输出里识别「请按音量键选择」。
 *
 * 真实安装里这类提示混在日志中间，用户往往已经错过了按键窗口。
 * 这里把它抓出来变成 [InstallPrompt]，UI 就能弹按钮让用户直接点。
 */
object PromptParser {

    private val questionRegex = Regex("""音量(上|下)键?\s*(?:选择)?\s*[:：]\s*([^，。;；\n]+)""")
    private val englishQuestionRegex = Regex("""(?i)volume\s*(up|down)\s*[:,]?\s*([^,.;\n]+)""")

    private const val KEY_HINT_ZH = "音量上 = 是 · 音量下 = 否 · 电源键 = 确认"
    private const val KEY_HINT_EN = "Volume Up = Yes · Volume Down = No · Power = Confirm"

    /** 判断这一行是不是在选择提示。 */
    fun isPrompt(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("音量") && (lower.contains("上键") || lower.contains("下键") || lower.contains("选择")) ||
            (lower.contains("volume") && (lower.contains("up") || lower.contains("down")))
    }

    /**
     * 解析出问题与选项。识别不出选项时退回「是 / 否」，
     * 宁可多给两个按钮，也不要让用户对着日志猜。
     */
    fun parse(line: String, id: Int): InstallPrompt? {
        if (!isPrompt(line)) return null
        val zh = questionRegex.findAll(line).toList()
        val en = englishQuestionRegex.findAll(line).toList()

        val options = when {
            zh.isNotEmpty() -> zh.map { it.groupValues[2].trim() }
            en.isNotEmpty() -> en.map { it.groupValues[2].trim() }
            else -> emptyList()
        }.filter { it.isNotBlank() }.distinct()

        val question = when {
            zh.isNotEmpty() -> zh.first().groupValues[2].trim()
            en.isNotEmpty() -> en.first().groupValues[2].trim()
            else -> line.trim()
        }

        val english = en.isNotEmpty() && zh.isEmpty()
        return InstallPrompt(
            id = id,
            question = question,
            options = options.ifEmpty { listOf("是", "否") }.take(4),
            keyHint = if (english) KEY_HINT_EN else KEY_HINT_ZH,
            raw = line,
        )
    }

    /** 扫描一段输出，取出第一条提示。 */
    fun firstPrompt(lines: List<String>, startId: Int = 1): InstallPrompt? {
        lines.forEachIndexed { index, line ->
            parse(line, startId + index)?.let { return it }
        }
        return null
    }
}
