package com.songci.app.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.songci.app.data.db.SongciDb
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.FontScale
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 数据层基准测试:对照 db/songci.db 的 SQL 基准(sqlite3 CLI 实测)。 */
internal fun testRepo(): SongciRepository {
    val tmp = File(Files.createTempDirectory("songci-test").toFile(), "songci.db")
    tmp.parentFile?.deleteOnExit() // 先注册目录,逆序删除时文件先于目录
    tmp.deleteOnExit()
    // 测试工作目录为 composeApp/;仓库 db 在 ../../db/
    File("../../db/songci.db").canonicalFile.copyTo(tmp, overwrite = true)
    val db = SongciDb(JdbcSqliteDriver("jdbc:sqlite:${tmp.absolutePath}"))
    val map = File("src/commonMain/composeResources/files/dynasty_map.json").readText()
    val ev = File("src/commonMain/composeResources/files/dynasty_evidence.json").readText()
    val dynasty = Dynasty(Dynasty.parseMap(map), Dynasty.parseMap(ev))
    val rhythmic = Rhythmic(
        Rhythmic.parseMap(File("src/commonMain/composeResources/files/rhythmic_map.json").readText()),
        Rhythmic.parseMap(File("src/commonMain/composeResources/files/rhythmic_bodies.json").readText()),
    )
    return SongciRepository(db, dynasty, rhythmic)
}

class SongciRepositoryTest {

    @Test fun searchMatchesBaseline() = runBlocking {
        // 基准:content LIKE '%明月%' = 648 行;LIMIT 100
        val repo = testRepo()
        val results = repo.search("明月")
        assertEquals(100, results.size)
        // long_desc 匹配为合法路径: 作者简介含「明月」的作者 id 集合
        val descAuthors = repo.authors().filter { "明月" in it.longDesc }.map { it.id }.toSet()
        assertTrue(results.all {
            it.content.contains("明月") || it.authorName.contains("明月") || (it.authorId in descAuthors)
        })
    }

    @Test fun rhythmicFilterFuzzyMatches() = runBlocking {
        // 词牌筛选模糊:「水」→ 水调歌头/水龙吟(精确匹配曾致组合搜索 0 结果)
        val repo = testRepo()
        val contains = repo.poemsByRhythmicContains("水")
        assertTrue(contains.isNotEmpty())
        assertTrue(contains.all { it.rhythmic.contains("水") })
        // 组合场景: 友古(蔡伸) + 词牌含水 → 水调歌头/水龙吟
        val combo = repo.search("友古").filter { it.rhythmic.contains("水") }
        assertTrue(combo.isNotEmpty())
        assertTrue(combo.all { it.authorName == "蔡伸" })
    }

    @Test fun searchByAuthorAliasReturnsAllPoems() = runBlocking {
        // 作者号(友古居士→蔡伸 175 首)命中 long_desc → 该作者词作全出(LIMIT 100)
        val results = testRepo().search("友古居士")
        assertEquals(100, results.size)                       // LIMIT 截断
        assertTrue(results.all { it.authorName == "蔡伸" })
    }

