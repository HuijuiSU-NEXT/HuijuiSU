package com.huijui.nova.checkup

import com.huijui.nova.detect.ChunqiuCheck
import com.huijui.nova.device.KsuMode
import com.huijui.nova.device.KsuModeDetector
import com.huijui.nova.hide.HideBundle

/**
 * 一键体检。
 *
 * 把一个 root 用户真正关心的东西合成一份报告：
 * 1. KernelSU 是否在工作、什么模式
 * 2. 模块有没有互相覆盖同一个文件（这是bootloop 最常见的原因）
 * 3. 隐藏检测命中多少项
 * 4. 有没有可回滚的 boot 备份
 * 5. 内核是否 4.x（决定能不能用 LKM）
 *
 * 同类管理器都没有这个：它们只列清单，不告诉你「哪里会出事」。
 */
object Checkup {

    enum class Level { Good, Warn, Bad, Info }

    data class Item(
        val title: String,
        val detail: String,
        val level: Level,
        val action: String = "",
        val actionKey: String = "",
    )

    data class Report(
        val score: Int = 0,
        val items: List<Item> = emptyList(),
        val running: Boolean = false,
    ) {
        val bad: Int get() = items.count { it.level == Level.Bad }
        val warn: Int get() = items.count { it.level == Level.Warn }
        val summary: String
            get() = when {
                bad > 0 -> "$bad 项需要处理"
                warn > 0 -> "$warn 项建议关注"
                items.isEmpty() -> "尚未体检"
                else -> "状态良好"
            }
    }

    suspend fun run(context: android.content.Context, onItem: ((Item) -> Unit)? = null): Report {
        val items = mutableListOf<Item>()

        fun add(item: Item) {
            items += item
            onItem?.invoke(item)
        }

        // 1. KSU 状态
        val ksu = KsuModeDetector.detect()
        add(
            when (ksu.mode) {
                KsuMode.Lkm, KsuMode.Gki -> Item(
                    "KernelSU 正常",
                    "${ksu.mode.label} · 内核 ${ksu.kernel.ifBlank { "?" }}",
                    Level.Good,
                )
                KsuMode.NonGki4x -> Item(
                    "4.x 非 GKI 内核",
                    "不能用 LKM，需要内核补丁或修补 boot",
                    Level.Warn,
                    "进入兼容模式",
                    "legacy",
                )
                KsuMode.NotWorking -> Item(
                    "KernelSU 未工作",
                    "没检测到 ksu 目录或 ksud",
                    Level.Bad,
                )
                else -> Item("KernelSU 状态未知", ksu.detail, Level.Warn)
            },
        )

        // 2. 模块冲突
        val modules = HideBundle.run(
            """
            for d in /data/adb/modules/*/; do
              id=${'$'}(basename "${'$'}d")
              [ -d "${'$'}d/system" ] || continue
              ( cd "${'$'}d" && find system -type f 2>/dev/null ) | sed "s|^|${'$'}id |"
            done
            """.trimIndent(),
        )
        val byPath = modules.output
            .mapNotNull { line ->
                val idx = line.indexOf(' ')
                if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
            }
            .groupBy({ it.second }, { it.first })
            .filterValues { it.size > 1 }

        if (byPath.isNotEmpty()) {
            val sample = byPath.entries.take(2).joinToString("；") { (path, ids) ->
                "${ids.joinToString("/")} 同时覆盖 $path"
            }
            add(
                Item(
                    "模块文件冲突（${byPath.size} 处）",
                    "$sample —— 后加载的会盖掉前一个，可能开不了机",
                    Level.Bad,
                    "看模块",
                    "modules",
                ),
            )
        } else {
            add(Item("模块无冲突", "没有两个模块覆盖同一文件", Level.Good))
        }

        // 3. 隐藏检测
        val report = ChunqiuCheck.run(context)
        val hits = report.hits
        add(
            if (hits == 0) {
                Item("隐藏状态干净", "春秋检测 0 命中", Level.Good)
            } else {
                Item(
                    "隐藏检测命中 $hits 项",
                    "系统属性 / 挂载 / maps 等特征暴露",
                    if (hits > 8) Level.Bad else Level.Warn,
                    "看报告",
                    "detect",
                )
            },
        )

        if (hits > 0) {
            add(
                Item(
                    "有 ${com.huijui.nova.patches.Patches.builtin.count { it.risk != com.huijui.nova.patches.Patches.Risk.Dangerous }} 个补丁可尝试",
                    "针对属性与引导状态的一键伪装，重启自动恢复",
                    Level.Info,
                    "补丁中心",
                    "patches",
                ),
            )
        }

        // 4. boot 备份
        val backup = HideBundle.run(
            "ls -d /sdcard/HuiJuiSU_Backup_* 2>/dev/null | tail -1",
        )
        val dir = backup.output.firstOrNull { it.startsWith("/sdcard") }.orEmpty()
        add(
            if (dir.isNotBlank()) {
                Item("有 boot 备份", dir, Level.Good)
            } else {
                Item(
                    "没有 boot 备份",
                    "刷坏后无法快速回滚，建议先备份",
                    Level.Warn,
                    "去刷写",
                    "flash",
                )
            },
        )

        // 5. 模块数量提示
        val count = HideBundle.run("ls /data/adb/modules 2>/dev/null | wc -l")
            .output.firstOrNull()?.trim()?.toIntOrNull() ?: 0
        if (count >= 15) {
            add(Item("模块较多（$count 个）", "启动变慢、冲突概率上升", Level.Warn, "看模块", "modules"))
        }

        // 评分：每项 Bad -25，Warn -10，Good 基线 100
        val score = (100 -
            items.count { it.level == Level.Bad } * 25 -
            items.count { it.level == Level.Warn } * 10).coerceIn(0, 100)

        return Report(score = score, items = items)
    }
}
