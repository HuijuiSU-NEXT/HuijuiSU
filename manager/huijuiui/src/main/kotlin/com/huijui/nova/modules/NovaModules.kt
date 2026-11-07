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
        val hasWebUi: Boolean = false,
    )

    suspend fun list(): List<Module> {
        // 不用特殊分隔符（不同 shell 对 \037 处理不一致）：
        // 用 "##<id>" 分段，逐行解析 module.prop 的键值
        val script = """
            for d in /data/adb/modules/*/; do
              id=${'$'}(basename "${'$'}d")
              echo "##${'$'}id"
              if [ -f "${'$'}d/disable" ]; then echo "disable=1"; else echo "disable=0"; fi
              if [ -f "${'$'}d/update" ]; then echo "update=1"; else echo "update=0"; fi
              if [ -d "${'$'}d/webroot" ]; then echo "webui=1"; fi
              if [ -f "${'$'}d/module.prop" ]; then
                grep -E '^(name|version|author|description)=' "${'$'}d/module.prop" 2>/dev/null
              fi
            done
            exit 0
        """.trimIndent()

        val result = HideBundle.runScript(script)
        android.util.Log.d(
            "HuiJuiModules",
            "ok=${result.ok} lines=${result.output.size} head=${result.output.take(3)}",
        )
        if (result.output.isEmpty()) return emptyList()

        val modules = mutableListOf<Module>()
        var id: String? = null
        var name = ""
        var ver = ""
        var auth = ""
        var desc = ""
        var dis = false
        var upd = false
        var webui = false

        fun flush() {
            val cur = id ?: return
            modules += Module(
                id = cur,
                name = name.ifBlank { cur },
                version = ver,
                author = auth,
                description = desc,
                enabled = !dis,
                pendingUpdate = upd,
                hasWebUi = webui,
            )
            id = null; name = ""; ver = ""; auth = ""; desc = ""; dis = false; upd = false; webui = false
        }

        result.output.forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("##") -> {
                    flush()
                    id = line.removePrefix("##").trim()
                }
                line.startsWith("name=") -> name = line.removePrefix("name=").trim()
                line.startsWith("version=") -> ver = line.removePrefix("version=").trim()
                line.startsWith("author=") -> auth = line.removePrefix("author=").trim()
                line.startsWith("description=") -> desc = line.removePrefix("description=").trim()
                line == "disable=1" -> dis = true
                line == "update=1" -> upd = true
                line == "webui=1" -> webui = true
            }
        }
        flush()

        return modules.sortedBy { it.name.lowercase() }
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
