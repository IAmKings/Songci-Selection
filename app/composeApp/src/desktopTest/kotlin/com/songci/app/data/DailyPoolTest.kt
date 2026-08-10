package com.songci.app.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 每日推荐池:同日同池、跨日换新、不重复、异常过滤、重置循环。 */
class DailyPoolTest {

    @Test
    fun `同一天多次调用返回同一池`() = runBlocking {
        val repo = testRepo()
        val day1 = repo.dailyPool(20000)
        val day2 = repo.dailyPool(20000)
        assertEquals(20, day1.size)
        assertEquals(day1.map { it.id }, day2.map { it.id })
    }

    @Test
    fun `不同日期池不同且不重复`() = runBlocking {
        val repo = testRepo()
        val day1 = repo.dailyPool(20000)
        val day2 = repo.dailyPool(20001)
        val day3 = repo.dailyPool(20002)
        val all = day1 + day2 + day3
        assertEquals(60, all.size)
        assertEquals(60, all.map { it.id }.distinct().size, "连续多天不得出现重复词")
    }

    @Test
    fun `异常词不进池`() = runBlocking {
        val repo = testRepo()
        val pool = repo.dailyPool(20010)
        pool.forEach { poem ->
            assertTrue(!poem.rhythmic.contains('⿰'), "词牌含 ⿰ 不应入池: ${poem.rhythmic}")
            assertTrue(poem.rhythmic.length <= 12, "词牌超长不应入池: ${poem.rhythmic}")
            assertTrue(poem.content.lines().size >= 2, "内容单行不应入池")
            listOf("⿰", "𠴇", "𫍙").forEach { ch ->
                assertTrue(!poem.content.contains(ch), "异常字符词不应入池: $ch")
            }
        }
    }

    @Test
    fun `标记耗尽后重置循环`() = runBlocking {
        val repo = testRepo()
        // 模拟 3 年推进:每天 20 首,累计 ≥21340 后应重置且仍能出池
        var lastIds = setOf<Long>()
        var pool: List<Poem> = emptyList()
        repeat(1075) { day ->   // 21340/20 = 1067 天 + 余量
            pool = repo.dailyPool(30000L + day)
            lastIds += pool.map { it.id }
        }
        assertEquals(20, pool.size, "重置后仍正常出池")
        // 重置逻辑:全量覆盖后继续(不抛异常即通过;池大小恒定)
        assertTrue(lastIds.size <= 21340 + 40, "累计不重复量不超过词库+当日池")
    }
}
