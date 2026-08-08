package com.songci.app.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.songci.app.data.db.SongciDb
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 数据层基准测试:对照 db/songci.db 的 SQL 基准(sqlite3 CLI 实测)。 */
class SongciRepositoryTest {

    private fun repo(): SongciRepository {
        val tmp = File(Files.createTempDirectory("songci-test").toFile(), "songci.db")
        // 测试工作目录为 composeApp/;仓库 db 在 ../../db/
        File("../../db/songci.db").canonicalFile.copyTo(tmp, overwrite = true)
        val db = SongciDb(JdbcSqliteDriver("jdbc:sqlite:${tmp.absolutePath}"))
        val json = File("src/commonMain/composeResources/files/dynasty_map.json").readText()
        val dynasty = Dynasty(Dynasty.parseMap(json))
        return SongciRepository(db, dynasty)
    }

    @Test fun searchMatchesBaseline() = runBlocking {
        // 基准:content LIKE '%明月%' = 648 行;LIMIT 100
        val results = repo().search("明月")
        assertEquals(100, results.size)
        assertTrue(results.all { it.content.contains("明月") || it.authorName.contains("明月") })
    }

    @Test fun poemByIdReturnsPoem() = runBlocking {
        val poem = repo().poemById(42)
        assertEquals(42L, poem?.id)
        assertTrue(!poem!!.content.isBlank())
    }

    @Test fun favoritesRoundtrip() = runBlocking {
        val r = repo()
        r.addFavorite(42)
        assertTrue(r.favorites().any { it.poem.id == 42L })
        r.removeFavorite(42)
        assertTrue(r.favorites().none { it.poem.id == 42L })
    }

    @Test fun dynastySamples() = runBlocking {
        val r = repo()
        val dynasty = r.dynasty
        val byName = r.authors().associateBy { it.name }
        assertEquals("北宋", dynasty.of(byName["苏轼"]!!.id))
        assertEquals("南宋", dynasty.of(byName["李清照"]!!.id))
        assertEquals("北宋", dynasty.of(byName["范仲淹"]!!.id))
    }

    @Test fun randomPoemsReturnsLimit() = runBlocking {
        val poems = repo().randomPoems(20)
        assertEquals(20, poems.size)
        assertEquals(20, poems.map { it.id }.toSet().size)
    }

    @Test fun poemsByRhythmicBaseline() = runBlocking {
        // 基准:rhythmic='水调歌头' = 743 行;LIMIT 100
        val poems = repo().poemsByRhythmic("水调歌头")
        assertEquals(100, poems.size)
        assertTrue(poems.all { it.rhythmic == "水调歌头" })
    }
}
