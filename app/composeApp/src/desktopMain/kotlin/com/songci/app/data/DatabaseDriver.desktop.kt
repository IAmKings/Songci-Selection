package com.songci.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import songci.composeapp.generated.resources.Res
import java.io.File

actual suspend fun createDatabaseDriver(): SqlDriver {
    val dir = File(System.getProperty("user.home"), ".songci")
    dir.mkdirs()
    val dbFile = File(dir, DB_FILE_NAME)
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    // 缓存库与资源大小不一致(数据更新后旧库残留,曾致词库缺失) → 重新复制
    if (!dbFile.exists() || dbFile.length() != bytes.size.toLong()) {
        dbFile.writeBytes(bytes)
    }
    return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
}
