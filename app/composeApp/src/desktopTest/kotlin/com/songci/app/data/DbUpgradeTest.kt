package com.songci.app.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.songci.app.data.db.SongciDb
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 升级迁移:资源库更新时用户表合并(ATTACH 旧库 → INSERT OR REPLACE)。 */
class DbUpgradeTest {

    private fun dbCopy(name: String): File {
        val f = File(Files.createTempDirectory("songci-upgrade").toFile(), name)
        File("../../db/songci.db").canonicalFile.copyTo(f, overwrite = true)
        // 源库含开发期用户数据:清空,测试断言才与源库状态无关
        java.sql.DriverManager.getConnection("jdbc:sqlite:${f.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                USER_TABLES.forEach { st.execute("DELETE FROM $it") }
            }
        }
        return f
    }

    private fun open(db: File) = SongciDb(JdbcSqliteDriver("jdbc:sqlite:${db.absolutePath}"))

    @Test fun mergePreservesUserTablesIdempotent() = runBlocking {
        val oldDb = dbCopy("old.db")
        val newDb = dbCopy("new.db")
        val old = open(oldDb)
        old.songciDbQueries.insertFavorite(42)
        old.songciDbQueries.recordView(42)
        old.songciDbQueries.recordView(43)
        old.songciDbQueries.insertPool(123L, "42,43")

        mergeUserData(oldDb.absolutePath, newDb.absolutePath)
        mergeUserData(oldDb.absolutePath, newDb.absolutePath)   // 幂等:重复合并不产生重复行

        val merged = open(newDb)
        assertEquals(1, merged.songciDbQueries.favorites().executeAsList().size)
        assertTrue(merged.songciDbQueries.favorites().executeAsList().any { it.id == 42L })
        assertEquals(setOf(42L, 43L), merged.songciDbQueries.recentViews(10).executeAsList().map { it.id }.toSet())
        assertEquals("42,43", merged.songciDbQueries.poolByDate(123L).executeAsOneOrNull())
        // 语料表不受影响
        assertTrue(merged.songciDbQueries.poemById(42).executeAsOneOrNull() != null)
    }

    @Test fun mergeSkipsMissingTable() = runBlocking {
        // 旧版本库无 recent_views:合并不崩溃,favorites 仍保留
        val oldDb = dbCopy("old.db")
        val newDb = dbCopy("new.db")
        open(oldDb).songciDbQueries.insertFavorite(7)
        java.sql.DriverManager.getConnection("jdbc:sqlite:${oldDb.absolutePath}").use { conn ->
            conn.createStatement().use { st -> st.execute("DROP TABLE recent_views") }
        }

        mergeUserData(oldDb.absolutePath, newDb.absolutePath)

        assertTrue(open(newDb).songciDbQueries.favorites().executeAsList().any { it.id == 7L })
    }
}
