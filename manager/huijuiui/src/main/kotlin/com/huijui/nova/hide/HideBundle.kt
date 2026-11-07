package com.huijui.nova.hide

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * hide_bundle（一键隐藏 v2.0）的接入层。
 *
 * 这个模块由外部作者维护，安装后落在 `/data/adb/modules/hide_bundle`，
 * 对外暴露的命令面是固定的几支 shell 脚本。管理器这边只做三件事：
 * 1. 读状态（装没装、四个子模块在不在、版本号）；
 * 2. 把脚本命令包成按钮；
 * 3. 把 stdout/stderr 原样流回界面。
 *
 * 不复制它的逻辑，也不改它的文件——作者升级脚本时这边不用跟着改。
 */
object HideBundle {

    // KernelSU 对并发 su 调用会返回 EACCES，这里串行化并失败重试
    private val rootMutex = kotlinx.coroutines.sync.Mutex()

    const val MODULE_ID = "hide_bundle"
    const val MODULE_DIR = "/data/adb/modules/$MODULE_ID"
    const val LOG_PATH = "/data/adb/hide_bundle.log"

    /** 子模块：id + 展示名 + 作用。 */
    val subModules = listOf(
        Triple("zygisksu", "Zygisk Next", "独立 Zygisk 实现"),
        Triple("zygisk_lsposed", "LSPosed", "Xposed 框架"),
        Triple("hma_oss_zygisk", "HMA-OSS", "隐藏应用列表"),
        Triple("oh_my_keymint", "OhMyKeymint", "KeyMint / 完整性"),
    )

    data class Status(
        val installed: Boolean = false,
        val enabled: Boolean = false,
        val version: String? = null,
        val moduleDir: String = MODULE_DIR,
        val subStates: List<Pair<String, Boolean>> = emptyList(),
        val loggedIn: Boolean = false,
    )

    /** 一次操作的结果：退出码 + 输出行。 */
    data class Result(val exitCode: Int, val output: List<String>) {
        val ok: Boolean get() = exitCode == 0
    }

