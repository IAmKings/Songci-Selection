package com.songci.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import songci.composeapp.generated.resources.Res
import java.io.File

actual suspend fun createDatabaseDriver(): SqlDriver {
    val dir = File(System.getProperty("user.home"), ".songci")
    dir.mkdirs()
    val dbFile = File(dir, DB_FILE_NAME)
    if (!dbFile.exists()) {
        dbFile.writeBytes(Res.readBytes(DB_RESOURCE_PATH))
    }
    return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
}
