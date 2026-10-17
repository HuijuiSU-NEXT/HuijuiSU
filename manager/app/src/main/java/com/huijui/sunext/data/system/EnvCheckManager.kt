package com.huijui.sunext.data.system

import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

data class CheckResult(
    val title: String,
    val isSafe: Boolean,
    val description: String
)

object EnvCheckManager {

    private fun execCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun runEnvironmentCheck(): List<CheckResult> {
        val results = mutableListOf<CheckResult>()

        val suPathExposed = File("/system/bin/su").exists()
        results.add(CheckResult("system/bin/su 隐藏", !suPathExposed, if (suPathExposed) "警告：路径暴露！" else "安全：已隐藏。"))

        val ksuDirExposed = File("/data/adb/ksu").exists()
        results.add(CheckResult("KernelSU 目录隐藏", !ksuDirExposed, if (ksuDirExposed) "警告：未开启挂载隐藏。" else "安全：已隐藏。"))

        val susfsLoaded = execCommand("cat /proc/filesystems").contains("susfs")
        results.add(CheckResult("SUSFS 状态", susfsLoaded, if (susfsLoaded) "正常：已加载。" else "异常：未启用 SUSFS。"))

        val isEnforcing = execCommand("getenforce").equals("Enforcing", ignoreCase = true)
        results.add(CheckResult("SELinux 状态", isEnforcing, if (isEnforcing) "正常：Enforcing 模式。" else "警告：非 Enforcing 模式。"))

        return results
    }
}