    /** 有 root 吗（`su -c id` 能返回 uid=0）。 */
    suspend fun hasRoot(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val out = process.inputStream.bufferedReader().readText()
            process.waitFor()
            out.contains("uid=0")
        }.getOrDefault(false)
    }

    /** 读当前状态。所有判断都走一次 su，避免非 root 下路径不可见。 */
    suspend fun readStatus(): Status = withContext(Dispatchers.IO) {
        val script = """
            if [ -d $MODULE_DIR ]; then
              echo "INSTALLED=1"
              [ -f $MODULE_DIR/disable ] && echo "DISABLED=1" || echo "DISABLED=0"
              grep -E '^version=' $MODULE_DIR/module.prop 2>/dev/null | head -1
              for m in zygisksu zygisk_lsposed hma_oss_zygisk oh_my_keymint; do
                [ -d /data/adb/modules/${'$'}m ] && echo "SUB=${'$'}m:1" || echo "SUB=${'$'}m:0"
              done
            else
              echo "INSTALLED=0"
            fi
        """.trimIndent()

        val result = run(script)
        val lines = result.output
        val installed = lines.any { it == "INSTALLED=1" }
        val disabled = lines.any { it == "DISABLED=1" }
        val version = lines.firstOrNull { it.startsWith("version=") }?.substringAfter("=")
        val subs = subModules.map { (id, _, _) ->
            id to lines.any { it == "SUB=$id:1" }
        }
        Status(
            installed = installed,
            enabled = installed && !disabled,
            version = version,
            subStates = subs,
            loggedIn = result.ok,
        )
    }

    /* ---------------- 命令面 ---------------- */

    fun install(): String = "sh $MODULE_DIR/oneclick.sh"
    fun forceReinstall(): String = "sh $MODULE_DIR/oneclick.sh --force"
    fun reimport(): String = "sh $MODULE_DIR/oneclick.sh --reimport"
    fun scanLists(): String = "sh $MODULE_DIR/applist.sh --apply"
    fun readLists(): String = "sh $MODULE_DIR/applist.sh"
    fun tailLog(): String = "tail -n 120 $LOG_PATH"
    fun showBlacklist(): String = "cat $MODULE_DIR/lists/generated_blacklist.txt 2>/dev/null | head -60"
    fun showWhitelist(): String = "cat $MODULE_DIR/lists/generated_whitelist.txt 2>/dev/null | head -60"

    /** 执行一段命令，按行回调（UI 可以边跑边显示）。 */
    suspend fun run(
        command: String,
        onLine: ((String) -> Unit)? = null,
    ): Result = withContext(Dispatchers.IO) {
        rootMutex.lock()
        try {
            var last: Result? = null
            repeat(2) { attempt ->
                val r = runOnce(command, onLine)
                if (r.ok || !r.output.any { it.contains("Permission denied") }) return@withContext r
                last = r
                kotlinx.coroutines.delay(200L * (attempt + 1))
            }
            last ?: Result(-1, emptyList())
        } finally {
            rootMutex.unlock()
        }
    }

    private suspend fun runOnce(
        command: String,
        onLine: ((String) -> Unit)? = null,
    ): Result = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val lines = mutableListOf<String>()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    lines += line
                    onLine?.invoke(line)
                    if (lines.size > 400) lines.removeAt(0)
                    line = reader.readLine()
                }
            }
            val code = process.waitFor()
            Result(code, lines)
        }.getOrElse { error ->
            Result(-1, listOf("执行失败：${error.message}"))
        }
    }

    /**
     * 执行多行脚本。
     *
     * 直接 `su -c "<多行>"` 在部分设备/内核上会执行失败，
     * 这里把脚本 base64 编码后单行写盘再执行，稳定性最好。
     */
    suspend fun runScript(
        script: String,
        onLine: ((String) -> Unit)? = null,
    ): Result {
        val b64 = android.util.Base64.encodeToString(
            script.toByteArray(),
            android.util.Base64.NO_WRAP,
        )
        val cmd = "echo '$b64' | base64 -d > /data/local/tmp/nova_script.sh && " +
            "sh /data/local/tmp/nova_script.sh; exit 0"
        return run(cmd, onLine)
    }

    /** 读取二进制文件（如 allowlist），返回原始字节。 */
    suspend fun runRaw(command: String): ByteArray = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder("su", "-c", command).start()
            val out = process.inputStream.readBytes()
            process.waitFor()
            out
        }.getOrDefault(ByteArray(0))
    }

    /** 读取一个文件（用 su，因为路径在 /data/adb）。 */
    suspend fun readFile(path: String, limit: Int = 120): List<String> = withContext(Dispatchers.IO) {
        val result = run("cat '$path' 2>/dev/null | head -$limit")
        result.output.ifEmpty { listOf("（空或不可读）") }
    }

    /** 模块是否已落地到本地（无 root 时也能用于显示提示）。 */
    fun isLikelyInstalled(): Boolean = runCatching { File(MODULE_DIR).isDirectory }.getOrDefault(false)

    /* ---------------- 内置安装包 ---------------- */

    /** 打包进 APK 的套件路径。 */
    const val ASSET_ZIP = "hide/hide-bundle-v2.0.zip"

    /**
     * 直接装内置包：把 assets 里的 zip 复制到 cache，交给 ksud / magisk 安装。
     *
     * 模块作者把整包给我们内置了，用户不需要自己去文件管理器找 zip。
     */
    suspend fun installBundled(
        context: android.content.Context,
        onLine: ((String) -> Unit)? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): Result = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "hide-bundle-v2.0.zip")
        runCatching {
            context.assets.open(ASSET_ZIP).use { input ->
                target.outputStream().use { output -> input.copyTo(output, bufferSize = 1 shl 16) }
            }
        }.onFailure { return@withContext Result(-1, listOf("读取内置安装包失败：${it.message}")) }
        onProgress?.invoke(0.35f)

        // 优先 ksud（KernelSU / 本管理器），退回 magisk
        val probe = run("command -v ksud >/dev/null 2>&1 && echo ksud || (command -v magisk >/dev/null 2>&1 && echo magisk || echo none)")
        val tool = probe.output.firstOrNull()?.trim().orEmpty()
        val command = when (tool) {
            "ksud" -> "ksud module install '${target.absolutePath}'"
            "magisk" -> "magisk --install-module '${target.absolutePath}'"
            else -> return@withContext Result(-1, listOf("找不到 ksud 或 magisk，无法安装模块"))
        }
        onLine?.invoke("$ $command")
        val result = run(command, onLine)
        onProgress?.invoke(1f)
        result
    }

    /** 内置包在 APK 里占多大（给界面显示）。 */
    fun bundledSize(context: android.content.Context): Long = runCatching {
        // assets 里的 zip 会被压缩，openFd 拿不到长度，只能读流
        context.assets.open(ASSET_ZIP).use { input ->
            val buffer = ByteArray(1 shl 16)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
            }
            total
        }
    }.getOrElse { 0L }
}
