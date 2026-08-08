package com.songci.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** 格律解析器基准:对齐 rhythmic_map.py 输出格式(扁平 "sketch|chars|forms|tune|rhythm")。 */
class RhythmicTest {

    @Test fun parseMapAndOf() {
        val raw = """{"水调歌头":"双调九十五字，前段九句四平韵，后段十句四平韵|95|11|中平中仄|----|1/3|元会曲/凯歌|水调歌头","浣溪沙":"双调四十二字|42|6|中仄中平|-J-Y|2/3|小庭花|浣溪沙"}"""
        val r = Rhythmic(Rhythmic.parseMap(raw))
        val spec = r.of("水调歌头")
        assertNotNull(spec)
        assertEquals(95, spec.chars)
        assertEquals(11, spec.forms)
        assertEquals("双调九十五字，前段九句四平韵，后段十句四平韵", spec.sketch)
        assertEquals("中平中仄", spec.tune)
        assertEquals("----", spec.rhythm)
        assertEquals(listOf(1, 3), spec.segEnds)
        assertEquals(listOf("元会曲", "凯歌"), spec.aliases)
        assertEquals(listOf("小庭花"), r.of("浣溪沙")?.aliases)
        assertEquals("水调歌头", spec.spec)
        assertNull(r.of("不存在的词牌"))
    }

    @Test fun bodiesAndMatchBody() {
        val raw = """{"水调歌头":"s1|95|11|中平|--|0/1|元会曲/凯歌|水调歌头"}"""
        val bodies = """{"水调歌头":"双调九十四字|94|中平|--|0/1|周紫芝;双调九十五字|95|中仄|JY|0/1|毛滂;双调九十六字|96|中平仄|--Y|0/2|苏轼"}"""
        val r = Rhythmic(Rhythmic.parseMap(raw), Rhythmic.parseMap(bodies))
        val list = r.bodiesOf("水调歌头")
        assertEquals(3, list.size)
        assertEquals(94, list[0].chars)
        assertEquals("周紫芝", list[0].author)   // 体作者
        // 多体匹配: 96 字词 → 第三体; 95 字 → 首体(优先)
        assertEquals(96, r.matchBody("水调歌头", 96)?.chars)
        assertEquals(95, r.matchBody("水调歌头", 95)?.chars)
        assertNull(r.matchBody("水调歌头", 100))
        assertEquals(emptyList(), r.bodiesOf("不存在的"))
    }

    @Test fun expandReturnsSameSpecCipai() {
        val raw = """{"出塞":"sk1|45|5|中平|--|0/1|空相忆/花自落/出塞|谒金门","谒金门":"sk2|45|5|中平|--|0/1|出塞|谒金门","浣溪沙":"sk3|42|6|中仄|JY|0/1|小庭花|浣溪沙"}"""
        val r = Rhythmic(Rhythmic.parseMap(raw))
        assertEquals(listOf("出塞", "谒金门"), r.expand("出塞"))      // 词牌名 → 同调
        assertEquals(listOf("出塞", "谒金门"), r.expand("空相忆"))    // 别名 → 同调
        assertEquals(listOf("浣溪沙"), r.expand("浣溪沙"))
        assertEquals(emptyList(), r.expand("不存在的"))
        assertEquals(emptyList(), r.expand(""))
    }

    @Test fun tuneLinesSplitsBySentenceAndSegment() {
        // 6 字 3 句 2 段: 句 1(平平)非段末, 句 2(仄仄)段末, 句 3(平平)段末(全词末)
        val spec = RhythmicSpec("sk", 6, 1, "平平仄仄平平", "-J-Y-Y", listOf(3, 5), emptyList(), "sk")
        val lines = spec.tuneLines()
        assertEquals(3, lines.size)
        assertEquals(listOf(listOf('平', '平'), listOf('仄', '仄'), listOf('平', '平')),
                     lines.map { it.chars.map { p -> p.first } })
        assertEquals(listOf(false, true, true), lines.map { it.segmentEnd })
        assertEquals(6, lines.sumOf { it.chars.size })
    }

    @Test fun parseSpecRejectsMalformed() {
        assertNull(Rhythmic.parseSpec("只有一段"))
        assertNull(Rhythmic.parseSpec("a|b|c|d"))              // 4 字段
        assertNull(Rhythmic.parseSpec("a|b|c|中平|--J|1/1||")) // tune/rhythm 长度不等
        assertNull(Rhythmic.parseSpec("a|3|1|中平仄|J-Y|2/1||")) // segs 末值 ≠ 末字索引
        assertEquals(3, Rhythmic.parseSpec("sk|3|1|中平仄|J-Y|0/2||")?.chars)
        assertEquals(emptyList(), Rhythmic.parseSpec("sk|3|1|中平仄|J-Y|0/2||")?.aliases)
    }
}
