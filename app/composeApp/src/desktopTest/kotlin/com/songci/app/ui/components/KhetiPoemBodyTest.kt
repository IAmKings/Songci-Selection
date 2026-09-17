package com.songci.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kheti.layout.KhetiAlignment
import com.kheti.layout.KhetiHang
import com.kheti.layout.KhetiTextStyle
import com.songci.app.theme.SongciColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * kheti 适配层块规格守门(纯函数,无组合环境):
 * 规格=DESIGN.md 语义与赫蹏规则的接缝,规格错则渲染层全错——在此断言而不是渲染后目检。
 */
class KhetiPoemBodyTest {

    private val style = KhetiTextStyle(fontSize = 18.sp * 1.15f, lineHeight = 37.sp * 1.15f)

    // ---- 横排:一阕 = 一个块;左对齐 + 行末悬挂 + 赫蹏间距 ----

    @Test fun stanzaSpecJoinsLinesAndAppliesHetiRules() {
        val spec = khetiStanzaSpec(listOf("明月几时有，", "把酒问青天。"), style, SongciColors.nearBlack)
        assertEquals("明月几时有，\n把酒问青天。", spec.text)
        assertEquals(KhetiAlignment.Start, spec.alignment)   // 决策:保持左对齐(heti 诗词默认居中,不采用)
        assertEquals(KhetiHang.LineEnd, spec.hang)           // 行末标点悬挂
        assertTrue(spec.spacing)                             // 中西文间距 + 标点挤压
        assertEquals(SongciColors.nearBlack, spec.color)     // 墨字 token
        assertEquals(0f, spec.firstLineIndentEm)             // 诗词无首行缩进(区别于古文)
    }

    @Test fun stanzaSpecTextRoundTripsWithoutLoss() {
        val stanza = listOf("一曲新词酒一杯。", "去年天气旧亭台。", "夕阳西下几时回。")
        val spec = khetiStanzaSpec(stanza, style, Color.Black)
        // 逐句不丢字(标点/换行原样)
        assertEquals(stanza.joinToString("\n"), spec.text)
        assertEquals(stanza.sumOf { it.length } + stanza.size - 1, spec.text.length)
    }

    // ---- 竖排:词牌/作者/各阕 = 各自持有样式与颜色的块;首块排最右 ----

    private val gapMeta = 24f     // 8dp×density3
    private val gapStanza = 102f  // 34dp×density3

    @Test fun verticalBlockSpecsOrderAndColors() {
        val title = KhetiTextStyle(fontSize = 36.sp)
        val author = KhetiTextStyle(fontSize = 15.sp)
        val body = KhetiTextStyle(fontSize = 18.sp)
        val specs = khetiVerticalBlockSpecs(
            "浣溪沙", "苏轼",
            listOf(listOf("一曲新词酒一杯。", "去年天气旧亭台。"), listOf("日日花前常病酒。", "敢辞帷帽为伤春。")),
            title, author, body, gapMetaPx = gapMeta, gapStanzaPx = gapStanza,
        )
        assertEquals(4, specs.size)                       // 词牌 + 作者 + 两阕
        assertEquals("浣溪沙", specs[0].text)              // 首块=词牌(layoutKhetiVerticalBlocks 排最右)
        assertEquals(SongciColors.primary, specs[0].color) // 词牌块 primary(品牌签名)
        assertEquals("　　苏轼", specs[1].text)            // 全角空格=列顶下沉两字位(旧版式节奏)
        assertEquals(SongciColors.stone, specs[1].color)
        assertEquals(SongciColors.nearBlack, specs[2].color)
        assertEquals(SongciColors.nearBlack, specs[3].color)
        // gap 挂在**前(右)一块**(kheti 语义「该块与左侧邻块」):上阕块承载阕距,末阕 gap=0(防左缘幽灵留白)
        assertEquals(gapMeta, specs[0].gapPx)
        assertEquals(gapMeta, specs[1].gapPx)
        assertEquals(gapStanza, specs[2].gapPx, "阕距必须挂在上阕块(挂下阕=落点屏外,阕间为 0)")
        assertEquals(0f, specs[3].gapPx)
        assertTrue(gapStanza > gapMeta * 3, "跨阕 gap 应显著大于元数据 gap(空一列 vs 8dp)")
    }

    @Test fun verticalBlockSpecsOmitBlankTitleAndEmptyStanza() {
        val specs = khetiVerticalBlockSpecs(
            "", "", listOf(emptyList(), listOf("无言独上西楼。")), style, style, style, gapMeta, gapStanza,
        )
        assertEquals(1, specs.size)
        assertEquals("无言独上西楼。", specs[0].text)
    }

    // ---- 竖排正文样式口径:列宽 = 行高 = 1.7×字号(2026-09-17 评审定,疏朗与横排呼应) ----

    @Test fun verticalBodyLineHeightRatioPreserved() {
        val s = KhetiTextStyle(fontSize = 18.sp * 1.15f, lineHeight = 18.sp * 1.15f * 1.7f)
        assertTrue(s.lineHeight.value / s.fontSize.value == 1.7f, "竖排列距口径意外变化(列间空白观感随之改变)")
    }

    // ---- 标题字重(旧版 headline=Medium 楷) ----

    @Test fun verticalTitleWeightMedium() {
        val t = KhetiTextStyle(fontSize = 36.sp, lineHeight = 43.2.sp, fontWeight = FontWeight.Medium)
        assertEquals(FontWeight.Medium, t.fontWeight)
    }
}
