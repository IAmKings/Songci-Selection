package com.songci.app.data

/**
 * Desktop 占位更新检查(MVP 仅 Android)。保持三端编译通过;多端同步后续实现。
 */
actual suspend fun checkForAppUpdate(
    repoOwner: String,
    repoName: String,
    currentVersion: String,
): UpdateCheckResult = UpdateCheckResult.Failed

/** Desktop 占位:浏览器打开留待多端同步。 */
actual fun openUrlInBrowser(url: String) = Unit

/** Desktop 占位:与 Android 版保持一致(版本号单一来源待多端同步)。 */
actual fun currentAppVersion(): String = "0.1.4"

/** Desktop 无自动发布 workflow(签名打包),不显示更新入口。 */
actual fun supportsAppUpdate(): Boolean = false
