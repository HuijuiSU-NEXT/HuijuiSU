package org.bakasu.bakasu.huijui.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * HuiJuiSU NEXT 模块快照
 *
 * 记录 /data/adb/modules/ 下所有模块的启用 / 卸载状态，
 * 刷机后一键还原，避免逐个手动重新配置。
 */
object HuijuiModuleSnapshot {

    private const val TAG = "HuijuiModuleSnapshot"
    private const val MODULES_DIR = "/data/adb/modules"
    private const val SNAPSHOT_FILE = "modules.json"
    private const val FORMAT_VERSION = 1

    data class ModuleEntry(
        val id: String,
        val enabled: Boolean,
        val remove: Boolean,
    )

    data class Snapshot(
        val createdAt: Long,
        val modules: List<ModuleEntry>,
    )

    /**
     * 捕获当前模块状态。
     */
    suspend fun capture(): Snapshot = withContext(Dispatchers.IO) {
        val out = HuijuiShellUtils.runAsRoot(
            "ls $MODULES_DIR 2>/dev/null",
            timeoutSeconds = 15,
            trim = true,
        )
        val ids = out.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val modules = ids.map { id ->
            val disable = checkFileExists("$MODULES_DIR/$id/disable")
            val remove = checkFileExists("$MODULES_DIR/$id/remove")
            ModuleEntry(
                id = id,
                enabled = !disable,
                remove = remove,
            )
        }
        Log.i(TAG, "捕获 ${modules.size} 个模块")
        Snapshot(
            createdAt = System.currentTimeMillis(),
            modules = modules,
        )
    }

    /**
     * 序列化到 JSON 文件。
     */
    fun writeToFile(snapshot: Snapshot, dir: File): Boolean = try {
        val arr = JSONArray()
        snapshot.modules.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("enabled", m.enabled)
                put("remove", m.remove)
            })
        }
        val root = JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("createdAt", snapshot.createdAt)
            put("modules", arr)
        }
        File(dir, SNAPSHOT_FILE).writeText(root.toString(2))
        Log.i(TAG, "快照写入 ${dir.absolutePath}/$SNAPSHOT_FILE")
        true
    } catch (t: Throwable) {
        Log.w(TAG, "写入快照失败：${t.message}")
        false
    }

    /**
     * 从 JSON 文件读取快照。
     */
    fun readFromFile(dir: File): Snapshot? = try {
        val f = File(dir, SNAPSHOT_FILE)
        if (!f.exists()) {
            null
        } else {
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("modules") ?: JSONArray()
            val modules = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ModuleEntry(
                    id = o.optString("id"),
                    enabled = o.optBoolean("enabled", true),
                    remove = o.optBoolean("remove", false),
                )
            }
            Snapshot(
                createdAt = root.optLong("createdAt", 0L),
                modules = modules,
            )
        }
    } catch (t: Throwable) {
        Log.w(TAG, "读取快照失败：${t.message}")
        null
    }

    /**
     * 按快照还原模块状态。
     *
     * 只操作 disable / remove 标志文件，不删除模块本体。
     * 已不存在的模块会跳过。
     */
    suspend fun restore(snapshot: Snapshot): RestoreResult = withContext(Dispatchers.IO) {
        var restored = 0
        var skipped = 0
        val failures = mutableListOf<String>()

        snapshot.modules.forEach { m ->
            if (!checkFileExists("$MODULES_DIR/${m.id}")) {
                skipped++
                return@forEach
            }
            try {
                if (m.enabled) {
                    HuijuiShellUtils.runAsRoot("rm -f $MODULES_DIR/${m.id}/disable", 10, trim = true)
                } else {
                    HuijuiShellUtils.runAsRoot("touch $MODULES_DIR/${m.id}/disable", 10, trim = true)
                }
                if (m.remove) {
                    HuijuiShellUtils.runAsRoot("touch $MODULES_DIR/${m.id}/remove", 10, trim = true)
                } else {
                    HuijuiShellUtils.runAsRoot("rm -f $MODULES_DIR/${m.id}/remove", 10, trim = true)
                }
                restored++
            } catch (t: Throwable) {
                failures += "${m.id}: ${t.message}"
            }
        }

        Log.i(TAG, "还原完成：$restored 成功，$skipped 跳过，${failures.size} 失败")
        RestoreResult(restored, skipped, failures)
    }

    private fun checkFileExists(path: String): Boolean {
        val out = HuijuiShellUtils.runAsRoot(
            "[ -e $path ] && echo yes || echo no",
            timeoutSeconds = 5,
            trim = true,
        )
        return out == "yes"
    }

    data class RestoreResult(
        val restored: Int,
        val skipped: Int,
        val failures: List<String>,
    )
}
