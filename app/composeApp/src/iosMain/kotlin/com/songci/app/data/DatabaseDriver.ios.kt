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
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createDatabaseDriver(): SqlDriver {
    val fileManager = NSFileManager.defaultManager
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).first() as String
    val path = "$documents/$DB_FILE_NAME"
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    // 缓存库与资源大小不一致(数据更新后旧库残留) → 重新复制
    val cachedSize = (fileManager.attributesOfItemAtPath(path, null)
        ?.get(NSFileSize) as? NSNumber)?.longValue
    if (!fileManager.fileExistsAtPath(path) || cachedSize != bytes.size.toLong()) {
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToFile(path, atomically = true)
    }
    // 预建库 user_version=1 与 schema.version 一致,驱动跳过建表
    return NativeSqliteDriver(SongciDb.Schema, path)
}
