package org.bakasu.bakasu.huijui.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.huijui.util.HuijuiShellUtils
import java.io.File

/**
 * 内置资源包（随 APK 一起分发，不依赖用户导入）。
 *
 * 包含：
 * - `hide_bundle_v1.4_mod.zip`：一键隐藏模块（恋雪 v1.4 超级定制版）
 * - `hide_bundle_v1.4_mod.zip` 为定制内置版：去掉 120MB 检测 APK，修好春秋脚本路径与兜底解压
 * - `evil_service_check.sh`：Evil Service 自查脚本
 *
 * 首次启动时静默部署到 `/data/adb/huijui/`，供模块与脚本直接调用。
 * 通过版本号控制，升级 APK 后会自动更新设备上的副本。
 */
object HuijuiBuiltinBundle {

    private const val TAG = "HuijuiBuiltin"
    private const val PREFS = "huijui_builtin"
    private const val KEY_VERSION = "deployed_version"

    /** 最近一次部署失败的原因（给 UI 显示）。 */
    @Volatile
    var lastError: String = ""
        private set

    /** 每次改动内置文件内容时 +1，触发重新部署。 */
    const val VERSION = 6

    const val DEVICE_DIR = "/data/adb/huijui"
    const val HIDE_ZIP_ASSET = "huijui/hide_bundle_v1.4_super.zip"
    const val EVIL_SH_ASSET = "huijui/evil_service_check.sh"
    const val CHUNQIU_ZIP_ASSET = "huijui/chunqiu_fix_v1.zip"
    const val HYBRID_ZIP_ASSET = "huijui/hybrid_mount_6.2.5.zip"

    const val HIDE_ZIP_NAME = "hide_bundle_v1.4_super.zip"
    const val EVIL_SH_NAME = "evil_service_check.sh"
    const val CHUNQIU_ZIP_NAME = "chunqiu_fix_v1.zip"
    const val HYBRID_ZIP_NAME = "hybrid_mount_6.2.5.zip"


    /** 用 root 判断设备上的文件是否存在且非空（应用进程自己 stat /data/adb 会失败）。 */
    suspend fun remoteExists(path: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val out = org.bakasu.bakasu.huijui.util.HuijuiShellUtils.runAsRoot(
            "test -s '$path' && echo __YES__ || echo __NO__",
            timeoutSeconds = 15,
            trim = true,
        )
        out.contains("__YES__")
    }

    /** APK 内的模块包，复制到应用缓存后返回（供模块安装器使用）。 */
    fun cachedHideZip(context: Context): File? = runCatching {
        val out = File(context.cacheDir, HIDE_ZIP_NAME)
        if (!out.isFile || out.length() <= 0) {
            context.assets.open(HIDE_ZIP_ASSET).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
        }
        out.takeIf { it.isFile && it.length() > 0 }
    }.getOrNull()

    /**
     * 把内置资源部署到 `/data/adb/huijui/`。
     * 幂等：版本未变且文件存在时直接跳过。
     */
    /** 在应用内解压内置脚本包，返回其中第一个 .sh（root 可读应用缓存）。 */
    fun extractChunqiu(context: Context): File? = runCatching {
        val dir = File(context.cacheDir, "chunqiu").apply {
            deleteRecursively()
            mkdirs()
        }
        context.assets.open(CHUNQIU_ZIP_ASSET).use { input ->
            java.util.zip.ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val out = File(dir, entry.name)
                    if (entry.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        dir.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".sh") }
    }.getOrNull()

    suspend fun deployIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val deployed = prefs.getInt(KEY_VERSION, 0)
            val zipOk = remoteExists("$DEVICE_DIR/$HIDE_ZIP_NAME")
            val shOk = remoteExists("$DEVICE_DIR/$EVIL_SH_NAME")
            val cqOk = remoteExists("$DEVICE_DIR/$CHUNQIU_ZIP_NAME")
            val hmOk = remoteExists("$DEVICE_DIR/$HYBRID_ZIP_NAME")

            if (deployed >= VERSION && zipOk && shOk && cqOk && hmOk) {
                Log.d(TAG, "内置资源已就位 (v$deployed)")
                return@withContext true
            }

