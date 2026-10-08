package com.glow.filllight

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val releaseTitle: String,
    val changelog: String,
    val releaseUrl: String,
    val downloadUrl: String?,
    val isNewVersion: Boolean,
)

/** 语义化版本号比较：remoteTag > currentVersion 则返回 true */
fun isVersionNewer(remoteTag: String, currentVersion: String): Boolean {
    fun parse(v: String): List<Int> {
        return v.trim().removePrefix("v").removePrefix("V")
            .split(".")
            .mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
    }
    val rList = parse(remoteTag)
    val cList = parse(currentVersion)
    val maxLen = maxOf(rList.size, cList.size)
    for (i in 0 until maxLen) {
        val r = rList.getOrElse(i) { 0 }
        val c = cList.getOrElse(i) { 0 }
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

object UpdateChecker {
    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/zh-hanlabs/FillLight/releases/latest"

    /** 请求 GitHub Releases API 检查最新版本 */
    suspend fun checkUpdate(currentVersion: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(GITHUB_LATEST_RELEASE_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "FillLight-Android-App")
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    error("HTTP ${conn.responseCode}: ${conn.responseMessage}")
                }
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonStr = reader.readText()
                reader.close()
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "")
                val releaseTitle = json.optString("name", tagName)
                val changelog = json.optString("body", "暂无更新日志")
                val releaseUrl = json.optString("html_url", "https://github.com/zh-hanlabs/FillLight/releases")

                val assets = json.optJSONArray("assets")
                var downloadUrl: String? = null
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            break
                        }
                    }
                }
                if (downloadUrl == null) {
                    downloadUrl = releaseUrl
                }

                val isNew = isVersionNewer(tagName, currentVersion)

                AppUpdateInfo(
                    tagName = tagName,
                    releaseTitle = releaseTitle,
                    changelog = changelog,
                    releaseUrl = releaseUrl,
                    downloadUrl = downloadUrl,
                    isNewVersion = isNew,
                )
            } finally {
                conn.disconnect()
            }
        }
    }
}
