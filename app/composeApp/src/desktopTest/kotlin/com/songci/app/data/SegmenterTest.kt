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
        assertEquals(3, segs[1].size)   // 后段 3 句        assertEquals("一曲新词酒一杯。", segs[0][0])
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

    // ---- splitVerticalColumns:竖排按列高标点截断 ----

    @Test fun verticalShortStaysOneColumn() {
        // 单句短于列容量 → 单列,保留全部字符
        val cols = splitVerticalColumns(listOf(listOf("明月几时有。")), maxChars = 10)
        assertEquals(1, cols.size)
        assertEquals("明月几时有。", cols[0].chars.joinToString(""))
        assertEquals(0, cols[0].ownerStanza)
    }

    @Test fun verticalLongCutsAtPunct() {
        // 超长句:列容量 8,应优先在标点后截断,续到下一列
        val cols = splitVerticalColumns(listOf(listOf("不定如萍泛，暂抛江沔，又留连京国。")), maxChars = 8)
        // 第一列应在某标点后断(不超过 8 字)
        assert(cols[0].chars.size <= 8) { "首列超容量" }
        assertEquals("，", cols[0].chars.last().toString())   // 末字符是标点
        // 全文拼接不丢字
        assertEquals("不定如萍泛，暂抛江沔，又留连京国。", cols.flatMap { it.chars }.joinToString(""))
    }

    @Test fun verticalStanzaGapPreserved() {
        // 双阕:跨阕交界的列 ownerStanza 不同,用于插入空列
        val cols = splitVerticalColumns(
            listOf(listOf("上阕第一句。", "上阕第二句。"), listOf("下阕第一句。", "下阕第二句。")),
            maxChars = 10,
        )
        assert(cols.any { it.ownerStanza == 0 })
        assert(cols.any { it.ownerStanza == 1 })
        // 跨阕边界存在 ownerStanza 变化
        assert(cols.zipWithNext().any { (a, b) -> a.ownerStanza != b.ownerStanza })
        // 全部 4 句字符不丢失
        val text = cols.flatMap { it.chars }.joinToString("")
        assertEquals(4, text.count { it == '句' })
    }

    @Test fun verticalZeroCapacityEmpty() {
        assertEquals(emptyList<VerticalColumn>(), splitVerticalColumns(listOf(listOf("测试。")), maxChars = 0))
    }

    @Test fun verticalShortSentenceNotMergedIntoLoneLongFragment() {
        // 短句(5字)后跟超长句:短句单独成列,超长句拆段独占后续列,绝不拼接
        val cols = splitVerticalColumns(
            listOf(listOf("月落乌啼。", "不定如萍泛，暂抛江沔，又留连京国。")),
            maxChars = 10,
        )
        val texts = cols.map { it.chars.joinToString("") }
        // 短句应独立成一列(不拼长句片段)
        assert(texts.any { it == "月落乌啼。" }) { "短句未被独立成列: $texts" }
        // 长句拆段,每段以标点结尾且 ≤ 容量,段间拼接完整不丢字
        val long = cols.filter { it != cols.first() }.flatMap { it.chars }.joinToString("")
        assertEquals("不定如萍泛，暂抛江沔，又留连京国。", long)
        // 任一长句段末都是标点(在标点处断列)
        assert(long.endsWith("。"))
    }

    @Test fun verticalShortSentencesStaySeparateColumns() {
        // 全部短句:每个句子独占一列,绝不合并(句号分列)
        val cols = splitVerticalColumns(
            listOf(listOf("月落乌啼。", "不避风霜。", "憔悴容颜。", "更说相思。")),
            maxChars = 10,
        )
        val texts = cols.map { it.chars.joinToString("") }
        assertEquals(4, texts.size)   // 4 句 → 4 独立列
        assertEquals("月落乌啼。", texts[0])
        assertEquals("不避风霜。", texts[1])
        assertEquals("憔悴容颜。", texts[2])
        assertEquals("更说相思。", texts[3])
    }
}
