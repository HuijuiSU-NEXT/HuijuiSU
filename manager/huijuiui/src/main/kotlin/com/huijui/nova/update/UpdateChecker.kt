package com.huijui.nova.update

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新检查。
 *
 * 直连 GitHub API 在国内经常超时，这里做三件事：
 * 1. 多镜像依次回退（api.github.com → jsDelivr 的 gh API 代理 → ghproxy）
 * 2. 结果落盘缓存 6 小时，避免每次进设置都打网络
 * 3. 版本号按段比较，而不是字符串相等
 */
object UpdateChecker {

    private const val REPO = "HuijuiSU-NEXT/HuijuiSU"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

    data class Release(val tag: String, val name: String, val notes: String, val url: String, val fromCache: Boolean = false)

    private fun mirrors(): List<Pair<String, String>> = listOf(
        "GitHub API" to "https://api.github.com/repos/$REPO/releases/latest",
        "ghproxy" to "https://ghproxy.net/https://api.github.com/repos/$REPO/releases/latest",
        "gh-proxy" to "https://gh-proxy.com/https://api.github.com/repos/$REPO/releases/latest",
    )

    fun latest(cacheFile: File): Release? {
        val cached = readCache(cacheFile)
        if (cached != null && System.currentTimeMillis() - cacheFile.lastModified() < CACHE_TTL_MS) {
            return cached.copy(fromCache = true)
        }

        for ((_, url) in mirrors()) {
            val text = runCatching { fetch(url) }.getOrNull() ?: continue
            val release = runCatching { parse(text) }.getOrNull() ?: continue
            runCatching { cacheFile.writeText(text) }
            return release
        }
        // 全部失败时，过期的缓存也比没有强
        return cached?.copy(fromCache = true)
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 12000
        conn.setRequestProperty("User-Agent", "HuiJuiSU")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parse(text: String): Release {
        val o = JSONObject(text)
        return Release(
            tag = o.optString("tag_name"),
            name = o.optString("name", o.optString("tag_name")),
            notes = o.optString("body").take(600),
            url = o.optString("html_url"),
        )
    }

    private fun readCache(file: File): Release? =
        runCatching { if (file.exists()) parse(file.readText()) else null }.getOrNull()

    /** 返回 -1 / 0 / 1：current 比 candidate 旧 / 相同 / 新。 */
    fun compare(current: String, candidate: String): Int {
        fun parts(v: String) = v.removePrefix("v").split('.', '-', '_')
            .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val a = parts(current)
        val b = parts(candidate)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return if (x < y) -1 else 1
        }
        return 0
    }
}
