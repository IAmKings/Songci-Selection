package com.songci.app.data

/**
 * 应用更新检查(GitHub Release)平台桥接接口。
 * MVP 仅 Android(查询 GitHub API 并返回最新版本);Desktop/iOS 后续按同接口实现 actual。
 * 零新依赖:Android 用 JRE 内置 HttpURLConnection 查询。
 */

/** 更新检查结果。 */
sealed interface UpdateCheckResult {
    /** 有新版。 */
    data class Available(val latestTag: String, val releaseNotes: String, val releaseUrl: String) : UpdateCheckResult

    /** 已是最新(或本地高于远程)。 */
    data object UpToDate : UpdateCheckResult

    /** 检查失败(离线/API 错误/网络异常)。 */
    data object Failed : UpdateCheckResult
}

/**
 * 检查 GitHub Release 是否有新版。
 * - repoOwner/repoName:仓库坐标(如 IAmKings / Songci-Selection)。
 * - currentVersion:本地版本号(如 0.1.0)。
 * - 返回 Available(latestTag 去 v 前缀后版本号,releaseNotes,releaseUrl)/ UpToDate / Failed。
 */
expect suspend fun checkForAppUpdate(repoOwner: String, repoName: String, currentVersion: String): UpdateCheckResult

/** semver 比较:返回 true 当 remote > local(去 v 前缀,数值比较 major.minor.patch)。 */
fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
    fun parse(v: String): Triple<Int, Int, Int> {
        val parts = v.trim().removePrefix("v").split(".")
        fun n(i: Int) = parts.getOrNull(i)?.toIntOrNull() ?: 0
        return Triple(n(0), n(1), n(2))
    }
    val r = parse(remoteTag)
    val l = parse(localVersion)
    // Triple 未实现 Comparable,显式逐段比较
    return when {
        r.first != l.first -> r.first > l.first
        r.second != l.second -> r.second > l.second
        else -> r.third > l.third
    }
}

/** 打开浏览器访问 url(Android 用 ACTION_VIEW intent;桌面/iOS 后续)。 */
expect fun openUrlInBrowser(url: String)

