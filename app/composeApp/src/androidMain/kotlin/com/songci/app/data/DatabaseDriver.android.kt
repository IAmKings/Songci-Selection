package com.songci.app.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.songci.app.data.db.SongciDb
import songci.composeapp.generated.resources.Res

/** 在 MainActivity.onCreate 中赋值。 */
object AppContextHolder {
    lateinit var context: Context
}

actual suspend fun createDatabaseDriver(): SqlDriver {
    val ctx = AppContextHolder.context
    val dbPath = ctx.getDatabasePath(DB_FILE_NAME)
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    // 缓存库与资源大小不一致(数据更新后旧库残留) → 重新复制
    if (!dbPath.exists() || dbPath.length() != bytes.size.toLong()) {
        dbPath.parentFile?.mkdirs()
        dbPath.outputStream().use { output ->
            // 与 iOS/桌面一致,走生成的资源访问(assets 内真实路径带 composeResources 前缀)
            output.write(bytes)
        }
    }
    // 预建库 user_version=1 与 schema.version 一致,驱动跳过建表
    return AndroidSqliteDriver(SongciDb.Schema, ctx, DB_FILE_NAME)
}
