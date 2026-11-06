package com.huijui.nova.modules

import com.huijui.nova.hide.HideBundle

/**
 * 模块管理（Nova 里最小可用的一版）。
 *
 * 只做三件事：列出 /data/adb/modules 下的模块、读 module.prop、开关（disable 文件）。
 * 安装、卸载、更新交给完整管理器——这里不重复造。
 */
object NovaModules {

    data class Module(
        val id: String,
        val name: String,
        val version: String,
        val author: String,
        val description: String,
        val enabled: Boolean,
        val pendingUpdate: Boolean,
    )

    suspend fun list(): List<Module> {
        val script = """
            for d in /data/adb/modules/*/; do
              id=${'$'}(basename "${'$'}d")
              prop="${'$'}d/module.prop"
              [ -f "${'$'}prop" ] || continue
              name=${'$'}(grep -m1 '^name=' "${'$'}prop" | cut -d= -f2-)
              ver=${'$'}(grep -m1 '^version=' "${'$'}prop" | cut -d= -f2-)
              auth=${'$'}(grep -m1 '^author=' "${'$'}prop" | cut -d= -f2-)
              desc=${'$'}(grep -m1 '^description=' "${'$'}prop" | cut -d= -f2-)
              dis=0; [ -f "${'$'}d/disable" ] && dis=1
              upd=0; [ -f "${'$'}d/update" ] && upd=1
              echo "${'$'}id|${'$'}name|${'$'}ver|${'$'}auth|${'$'}desc|${'$'}dis|${'$'}upd"
            done
        """.trimIndent()

        val result = HideBundle.run(script)
        if (!result.ok) return emptyList()

        return result.output.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 7) return@mapNotNull null
            Module(
                id = parts[0],
                name = parts[1].ifBlank { parts[0] },
                version = parts[2],
                author = parts[3],
                description = parts[4],
                enabled = parts[5] != "1",
                pendingUpdate = parts[6] == "1",
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun setEnabled(id: String, enabled: Boolean): Boolean {
        val cmd = if (enabled) {
            "rm -f /data/adb/modules/$id/disable"
        } else {
            "touch /data/adb/modules/$id/disable"
        }
        return HideBundle.run(cmd).ok
    }

    suspend fun uninstall(id: String): Boolean {
        // 官方做法：放 remove 标记，重启后由管理器清理，避免运行中删文件
        return HideBundle.run("touch /data/adb/modules/$id/remove && touch /data/adb/modules/$id/disable").ok
    }
}
