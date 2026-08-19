package com.songci.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android 更新检查:GET https://api.github.com/repos/{owner}/{repo}/releases/latest。
 * 零新依赖:HttpURLConnection(JRE 内置)+ org.json(Android 平台自带)。
 * 未认证限流 60 次/h/IP;手动检查场景足够。
 */
actual suspend fun checkForAppUpdate(
    repoOwner: String,
    repoName: String,
    currentVersion: String,
): UpdateCheckResult = withContext(Dispatchers.IO) {
    try {
        val conn = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "SongciSelection")

        val code = conn.responseCode
        if (code != 200) return@withContext UpdateCheckResult.Failed

        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val tag = json.optString("tag_name", "")
        val notes = json.optString("body", "").trim().take(200)   // 摘要截断
        val url = json.optString("html_url", "https://github.com/$repoOwner/$repoName/releases")
        if (tag.isBlank()) return@withContext UpdateCheckResult.Failed

        if (isNewerVersion(tag, currentVersion)) {
            UpdateCheckResult.Available(tag.removePrefix("v"), notes, url)
        } else {
            UpdateCheckResult.UpToDate
        }
    } catch (e: Exception) {
        UpdateCheckResult.Failed
    }
}

/** 打开浏览器:ACTION_VIEW intent。 */
actual fun openUrlInBrowser(url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        AppContextHolder.context.startActivity(intent)
    } catch (e: Exception) {
        // 无浏览器等极端情况:静默忽略,不崩溃
    }
}

/** 当前版本号:从 PackageManager 读 versionName(单一来源:manifest versionName,跟随 build.gradle.kts)。 */
actual fun currentAppVersion(): String = try {
    val pm = AppContextHolder.context.packageManager
    pm.getPackageInfo(AppContextHolder.context.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}
