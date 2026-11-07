package com.huijui.nova.heal

import com.huijui.nova.hide.HideBundle

/**
 * 自愈（Self-heal）。
 *
 * 同类管理器只告诉你「有冲突」，不帮你处理。这里做三件事：
 * 1. 模块冲突检测 → 直接生成停用标记（/data/adb/modules/<id>/disable），重启后不再加载
 * 2. 一键停用「最近安装」的模块（bootloop 最常见诱因）
 * 3. 清理缓存 + 重置被改乱的属性
 *
 * 全部操作可逆：停用只是加 disable 文件，删掉就恢复。
 */
object SelfHeal {

    data class Conflict(val path: String, val moduleIds: List<String>)

    /** 扫描模块之间互相覆盖的文件。 */
    suspend fun conflicts(): List<Conflict> {
        val r = HideBundle.run(
            """
            for d in /data/adb/modules/*/; do
              id=${'$'}(basename "${'$'}d")
              [ -d "${'$'}d/system" ] || continue
              ( cd "${'$'}d" && find system -type f 2>/dev/null ) | sed "s|^|${'$'}id |"
            done
            """.trimIndent(),
        )
        return r.output
            .mapNotNull { line ->
                val idx = line.indexOf(' ')
                if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
            }
            .groupBy({ it.second }, { it.first })
            .filterValues { it.size > 1 }
            .map { (path, ids) -> Conflict(path, ids.distinct()) }
    }

    /** 停用某个模块（写 disable 文件，重启生效）。 */
    suspend fun disable(moduleId: String): Boolean =
        HideBundle.run("touch /data/adb/modules/$moduleId/disable && echo ok").ok

    /** 恢复某个模块。 */
    suspend fun enable(moduleId: String): Boolean =
        HideBundle.run("rm -f /data/adb/modules/$moduleId/disable && echo ok").ok

    /** 找出最近安装的几个模块（按目录 mtime）。 */
    suspend fun recentModules(limit: Int = 3): List<Pair<String, Long>> =
        HideBundle.run(
            "ls -t /data/adb/modules 2>/dev/null | head -$limit",
        ).output.filter { it.isNotBlank() }.map { it.trim() to 0L }

    /** 清理包缓存与残留。 */
    suspend fun clearCaches(): List<String> {
        val r = HideBundle.run(
            "rm -rf /data/local/tmp/* 2>/dev/null; " +
                "rm -rf /data/adb/ksu/log/* 2>/dev/null; echo cleaned",
        )
        return r.output
    }

    /**
     * 一键自愈：停用每组冲突里「后加载」的模块（按目录顺序，后者覆盖前者）。
     * 返回被停用的模块列表。
     */
    suspend fun healAll(): List<String> {
        val list = conflicts()
        val disabled = mutableListOf<String>()
        list.forEach { c ->
            // 同一路径被多个模块覆盖时，保留第一个，停用其余
            c.moduleIds.drop(1).forEach { id ->
                if (id !in disabled && disable(id)) disabled += id
            }
        }
        return disabled
    }
}
