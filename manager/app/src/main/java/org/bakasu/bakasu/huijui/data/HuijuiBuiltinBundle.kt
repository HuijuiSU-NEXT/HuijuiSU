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
 * - `hide_bundle_v1.3.zip`：一键隐藏模块（恋雪 v1.3）
 * - `evil_service_check.sh`：Evil Service 自查脚本
 *
 * 首次启动时静默部署到 `/data/adb/huijui/`，供模块与脚本直接调用。
 * 通过版本号控制，升级 APK 后会自动更新设备上的副本。
 */
object HuijuiBuiltinBundle {

    private const val TAG = "HuijuiBuiltin"
    private const val PREFS = "huijui_builtin"
    private const val KEY_VERSION = "deployed_version"

    /** 每次改动内置文件内容时 +1，触发重新部署。 */
    const val VERSION = 2

    const val DEVICE_DIR = "/data/adb/huijui"
    const val HIDE_ZIP_ASSET = "huijui/hide_bundle_v1.3.zip"
    const val EVIL_SH_ASSET = "huijui/evil_service_check.sh"
    const val CHUNQIU_ZIP_ASSET = "huijui/chunqiu_fix_v1.zip"

    const val HIDE_ZIP_NAME = "hide_bundle_v1.3.zip"
    const val EVIL_SH_NAME = "evil_service_check.sh"
    const val CHUNQIU_ZIP_NAME = "chunqiu_fix_v1.zip"

    /** 设备上的模块包路径（不存在返回 null）。 */
    fun deviceHideZip(): File? = File(DEVICE_DIR, HIDE_ZIP_NAME).takeIf { it.isFile }

    /** 设备上的自查脚本路径（不存在返回 null）。 */
    fun deviceEvilScript(): File? = File(DEVICE_DIR, EVIL_SH_NAME).takeIf { it.isFile }

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
    suspend fun deployIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val deployed = prefs.getInt(KEY_VERSION, 0)
            val zipOk = File(DEVICE_DIR, HIDE_ZIP_NAME).let { it.isFile && it.length() > 0 }
            val shOk = File(DEVICE_DIR, EVIL_SH_NAME).let { it.isFile && it.length() > 0 }
            val cqOk = File(DEVICE_DIR, CHUNQIU_ZIP_NAME).let { it.isFile && it.length() > 0 }

            if (deployed >= VERSION && zipOk && shOk && cqOk) {
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
                append("chmod 644 '$DEVICE_DIR/$HIDE_ZIP_NAME'; ")
                append("chmod 644 '$DEVICE_DIR/$CHUNQIU_ZIP_NAME'; ")
                append("chmod 755 '$DEVICE_DIR/$EVIL_SH_NAME'; ")
                append("ls -l $DEVICE_DIR")
            }
            val out = HuijuiShellUtils.runAsRoot(cmd)
            val ok = out.contains(HIDE_ZIP_NAME)
            if (ok) {
                prefs.edit().putInt(KEY_VERSION, VERSION).apply()
                Log.i(TAG, "内置资源部署完成 v$VERSION")
            } else {
                Log.w(TAG, "内置资源部署失败：$out")
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
        val path = File(DEVICE_DIR, EVIL_SH_NAME)
        if (!path.isFile) return@withContext "内置脚本不可用（需要 root 部署）"
        HuijuiShellUtils.runAsRoot("sh '${path.absolutePath}' 2>&1")
    }
}
