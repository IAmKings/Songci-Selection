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
import platform.Foundation.create
import platform.Foundation.writeToFile

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
    // 缓存版本标记: 版本文件缺失/不一致(数据更新) → 重新复制并记录版本
    val cachedVersion = fileManager.contentsAtPath(versionPath)?.let {
        NSString.create(data = it, encoding = NSUTF8StringEncoding) as String
    }?.trim()
    if (!fileManager.fileExistsAtPath(path) || cachedVersion != resVersion) {
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToFile(path, atomically = true)
        (resVersion as NSString).writeToFile(versionPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
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
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToFile(groupDb, atomically = true)
            (resVersion as NSString).writeToFile(groupVersion, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        }
    }
    // 预建库 user_version=1 与 schema.version 一致,驱动跳过建表
    return NativeSqliteDriver(SongciDb.Schema, path)
}

/** 小组件 App Group(与 iosApp Widget Extension 共享)。 */
private const val GROUP_ID = "group.com.songci.selection"
