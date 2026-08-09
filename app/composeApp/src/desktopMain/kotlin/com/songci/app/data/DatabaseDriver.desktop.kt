package com.songci.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import songci.composeapp.generated.resources.Res
import java.io.File

actual suspend fun createDatabaseDriver(): SqlDriver {
    val dir = File(System.getProperty("user.home"), ".songci")
    dir.mkdirs()
    val dbFile = File(dir, DB_FILE_NAME)
    val versionFile = File(dir, "$DB_FILE_NAME.version")
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    val resVersion = Res.readBytes("files/db_version.txt").decodeToString().trim()
    // 缓存版本标记: 版本文件缺失/不一致(数据更新) → 重新复制并记录版本
    if (!dbFile.exists() || versionFile.readTextOrNull() != resVersion) {
        dbFile.writeBytes(bytes)
        versionFile.writeText(resVersion)
    }
    // 小组件共享: 复制 db → macOS App Group 容器(WidgetKit 扩展读取),版本标记判新
    val groupDir = File(System.getProperty("user.home"),
        "Library/Group Containers/group.com.songci.selection")
    if (groupDir.exists() || groupDir.mkdirs()) {
        val groupDb = File(groupDir, DB_FILE_NAME)
        val groupVersion = File(groupDir, "$DB_FILE_NAME.version")
        if (!groupDb.exists() || groupVersion.readTextOrNull() != resVersion) {
            groupDb.writeBytes(bytes)
            groupVersion.writeText(resVersion)
        }
    }
    return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
}

private fun File.readTextOrNull(): String? =
    if (exists()) readText() else null
