package com.songci.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** 格律解析器基准:对齐 rhythmic_map.py 输出格式(扁平 "sketch|chars|forms|tune|rhythm")。 */
class RhythmicTest {

    @Test fun parseMapAndOf() {
        val raw = """{"水调歌头":"双调九十五字，前段九句四平韵，后段十句四平韵|95|11|中平中仄|----|1/3","浣溪沙":"双调四十二字|42|6|中仄中平|-J-Y|2/3"}"""
        val r = Rhythmic(Rhythmic.parseMap(raw))
        val spec = r.of("水调歌头")
        assertNotNull(spec)
        assertEquals(95, spec.chars)
        assertEquals(11, spec.forms)
        assertEquals("双调九十五字，前段九句四平韵，后段十句四平韵", spec.sketch)
        assertEquals("中平中仄", spec.tune)
        assertEquals("----", spec.rhythm)
        assertEquals(listOf(1, 3), spec.segEnds)
        assertNull(r.of("不存在的词牌"))
    }

    @Test fun tuneLinesSplitsBySentenceAndSegment() {
        // 6 字 3 句 2 段: 句 1(平平)非段末, 句 2(仄仄)段末, 句 3(平平)段末(全词末)
        val spec = RhythmicSpec("sk", 6, 1, "平平仄仄平平", "-J-Y-Y", listOf(3, 5))
        val lines = spec.tuneLines()
        assertEquals(3, lines.size)
        assertEquals(listOf(listOf('平', '平'), listOf('仄', '仄'), listOf('平', '平')),
                     lines.map { it.chars.map { p -> p.first } })
        assertEquals(listOf(false, true, true), lines.map { it.segmentEnd })
        assertEquals(6, lines.sumOf { it.chars.size })
    }

    @Test fun parseSpecRejectsMalformed() {
        assertNull(Rhythmic.parseSpec("只有一段"))
        assertNull(Rhythmic.parseSpec("a|b|c|d"))          // 4 字段
        assertNull(Rhythmic.parseSpec("a|b|c|中平|--J|1/1")) // tune/rhythm 长度不等
        assertNull(Rhythmic.parseSpec("a|3|1|中平仄|J-Y|2/1")) // segs 末值 ≠ 末字索引
        assertEquals(3, Rhythmic.parseSpec("sk|3|1|中平仄|J-Y|0/2")?.chars)
    }
}
