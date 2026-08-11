package com.songci.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.songci.app.data.db.SongciDb
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import songci.composeapp.generated.resources.Res
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
private fun nsData(bytes: ByteArray): NSData = bytes.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
}

/** 升级复制:临时副本 → mergeUserData 合并旧库用户表 → 原子替换(失败保旧库+临时副本,下次启动重试)。 */
@OptIn(ExperimentalForeignApi::class)
private fun refreshDbFile(fileManager: NSFileManager, path: String, tmpPath: String, bytes: ByteArray, version: String) {
    nsData(bytes).writeToFile(tmpPath, atomically = true)
    if (fileManager.fileExistsAtPath(path)) {
        try {
            mergeUserData(path, tmpPath)
        } catch (_: Throwable) {
            // 合并失败:降级直接替换(保启动优先,本次用户数据丢失)
        }
    }
    fileManager.replaceItemAtURL(
        NSURL.fileURLWithPath(path),
        withItemAtURL = NSURL.fileURLWithPath(tmpPath),
        backupItemName = null,
        options = 0u,
        resultingItemURL = null,
        error = null,
    )
    (version as NSString).writeToFile("$path.version", atomically = true, encoding = NSUTF8StringEncoding, error = null)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createDatabaseDriver(): SqlDriver {
    val fileManager = NSFileManager.defaultManager
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).first() as String
    val path = "$documents/$DB_FILE_NAME"
    val versionPath = "$documents/$DB_FILE_NAME.version"
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    val resVersion = Res.readBytes("files/db_version.txt").decodeToString().trim()
    // 缓存版本标记: 版本文件缺失/不一致(数据更新) → 临时副本 → 合并用户表 → 替换
    val cachedVersion = fileManager.contentsAtPath(versionPath)?.let {
        NSString.create(data = it, encoding = NSUTF8StringEncoding) as String
    }?.trim()
    if (!fileManager.fileExistsAtPath(path) || cachedVersion != resVersion) {
        refreshDbFile(fileManager, path, "$documents/$DB_FILE_NAME.new", bytes, resVersion)
    }
    // 小组件共享: 复制 db → App Group 容器(WidgetKit 扩展读取),版本标记判新
    val groupDir = fileManager.containerURLForSecurityApplicationGroupIdentifier(GROUP_ID)?.path
    if (groupDir != null) {
        val groupDb = "$groupDir/$DB_FILE_NAME"
        val groupVersion = "$groupDir/$DB_FILE_NAME.version"
        val groupCached = fileManager.contentsAtPath(groupVersion)?.let {
            NSString.create(data = it, encoding = NSUTF8StringEncoding) as String
        }?.trim()
        if (!fileManager.fileExistsAtPath(groupDb) || groupCached != resVersion) {
            refreshDbFile(fileManager, groupDb, "$groupDir/$DB_FILE_NAME.new", bytes, resVersion)
        }
    }
    // 预建库 user_version=1 与 schema.version 一致,驱动跳过建表
    return NativeSqliteDriver(SongciDb.Schema, path)
}

actual fun mergeUserData(oldDbPath: String, newDbPath: String) {
    // ATTACH 旧库 → 逐表 INSERT OR REPLACE;旧库缺表(老版本)/数据不合 → 该表 INSERT 抛错 → 跳过该表
    // (PRD:任何用户表合并失败不阻塞启动,该表数据本次降级放弃)。executeQuery 的 mapper 泛型在
    // native 侧推断不稳,统一用 execute。
    val driver = NativeSqliteDriver(SongciDb.Schema, newDbPath)
    try {
        driver.execute(null, "ATTACH DATABASE '${quoteLiteral(oldDbPath)}' AS olddb", 0) { }
        try {
            USER_TABLES.forEach { t ->
                try {
                    driver.execute(null, "INSERT OR REPLACE INTO $t SELECT * FROM olddb.$t", 0) { }
                } catch (_: Throwable) {
                }
            }
        } finally {
            runCatching { driver.execute(null, "DETACH DATABASE olddb", 0) { } }
        }
    } finally {
        driver.close()
    }
}

/** 小组件 App Group(与 iosApp Widget Extension 共享)。 */
private const val GROUP_ID = "group.com.songci.selection"
