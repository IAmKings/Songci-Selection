package com.songci.app.data

import kotlin.test.Test
import kotlin.test.assertEquals

/** 分段器基准:精确切分(首体字数一致)/变体兜底/单调单段。 */
class SegmenterTest {

    // 浣溪沙 42 字 2 段: 前 3 句 21 字(段末字索引 20), 后 3 句 21 字
    private val huanshisha = RhythmicSpec("双调四十二字", 42, 6,
        "中平中仄中平仄中平中仄中平平中仄中平中仄平中平中仄中平仄中平中仄中平平中仄中平中仄平",
        "-J-JY-J-JY", listOf(20, 41), emptyList(), "浣溪沙")

    private val content42 = "一曲新词酒一杯。\n去年天气旧亭台。\n夕阳西下几时回。\n无可奈何花落去。\n似曾相识燕归来。\n小园香径独徘徊。"

    @Test fun preciseSegmentsDoubleTone() {
        val segs = Segmenter.segment(content42, huanshisha)
        assertEquals(2, segs.size)
        assertEquals(3, segs[0].size)   // 前段 3 句
        assertEquals(3, segs[1].size)   // 后段 3 句
        assertEquals("一曲新词酒一杯。", segs[0][0])
        assertEquals("夕阳西下几时回。", segs[0][2])
        assertEquals("小园香径独徘徊。", segs[1][2])
    }

    @Test fun singleToneNoSplit() {
        val mono = RhythmicSpec("单调三十三字", 33, 1, "中仄仄平平", "J-JY", listOf(32), emptyList(), "如梦令")
        val content = "常记溪亭日暮。\n沉醉不知归路。\n兴尽晚回舟。\n误入藕花深处。\n争渡，争渡。\n惊起一滩鸥鹭。"  // 6+6+5+6+4+6=33
        val segs = Segmenter.segment(content, mono)
        assertEquals(1, segs.size)
        assertEquals(6, segs[0].size)
    }

    @Test fun fallbackOnCharMismatch() {
        // 变体词(96 字 vs 首体 93): 走行数对半兜底
        val spec = huanshisha
        val variant = content42.replace("一曲新词酒一杯。", "一曲新词酒一杯半。")   // +1 字
        val segs = Segmenter.segment(variant, spec)
        assertEquals(2, segs.size)
        assertEquals(3, segs[0].size)   // 6 行对半
    }

    @Test fun fallbackNoSpecAndShortContent() {
        assertEquals(listOf(listOf("一行。", "两行。")),
                     Segmenter.segment("一行。\n两行。", null))       // <4 行单段
        val long = (1..6).map { "第${it}句。" }.joinToString("\n")
        val segs = Segmenter.segment(long, null)
        assertEquals(2, segs.size)                                   // ≥4 行对半
        assertEquals(3, segs[0].size)
    }

    @Test fun charsStripPunctuation() {
        assertEquals("一曲新词酒一杯", Segmenter.chars("一曲新词酒一杯。"))
        assertEquals("去年天气旧亭台", Segmenter.chars("去年天气旧亭台\n"))
    }
}
