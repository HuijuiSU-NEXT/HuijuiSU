package org.bakasu.bakasu

import android.system.Os

/**
 * @author weishu
 * @date 2022/12/10.
 */

data class KernelVersion(val major: Int, val patchLevel: Int, val subLevel: Int) {
    override fun toString(): String = "$major.$patchLevel.$subLevel"
    fun isGKI(): Boolean = when {
        major > 5 -> true
        major == 5 && patchLevel >= 10 -> true
        else -> false
    }
}

fun parseKernelVersion(version: String): KernelVersion {
    val find = "(\\d+)\\.(\\d+)\\.(\\d+)".toRegex().find(version)
    return if (find != null) {
        KernelVersion(find.groupValues[1].toInt(), find.groupValues[2].toInt(), find.groupValues[3].toInt())
    } else {
        KernelVersion(-1, -1, -1)
    }
}

fun getKernelVersion(): KernelVersion {
    Os.uname().release.let {
        return parseKernelVersion(it)
    }
}

/**
 * 从内核版本字符串自动解析 KMI（Kernel Module Interface）。
 *
 * 输入示例：6.12.69-android16-6-g586bfab1b9c5-abogki53674944-4k
 * 输出示例：android16-6.12
 *
 * 输入示例：5.10.198-android13-8-00001-g1234567890ab
 * 输出示例：android13-5.10
 *
 * 输入示例：5.15.123-android14-6-gabcd
 * 输出示例：android14-5.15
 */
fun parseKmi(kernelRelease: String): String {
    // 匹配 androidXX-Y.Z 片段（内核 release 里的 android 版本标识）
    val androidRegex = """android(\d+)""".toRegex()
    val androidMatch = androidRegex.find(kernelRelease)
    val androidPart = androidMatch?.value ?: ""

    // 解析内核主版本.次版本
    val kv = parseKernelVersion(kernelRelease)
    val kernelPart = if (kv.major > 0) "${kv.major}.${kv.patchLevel}" else ""

    return when {
        androidPart.isEmpty() || kernelPart.isEmpty() -> ""
        // 过滤掉 android 后面是 11 以下的老设备
        androidMatch!!.groupValues[1].toInt() < 11 -> ""
        else -> "$androidPart-$kernelPart"
    }
}

