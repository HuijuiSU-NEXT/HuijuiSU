package com.huijui.nova.repo

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * 在线模块仓库。
 *
 * 索引来自 HuijuiSU-NEXT/Huijui_Module_Repo 的 `modules.json`：
 * 一个数组，每项含 name / author / version / description / stars / releases[].zipUrl。
 *
 * 直连 raw 被墙时自动退回 GitHub API 的 raw 模式，两条都失败才报错。
 */
object ModuleRepo {

    private const val REPO = "HuijuiSU-NEXT/Huijui_Module_Repo"
    private const val BRANCH = "main"
    private const val FILE = "modules.json"

    /** 依次尝试；前面的挂了就换下一条。国内网络通常 jsDelivr / ghproxy 更通。 */
    val mirrors: List<Pair<String, String>> = listOf(
        "GitHub" to "https://raw.githubusercontent.com/$REPO/$BRANCH/$FILE",
        "GitHub API" to "https://api.github.com/repos/$REPO/contents/$FILE",
        "jsDelivr" to "https://cdn.jsdelivr.net/gh/$REPO@$BRANCH/$FILE",
        "ghproxy" to "https://ghproxy.net/https://raw.githubusercontent.com/$REPO/$BRANCH/$FILE",
        "gh-proxy" to "https://gh-proxy.com/https://raw.githubusercontent.com/$REPO/$BRANCH/$FILE",
        "gitmirror" to "https://raw.gitmirror.com/$REPO/$BRANCH/$FILE",
        "ghproxy.cn" to "https://ghproxy.cn/https://raw.githubusercontent.com/$REPO/$BRANCH/$FILE",
    )

    data class Mod(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val description: String,
        val stars: Int,
        val sizeKb: Long,
        val zipUrl: String,
        val updatedAt: String,
    )

    /** 上一次成功用的镜像名，界面上显示出来方便排查。 */
    @Volatile
    var lastMirror: String = ""
        private set

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 20000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "HuiJuiSU")
        if (url.contains("api.github.com")) {
            conn.setRequestProperty("Accept", "application/vnd.github.raw")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    /** 逐条试镜像，返回第一条能解析成功的内容。 */
    private fun download(): String {
        val errors = StringBuilder()
        for ((name, url) in mirrors) {
            val text = runCatching { fetch(url) }.getOrElse { e ->
                errors.append(name).append(": ").append(e.message ?: "fail").append('\n')
                null
            } ?: continue
            if (text.trimStart().startsWith("[")) {
                lastMirror = name
                return text
            }
            errors.append(name).append(": 返回内容不是数组\n")
        }
        throw IllegalStateException("所有镜像都失败：\n$errors")
    }

    /** 拉取并解析，按星标与更新时间排序，最多返回 [limit] 条。 */
    fun load(limit: Int = 150): List<Mod> {
        val text = download()
        val arr = JSONArray(text)
        val list = ArrayList<Mod>(minOf(arr.length(), 400))
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val releases = o.optJSONArray("releases") ?: continue
            val latest = releases.optJSONObject(0) ?: continue
            val zip = latest.optString("zipUrl")
            if (zip.isBlank()) continue
            list += Mod(
                id = o.optString("id"),
                name = o.optString("name", o.optString("id")),
                author = o.optString("author"),
                version = o.optString("version"),
                description = o.optString("description").replace("\n", " ").trim(),
                stars = o.optInt("stars", 0),
                sizeKb = latest.optLong("size", 0) / 1024,
                zipUrl = zip,
                updatedAt = o.optString("updatedAt").take(10),
            )
        }
        return list
            .sortedWith(compareByDescending<Mod> { it.stars }.thenByDescending { it.updatedAt })
            .take(limit)
    }

    /** 下载 zip 到本地，返回文件路径。 */
    fun downloadZip(zipUrl: String, dir: java.io.File, name: String): String {
        val target = java.io.File(dir, "repo_" + name.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".zip")
        val conn = URL(zipUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 120000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "HuiJuiSU")
        conn.inputStream.use { input ->
            target.outputStream().use { out -> input.copyTo(out, 1 shl 16) }
        }
        return target.absolutePath
    }
}
