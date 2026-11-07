package com.huijui.nova.susfs

import com.huijui.nova.hide.HideBundle

/**
 * SUSFS 控制层。
 *
 * 全部走 `ksud susfs config`：
 * - `list_all` 读完整配置（JSON）
 * - `enable` / `disable` 总开关
 * - `logging` / `avc_log_spoofing` / `hide_sus_mnts_for_non_su_procs` 三个子开关用 add/remove
 * - `sus_path add|remove|list` 管理隐藏路径
 * - `backup` / `restore` 导出与恢复
 */
object Susfs {

    data class Status(
        val installed: Boolean = false,
        val enabled: Boolean = false,
        val logging: Boolean = false,
        val avcLogSpoofing: Boolean = false,
        val hideSusMnts: Boolean = false,
        val paths: List<String> = emptyList(),
        val raw: String = "",
        val error: String = "",
    )

    suspend fun status(): Status {
        val r = HideBundle.run("ksud susfs config list_all")
        val text = r.output.joinToString("\n")
        if (!r.ok || text.isBlank()) {
            return Status(error = if (r.ok) "SUSFS 未启用或内核不支持" else "需要 Root / ksud")
        }
        return Status(
            installed = true,
            enabled = Regex("\"enabled\"\\s*:\\s*true").containsMatchIn(text),
            logging = Regex("\"logging\"\\s*:\\s*(true|\\[\\s*\\{\\s*\"?[^\\]]*)").containsMatchIn(text),
            avcLogSpoofing = text.contains("avc_log_spoofing", true),
            hideSusMnts = Regex("\"hide_sus_mnts_for_non_su_procs\"\\s*:\\s*(true|\\[\\s*\\{)")
                .containsMatchIn(text),
            paths = Regex("\"(/[^\"]+)\"").findAll(text).map { it.groupValues[1] }.distinct().toList(),
            raw = text,
        )
    }

    suspend fun setEnabled(on: Boolean): HideBundle.Result =
        HideBundle.run("ksud susfs config ${if (on) "enable" else "disable"}")

    /** 三个开关都是「有记录=开，无记录=关」。 */
    suspend fun setToggle(key: String, on: Boolean): HideBundle.Result =
        HideBundle.run("ksud susfs config $key ${if (on) "add" else "remove"}")

    suspend fun addPath(path: String, loop: Boolean): HideBundle.Result =
        HideBundle.run("ksud susfs config sus_path add '$path'${if (loop) " --loop" else ""}")

    suspend fun removePath(path: String): HideBundle.Result =
        HideBundle.run("ksud susfs config sus_path remove '$path'")

    suspend fun listPaths(): List<String> =
        HideBundle.run("ksud susfs config sus_path list").output.filter { it.isNotBlank() }

    suspend fun backupTo(path: String): HideBundle.Result =
        HideBundle.run("ksud susfs config backup > '$path' 2>&1 && echo saved:$path")

    suspend fun restoreFrom(path: String): HideBundle.Result =
        HideBundle.run("ksud susfs config restore '$path'")

    suspend fun restoreDefault(): HideBundle.Result =
        HideBundle.run("ksud susfs config restore")
}
