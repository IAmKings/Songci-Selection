package com.songci.app.data

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.songci.app.data.db.SongciDb
import java.io.File
import songci.composeapp.generated.resources.Res

/** 在 MainActivity.onCreate 中赋值(activity 用于通知权限请求等运行时授权场景)。 */
object AppContextHolder {
    lateinit var context: Context
    var activity: Activity? = null
}

private fun File.readTextOrNull(): String? = if (exists()) readText() else null

actual suspend fun createDatabaseDriver(): SqlDriver {
    val ctx = AppContextHolder.context
    val dbPath = ctx.getDatabasePath(DB_FILE_NAME)
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    val resVersion = Res.readBytes("files/db_version.txt").decodeToString().trim()
    val versionFile = File(dbPath.parentFile, "$DB_FILE_NAME.version")
    // 缓存版本标记: 版本文件缺失/不一致(数据更新) → 临时副本 → 合并用户表 → 替换
    if (!dbPath.exists() || versionFile.readTextOrNull() != resVersion) {
        dbPath.parentFile?.mkdirs()
        val tmp = File(dbPath.parentFile, "$DB_FILE_NAME.new")
        tmp.outputStream().use { output ->
            // 与 iOS/桌面一致,走生成的资源访问(assets 内真实路径带 composeResources 前缀)
            output.write(bytes)
        }
        if (dbPath.exists()) {
            try {
                mergeUserData(dbPath.absolutePath, tmp.absolutePath)
            } catch (_: Throwable) {
                // 合并失败:降级直接替换(保启动优先,本次用户数据丢失)
            }
        }
        tmp.copyTo(dbPath, overwrite = true)
        tmp.delete()
        versionFile.writeText(resVersion)
    }
    // 预建库 user_version=1 与 schema.version 一致,驱动跳过建表
    return AndroidSqliteDriver(SongciDb.Schema, ctx, DB_FILE_NAME)
}

actual fun mergeUserData(oldDbPath: String, newDbPath: String) {
    var old: SQLiteDatabase? = null
    var new: SQLiteDatabase? = null
    try {
        old = SQLiteDatabase.openDatabase(oldDbPath, null, SQLiteDatabase.OPEN_READONLY)
        new = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READWRITE)
        // attachDatabase/detachDatabase 是隐藏 API(public SDK 无此符号):用 execSQL 原始 ATTACH
        new.execSQL("ATTACH DATABASE '${quoteLiteral(oldDbPath)}' AS olddb")
        USER_TABLES.forEach { t ->
            val exists = old.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(t)
            ).use { it.moveToFirst() }
            if (exists) new.execSQL("INSERT OR REPLACE INTO $t SELECT * FROM olddb.$t")
        }
    } finally {
        runCatching { new?.execSQL("DETACH DATABASE olddb") }
        old?.close()
        new?.close()
    }
}
