package org.bakasu.bakasu.huijui.data

import android.content.Context
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
     *
     * 用单次 su 调用拿到所有模块及状态，避免每个模块单独 su（性能问题）。
     */
    suspend fun capture(): Snapshot = withContext(Dispatchers.IO) {
        val script = buildString {
            append("for d in $MODULES_DIR/*/; do ")
            append("id=\$(basename \"\$d\"); ")
            append("disable=0; remove=0; ")
            append("[ -f \"\$d/disable\" ] && disable=1; ")
            append("[ -f \"\$d/remove\" ] && remove=1; ")
            append("echo \"\$id|\$disable|\$remove\"; ")
            append("done")
        }
        val out = HuijuiShellUtils.runAsRoot(script, timeoutSeconds = 30, trim = true)
        Log.i(TAG, "raw output:\n$out")

        val modules = out.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val parts = trimmed.split("|")
            if (parts.size != 3) return@mapNotNull null
            ModuleEntry(
                id = parts[0],
                enabled = parts[1] == "0",
                remove = parts[2] == "1",
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
     *
     * 先写到 App 私有目录，再用 su 复制到目标位置，
     * 避免 SELinux context 问题导致 App 无法直接写入 /sdcard/。
     */
    fun writeToFile(context: Context, snapshot: Snapshot, dir: File): Boolean = try {
        val json = buildJson(snapshot)
        val tempFile = File(context.cacheDir, SNAPSHOT_FILE)
        tempFile.writeText(json)
        Log.i(TAG, "临时文件已写：${tempFile.absolutePath} (${tempFile.length()} bytes)")

        val targetPath = "${dir.absolutePath}/$SNAPSHOT_FILE"
        val out = HuijuiShellUtils.runAsRoot(
            "cp '${tempFile.absolutePath}' '$targetPath' && chmod 644 '$targetPath'",
            timeoutSeconds = 10,
            trim = true,
        )
        val ok = File(dir, SNAPSHOT_FILE).exists()
        Log.i(TAG, "cp 结果：[$out]，目标存在=$ok")
        ok
    } catch (t: Throwable) {
        Log.w(TAG, "写入快照失败", t)
        false
    }

    /**
     * 从 JSON 文件读取快照。
     */
    fun readFromFile(dir: File): Snapshot? = try {
        val f = File(dir, SNAPSHOT_FILE)
        if (!f.exists()) {
            Log.i(TAG, "无快照文件：${f.absolutePath}")
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
        Log.w(TAG, "读取快照失败", t)
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
            val moduleDir = "$MODULES_DIR/${m.id}"
            val exists = HuijuiShellUtils.runAsRoot(
                "[ -d '$moduleDir' ] && echo yes || echo no",
                timeoutSeconds = 5,
                trim = true,
            ) == "yes"
            if (!exists) {
                skipped++
                return@forEach
            }
            try {
                if (m.enabled) {
                    HuijuiShellUtils.runAsRoot("rm -f '$moduleDir/disable'", 10, trim = true)
                } else {
                    HuijuiShellUtils.runAsRoot("touch '$moduleDir/disable'", 10, trim = true)
                }
                if (m.remove) {
                    HuijuiShellUtils.runAsRoot("touch '$moduleDir/remove'", 10, trim = true)
                } else {
                    HuijuiShellUtils.runAsRoot("rm -f '$moduleDir/remove'", 10, trim = true)
                }
                restored++
            } catch (t: Throwable) {
                failures += "${m.id}: ${t.message}"
            }
        }

        Log.i(TAG, "还原完成：$restored 成功，$skipped 跳过，${failures.size} 失败")
        RestoreResult(restored, skipped, failures)
    }

    private fun buildJson(snapshot: Snapshot): String {
        val arr = JSONArray()
        snapshot.modules.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("enabled", m.enabled)
                put("remove", m.remove)
            })
        }
        return JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("createdAt", snapshot.createdAt)
            put("modules", arr)
        }.toString(2)
    }

    data class RestoreResult(
        val restored: Int,
        val skipped: Int,
        val failures: List<String>,
    )
}