            val zip = cachedHideZip(context) ?: run {
                Log.w(TAG, "读取内置模块包失败")
                return@withContext false
            }
            val cq = File(context.cacheDir, CHUNQIU_ZIP_NAME).also { out ->
                context.assets.open(CHUNQIU_ZIP_ASSET).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            val hm = File(context.cacheDir, HYBRID_ZIP_NAME).also { out ->
                context.assets.open(HYBRID_ZIP_ASSET).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            val sh = File(context.cacheDir, EVIL_SH_NAME).also { out ->
                context.assets.open(EVIL_SH_ASSET).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }

            // root 才能写 /data/adb；没有 root 时静默跳过，不打扰用户
            val cmd = buildString {
                append("mkdir -p $DEVICE_DIR; ")
                append("cp '${zip.absolutePath}' '$DEVICE_DIR/$HIDE_ZIP_NAME'; ")
                append("cp '${sh.absolutePath}' '$DEVICE_DIR/$EVIL_SH_NAME'; ")
                append("cp '${cq.absolutePath}' '$DEVICE_DIR/$CHUNQIU_ZIP_NAME'; ")
                append("cp '${hm.absolutePath}' '$DEVICE_DIR/$HYBRID_ZIP_NAME'; ")
                append("chmod 644 '$DEVICE_DIR/$HYBRID_ZIP_NAME'; ")
                append("chmod 644 '$DEVICE_DIR/$HIDE_ZIP_NAME'; ")
                append("chmod 644 '$DEVICE_DIR/$CHUNQIU_ZIP_NAME'; ")
                // 清理旧版本的隐藏模块包，避免 /data/adb/huijui 越堆越多
                append("ls '$DEVICE_DIR'/hide_bundle_v1*.zip 2>/dev/null | grep -v '/$HIDE_ZIP_NAME\\$' | xargs -r rm -f 2>/dev/null; ")
                append("chmod 755 '$DEVICE_DIR/$EVIL_SH_NAME'; ")
                append("ls -l $DEVICE_DIR")
            }
            val out = HuijuiShellUtils.runAsRoot(cmd)
            val ok = out.contains(HIDE_ZIP_NAME)
            if (ok) {
                ensureBootModule(context)
                prefs.edit().putInt(KEY_VERSION, VERSION).apply()
                Log.i(TAG, "内置资源部署完成 v$VERSION")
            } else {
                Log.w(TAG, "内置资源部署失败：$out")
                lastError = out.take(200)
            }
            ok
        } catch (t: Throwable) {
            Log.w(TAG, "内置资源部署异常：${t.message}")
            false
        }
    }

    /** 执行 Evil Service 自查，返回脚本输出。 */
    suspend fun runEvilCheck(context: Context): String = withContext(Dispatchers.IO) {
        deployIfNeeded(context)
        val path = "$DEVICE_DIR/$EVIL_SH_NAME"
        // 用 root 判断 + 执行，应用进程本身读不到 /data/adb
        val out = HuijuiShellUtils.runAsRoot(
            "test -s '$path' && sh '$path' 2>&1 || echo __NO_SCRIPT__",
            timeoutSeconds = 120,
        )
        if (out.contains("__NO_SCRIPT__") && out.trim().lines().size <= 1) {
            "内置脚本不可用：${lastError.ifBlank { "部署失败，请检查 root 授权" }}"
        } else {
            out
        }
    }

    /**
     * 写入开机自启模块 `huijui_core`。
     *
     * 作用：设备重启后由 KernelSU 拉起，几秒内完成
     * - 首次应用隐藏默认值
     * - 自动安装元模块 Hybrid Mount
     * - 再自动安装一键隐藏模块
     *
     * 用户不需要打开管理器。
     */
    private fun ensureBootModule(context: Context) {
        val dir = "/data/adb/modules/huijui_core"
        fun b64(asset: String): String = android.util.Base64.encodeToString(
            context.assets.open("huijui/core/$asset").use { it.readBytes() },
            android.util.Base64.NO_WRAP,
        )
        val sb = StringBuilder()
        sb.append("mkdir -p $dir; ")
        listOf("module.prop", "post-fs-data.sh", "boot-completed.sh").forEach { name ->
            sb.append("echo '${b64(name)}' | base64 -d > $dir/$name; ")
        }
        sb.append("chmod 644 $dir/module.prop; ")
        sb.append("chmod 755 $dir/post-fs-data.sh $dir/boot-completed.sh; ")
        sb.append("touch $dir/update; ")   // 让内核下次启动扫描到
        sb.append("ls $dir")
        val out = HuijuiShellUtils.runAsRoot(sb.toString(), timeoutSeconds = 30)
        Log.i(TAG, "开机自启模块写入：${out.contains("module.prop")}")
    }
}
