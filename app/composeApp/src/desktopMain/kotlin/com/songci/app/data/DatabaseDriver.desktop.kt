package com.songci.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import songci.composeapp.generated.resources.Res
import java.io.File

/** 升级复制:资源字节写临时副本 → mergeUserData 合并旧库用户表 → 原子替换。 */
private fun refreshDbFile(dbFile: File, tmpFile: File, bytes: ByteArray, version: String) {
    tmpFile.writeBytes(bytes)
    if (dbFile.exists()) {
        try {
            mergeUserData(dbFile.absolutePath, tmpFile.absolutePath)
        } catch (_: Throwable) {
            // 合并失败:降级直接替换(保启动优先,本次用户数据丢失)
        }
    }
    tmpFile.copyTo(dbFile, overwrite = true)
    tmpFile.delete()
    val versionFile = File(dbFile.parentFile, "$dbFile.name.version")
    versionFile.writeText(version)
}

actual suspend fun createDatabaseDriver(): SqlDriver {
    val dir = File(System.getProperty("user.home"), ".songci")
    dir.mkdirs()
    val dbFile = File(dir, DB_FILE_NAME)
    val bytes = Res.readBytes(DB_RESOURCE_PATH)
    val resVersion = Res.readBytes("files/db_version.txt").decodeToString().trim()
    // 缓存版本标记: 版本文件缺失/不一致(数据更新) → 复制 → 合并用户表 → 替换
    if (!dbFile.exists() || File(dir, "$DB_FILE_NAME.version").readTextOrNull() != resVersion) {
        refreshDbFile(dbFile, File(dir, "$DB_FILE_NAME.new"), bytes, resVersion)
    }
    // 小组件共享: 复制 db → macOS App Group 容器(WidgetKit 扩展读取),版本标记判新
    // 注意: 非沙盒 host 首次访问触发 TCC 弹窗("访问其他App的数据"),正式签名后仅一次授权;
    // 不做延迟(会阻塞数据加载链路,启动停在 LoadingScreen)
    val groupDir = File(System.getProperty("user.home"),
        "Library/Group Containers/group.com.songci.selection")
    if (groupDir.exists() || groupDir.mkdirs()) {
        val groupDb = File(groupDir, DB_FILE_NAME)
        if (!groupDb.exists() || File(groupDir, "$DB_FILE_NAME.version").readTextOrNull() != resVersion) {
            refreshDbFile(groupDb, File(groupDir, "$DB_FILE_NAME.new"), bytes, resVersion)
        }
    }
    return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
}

private fun File.readTextOrNull(): String? =
    if (exists()) readText() else null

actual fun mergeUserData(oldDbPath: String, newDbPath: String) {
    java.sql.DriverManager.getConnection("jdbc:sqlite:$newDbPath").use { conn ->
        conn.createStatement().use { st ->
            st.execute("ATTACH DATABASE '${quoteLiteral(oldDbPath)}' AS olddb")
            try {
                USER_TABLES.forEach { t ->
                    val exists = st.executeQuery(
                        "SELECT 1 FROM olddb.sqlite_master WHERE type='table' AND name='$t'"
                    ).use { it.next() }
                    if (exists) st.execute("INSERT OR REPLACE INTO $t SELECT * FROM olddb.$t")
                }
            } finally {
                runCatching { st.execute("DETACH DATABASE olddb") }
            }
        }
    }
}