    @Test fun searchByAliasReturnsSameSpecPoems() = runBlocking {
        // 回归:单键同调组别名(青衫湿→人月圆 15 首)搜索必须命中,不因展开数=1 被短路
        val results = testRepo().search("青衫湿")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.rhythmic == "人月圆" })
    }

    @Test fun poemByIdReturnsPoem() = runBlocking {
        val poem = testRepo().poemById(42)
        assertEquals(42L, poem?.id)
        assertTrue(!poem!!.content.isBlank())
    }

    @Test fun favoritesRoundtrip() = runBlocking {
        val r = testRepo()
        r.addFavorite(42)
        assertTrue(r.favorites().any { it.poem.id == 42L })
        r.removeFavorite(42)
        assertTrue(r.favorites().none { it.poem.id == 42L })
    }

    @Test fun recentViewsDedupCapOrder() = runBlocking {
        val r = testRepo()
        val ids = r.randomPoems(35).map { it.id }
        assertEquals(35, ids.size)
        // 5ms 间隔:viewed_at 为 epoch 毫秒,保证时间戳严格递增(同毫秒排序不稳)
        ids.forEach { id -> r.recordView(id); kotlinx.coroutines.delay(5) }
        val recent = r.recentViews()
        assertEquals(30, recent.size)                      // 上限裁剪:35 → 30
        assertEquals(ids.last(), recent.first().id)        // 最新在前
        assertTrue(recent.map { it.id }.all { it in ids.drop(5).toSet() })  // 最旧 5 条被淘汰
        r.recordView(ids.first())                          // 重复查看 → 去重置顶
        val after = r.recentViews()
        assertEquals(30, after.size)
        assertEquals(1, after.count { it.id == ids.first() })   // 无重复条目
        assertEquals(ids.first(), after.first().id)             // 置顶
    }

    @Test fun dynastySamples() = runBlocking {
        val r = testRepo()
        val dynasty = r.dynasty
        val byName = r.authors().associateBy { it.name }
        assertEquals("北宋", dynasty.of(byName["苏轼"]!!.id))
        assertEquals("南宋", dynasty.of(byName["李清照"]!!.id))
        assertEquals("北宋", dynasty.of(byName["范仲淹"]!!.id))
        assertEquals("五代", dynasty.of(byName["李煜"]!!.id))
        assertEquals("唐", dynasty.of(byName["温庭筠"]!!.id))
        assertEquals("宋", dynasty.of(byName["巴谈"]!!.id))
        // 年份证据:苏轼 1037-1101,巴谈(无年份)无证据
        assertEquals("1037-1101", dynasty.evidenceOf(byName["苏轼"]!!.id))
        assertNull(dynasty.evidenceOf(byName["巴谈"]!!.id))
    }

    @Test fun randomPoemsReturnsLimit() = runBlocking {
        val poems = testRepo().randomPoems(20)
        assertEquals(20, poems.size)
        assertEquals(20, poems.map { it.id }.toSet().size)
    }

    @Test fun randomPoemsFiltersAbnormalChars() = runBlocking {
        // 回归:随机诗与推荐池同源过滤(widget/首页随机入口共用),异常字符不推荐
        repeat(5) {
            testRepo().randomPoems(20).forEach { p ->
                assertTrue("⿰" !in p.rhythmic && "⿰" !in p.content, "含 ⿰: ${p.rhythmic}")
                assertTrue("𠴇" !in p.content && "𫍙" !in p.content, "含缺字: ${p.rhythmic}")
                assertTrue(p.rhythmic.length <= 12, "词牌超长: ${p.rhythmic}")
                assertTrue(p.content.lines().size >= 2, "单行词: ${p.rhythmic}")
            }
        }
    }

    @Test fun poemsByRhythmicBaseline() = runBlocking {
        // 基准:rhythmic='水调歌头' = 743 行;LIMIT 100
        val poems = testRepo().poemsByRhythmic("水调歌头")
        assertEquals(100, poems.size)
        assertTrue(poems.all { it.rhythmic == "水调歌头" })
    }
}

/** AC1:字号档位持久化往返(桌面 Properties 存储,隔离文件)。 */
class SettingsPersistenceTest {

    private val testFile = File(Files.createTempDirectory("songci-settings").toFile(), "settings.properties")
    init { testFile.parentFile?.deleteOnExit(); testFile.deleteOnExit() }

    @Test fun fontScaleRoundtrip() {
        System.setProperty(SETTINGS_FILE_PROPERTY, testFile.absolutePath)
        try {
            assertNull(loadFontScaleName())
            saveFontScaleName("LARGE")
            assertEquals("LARGE", loadFontScaleName())
            saveFontScaleName("SMALL")
            assertEquals("SMALL", loadFontScaleName())
        } finally {
            System.clearProperty(SETTINGS_FILE_PROPERTY)
            testFile.delete()
        }
    }

    @Test fun notificationPrefsRoundtrip() {
        System.setProperty(SETTINGS_FILE_PROPERTY, testFile.absolutePath)
        try {
            // 默认值
            assertEquals(NotificationPrefs(), loadNotificationPrefs())
            // 全字段往返(含 lastScheduledDay 滚动窗口标记)
            saveNotificationPrefs(NotificationPrefs(enabled = true, hour = 7, minute = 30, lastScheduledDay = 20000L))
            assertEquals(NotificationPrefs(enabled = true, hour = 7, minute = 30, lastScheduledDay = 20000L), loadNotificationPrefs())
            // 关闭状态与 0 点(00:00 合法,非默认 21:00)
            saveNotificationPrefs(NotificationPrefs(enabled = false, hour = 0, minute = 0))
            assertEquals(NotificationPrefs(enabled = false, hour = 0, minute = 0), loadNotificationPrefs())
        } finally {
            System.clearProperty(SETTINGS_FILE_PROPERTY)
            testFile.delete()
        }
    }
}

/** 字号状态:更新即时生效(VM 层)。 */
class FontScaleStateTest {

    private val settingsFile = File(Files.createTempDirectory("songci-vmtest").toFile(), "settings.properties")
    init { settingsFile.parentFile?.deleteOnExit(); settingsFile.deleteOnExit() }

    @Test fun fontScaleTransitions() = runBlocking {
        System.setProperty(SETTINGS_FILE_PROPERTY, settingsFile.absolutePath)
        try {
            val vm = AppViewModel(testRepo())
            vm.updateFontScale(FontScale.LARGE)
            assertEquals(FontScale.LARGE, vm.fontScale)
            assertEquals(1.15f, vm.fontScale.scale)
            vm.updateFontScale(FontScale.SMALL)
            assertEquals(0.9f, vm.fontScale.scale)
        } finally {
            System.clearProperty(SETTINGS_FILE_PROPERTY)
            settingsFile.delete()
        }
    }
}
