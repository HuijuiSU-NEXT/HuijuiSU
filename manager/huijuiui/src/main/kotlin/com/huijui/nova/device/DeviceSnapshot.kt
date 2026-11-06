package com.huijui.nova.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 真实设备信息（只读，不需要 Root）。
 *
 * 之前首页 Hero 卡写的是「Xiaomi 14 Ultra / 6.1.75」这种演示数据，
 * 换成真机读取后，型号、Android 版本、内核、ABI、内存都是用户自己手机上的值。
 */
@Immutable
data class DeviceSnapshot(
    val model: String,
    val manufacturer: String,
    val androidRelease: String,
    val sdkInt: Int,
    val kernel: String,
    val abi: String,
    val totalRamMb: Int,
    val cores: Int,
) {
    /** 卡片副标题用的一行摘要。 */
    val summary: String
        get() = "$manufacturer $model · Android $androidRelease (API $sdkInt)"

    /** KMI 近似值：androidXX-<内核主版本>。 */
    val kmi: String
        get() {
            val major = kernel.substringBefore('-').split('.').take(2).joinToString(".")
            return "android$androidRelease-$major"
        }

    val ramLabel: String
        get() = if (totalRamMb >= 1024) "%.1f GB".format(totalRamMb / 1024f) else "$totalRamMb MB"
}

fun readDeviceSnapshot(context: Context): DeviceSnapshot {
    val totalRam = runCatching {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        (info.totalMem / (1024 * 1024)).toInt()
    }.getOrDefault(0)

    return DeviceSnapshot(
        model = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Unknown",
        manufacturer = Build.MANUFACTURER?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
            ?: "Unknown",
        androidRelease = Build.VERSION.RELEASE ?: "?",
        sdkInt = Build.VERSION.SDK_INT,
        kernel = System.getProperty("os.version") ?: "unknown",
        abi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "unknown",
        totalRamMb = totalRam,
        cores = Runtime.getRuntime().availableProcessors(),
    )
}

@Composable
fun rememberDeviceSnapshot(): DeviceSnapshot {
    val context = LocalContext.current
    return remember(context) { readDeviceSnapshot(context) }
}
