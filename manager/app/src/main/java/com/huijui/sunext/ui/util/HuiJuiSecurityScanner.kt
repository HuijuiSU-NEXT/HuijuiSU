package com.huijui.sunext.ui.util

import java.io.File
import java.util.zip.ZipFile

object HuiJuiSecurityScanner {

    // 危险指令正则匹配（防格机、防后门）
    private val dangerousPatterns = listOf(
        Regex("""rm\s+-[rf]*\s+/(?:\s|$)""", RegexOption.IGNORE_CASE),       // 删除根目录
        Regex("""rm\s+-[rf]*\s+/system""", RegexOption.IGNORE_CASE),       // 删除系统分区
        Regex("""rm\s+-[rf]*\s+/data(?:\s|$)""", RegexOption.IGNORE_CASE),   // 删除数据分区
        Regex("""dd\s+if=/dev/zero""", RegexOption.IGNORE_CASE),           // 零数据覆写（格机）
        Regex("""mkfs\.""", RegexOption.IGNORE_CASE),                      // 格式化分区
        Regex("""curl\s+.*\|\s*sh""", RegexOption.IGNORE_CASE),            // 下载并执行脚本（后门）
        Regex("""wget\s+.*\|\s*sh""", RegexOption.IGNORE_CASE),            // 下载并执行脚本（后门）
        Regex("""base64\s+-d.*\|\s*sh""", RegexOption.IGNORE_CASE)         // 解码并执行（隐藏后门）
    )

    // 必须要扫描的关键脚本
    private val targetScripts = listOf(
        "customize.sh",
        "post-fs-data.sh",
        "service.sh",
        "uninstall.sh",
        "META-INF/com/google/android/update-binary"
    )

    /**
     * 扫描模块 ZIP 文件
     * @return Pair<Boolean, String> 第一个参数是是否安全，第二个参数是警告信息
     */
    fun scanModule(zipFile: File): Pair<Boolean, String> {
        if (!zipFile.exists()) return Pair(false, "文件不存在")

        return try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory && targetScripts.any { entry.name.endsWith(it) }) {
                        val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                        // 逐行扫描
                        content.lines().forEach { line ->
                            val cleanLine = line.trim()
                            if (!cleanLine.startsWith("#")) { // 忽略注释
                                dangerousPatterns.forEach { pattern ->
                                    if (pattern.containsMatchIn(cleanLine)) {
                                        return Pair(false, "🛑 拦截！检测到恶意脚本指令 (${entry.name}): ${cleanLine.take(50)}...")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Pair(true, "✅ 脚本安全检查通过")
        } catch (e: Exception) {
            Pair(false, "扫描异常: ${e.message}")
        }
    }

    /**
     * 扫描模块 URI (安全防护)
     */
    fun scanModule(context: android.content.Context, uri: android.net.Uri): Pair<Boolean, String> {
        var tempFile: java.io.File? = null
        return try {
            // 只检查 .zip 模块，防止把内核 img 误判
            if (!uri.lastPathSegment.orEmpty().endsWith(".zip", ignoreCase = true)) {
                return Pair(true, "非 zip 模块，跳过扫描")
            }
            tempFile = java.io.File.createTempFile("module_scan", ".zip", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val result = scanModule(tempFile)
            if (!result.first) {
                tempFile.delete() // 发现危险直接物理删除临时文件
            }
            result
        } catch (e: Exception) {
            tempFile?.delete()
            Pair(false, "扫描异常: ${e.message}")
        }
    }
}
