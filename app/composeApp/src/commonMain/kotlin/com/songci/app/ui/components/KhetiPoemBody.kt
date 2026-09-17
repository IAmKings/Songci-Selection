package com.songci.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheti.KhetiMetrics
import com.kheti.KhetiRegion
import com.kheti.compose.KhetiBlockSpec
import com.kheti.compose.layoutKhetiBlocks
import com.kheti.layout.KhetiAlignment
import com.kheti.layout.KhetiEngine
import com.kheti.layout.KhetiHang
import com.kheti.layout.KhetiTextStyle
import com.kheti.layout.KhetiVerticalBlockSpec
import com.kheti.layout.KhetiVerticalEngine
import com.kheti.layout.drawKhetiText
import com.kheti.layout.drawKhetiVerticalText
import com.kheti.layout.layoutKhetiVerticalBlocks
import com.songci.app.theme.SongciColors
import com.songci.app.theme.poemFamily
import com.songci.app.ui.FontStyle

/**
 * Songci ↔ kheti 排版适配层(唯一 import com.kheti.* 的文件;kheti 类型不外泄到 screens 层)。
 *
 * - 横排 [KhetiPoemStanza]:一阕 = 一个 KhetiBlockSpec;左对齐 + 行末标点悬挂 + 中西文 ¼ 字宽间距 + 标点挤压;
 *   字号/行高 = DESIGN.md poem-body(宽 20sp/2.1,窄 18sp/2.05)× 设置页 fontScale。经 kheti 引擎层直连
 *   (kheti-compose 高层 API 只有枚举字号档位,无法承载 DESIGN.md 任意缩放;决策见 design.md §2)。
 * - 竖排 [KhetiVerticalPoemBody]:KhetiVerticalEngine 取代旧手写字符矩阵(列序右→左、标点悬挂列尾、縦中横、
 *   区域标点 CN);词牌 primary 块 + 右侧 primary 竖线、作者 stone 块、跨阕 24px 块间 gap 由 per-block 规格保留。
 * - iOS 回退缝:[useKhetiRendering] expect/actual;iOS 真机验证异常时把 ios actual 置 false → 走 DetailLegacy.kt 旧路径。
 *
 * 颜色一律 SongciColors token(禁硬编码);字体族由 poemFamily(fontStyle) 注入,设置三档(楷/宋/明)生效。
 */

/** 三端渲染开关(决策:三端同接 + iOS 验证回退;actual 见各平台 KhetiAvailability)。 */
expect fun useKhetiRendering(): Boolean

// ---------------------------------------------------------------- 横排

/** 横排正文样式:DESIGN.md poem-body(宽 20sp/行高 2.1,窄 18sp/行高 2.05)× fontScale;字距 0.02em(默认)。 */
@Composable
internal fun rememberKhetiPoemStyle(wide: Boolean, scale: Float, fontStyle: FontStyle): KhetiTextStyle {
    val font = poemFamily(fontStyle)
    return remember(wide, scale, font) {
        KhetiTextStyle(
            fontSize = (if (wide) 20.sp else 18.sp) * scale,
            lineHeight = (if (wide) 42.sp else 37.sp) * scale,
            fontFamily = font,
            size = KhetiMetrics.Size.Normal,
        )
    }
}

/** 纯函数:一阕 → 块规格(可在无组合环境单测)。 */
internal fun khetiStanzaSpec(stanza: List<String>, style: KhetiTextStyle, color: Color): KhetiBlockSpec =
    KhetiBlockSpec(
        text = stanza.joinToString("\n"),
        style = style,
        alignment = KhetiAlignment.Start,
        hang = KhetiHang.LineEnd,
        spacing = true,
        color = color,
    )

/**
 * 横排渲染一阕(段内多句以 \n 分行;行末悬挂标点溢出行宽,画布不裁剪)。
 * 宽度取父约束(BoxWithConstraints 组合期即得,无旧实现 onSizeChanged 的二次重排)。
 * 注意:横排**无**网格覆盖层——行盒线经真机评审认为破坏稿纸版面(2026-09-17),网格仅竖排保留。
 */
@Composable
internal fun KhetiPoemStanza(
    stanza: List<String>,
    style: KhetiTextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val engine = remember(measurer, density) { KhetiEngine(measurer, density) }
    BoxWithConstraints(modifier) {
        val limit = constraints.maxWidth.coerceAtLeast(1)
        val spec = remember(stanza, style, color) { khetiStanzaSpec(stanza, style, color) }
        val blockLayout = remember(spec, limit) { layoutKhetiBlocks(engine, listOf(spec), limit) }
        Canvas(
            Modifier
                .layout { measurable, _ ->
                    val w = blockLayout.width
                    val h = blockLayout.height
                    val placeable = measurable.measure(
                        Constraints(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h),
                    )
                    layout(w, h) { placeable.placeRelative(0, 0) }
                }
                .semantics { text = AnnotatedString(stanza.joinToString("\n")) },
        ) {
            blockLayout.blocks.forEach { b ->
                drawKhetiText(
                    measurer = measurer,
                    layout = b.layout,
                    style = b.style,
                    color = b.color,
                    density = density,
                    topOffset = b.top,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 竖排

/**
 * 纯函数:竖排块规格序列。layoutKhetiVerticalBlocks 首块排最右(词牌最右,阅读起点),
 * 每块各自持有样式/颜色;gapPx = 该块与其左侧相邻块的额外间距(**kheti 引擎按 px 工作**,
 * 调用方必须用 with(density){ X.dp.toPx() } 传入;且 gap 挂在**前(右)一块**上——挂错侧会落在内容左缘之外)。
 */
internal fun khetiVerticalBlockSpecs(
    title: String,
    author: String,
    stanzas: List<List<String>>,
    titleStyle: KhetiTextStyle,
    authorStyle: KhetiTextStyle,
    bodyStyle: KhetiTextStyle,
    gapMetaPx: Float,
    gapStanzaPx: Float,
): List<KhetiVerticalBlockSpec> = buildList {
    if (title.isNotBlank()) {
        add(KhetiVerticalBlockSpec(title, titleStyle, SongciColors.primary, gapPx = gapMetaPx))
    }
    if (author.isNotBlank()) {
        // 作者名前两个全角空格 = 列顶下沉两字位(旧版式节奏;替代旧 ownerStanza=-1 元数据列约定)
        add(KhetiVerticalBlockSpec("　　$author", authorStyle, SongciColors.stone, gapPx = gapMetaPx))
    }
    val stanzaBlocks = stanzas.filter { it.isNotEmpty() }
    // ⚠️ kheti gapPx 语义:「该块与其**左侧**相邻块」之间——阕距必须挂在**前(右)一阕**块上,
    // 否则落点在内容左缘之外、阕间实际为 0(2026-09-17 真机验收发现的第一版误挂 bug)。
    // 阕距语义 = 「空开一列」:gap 取一个列宽(colWidth),传统竖排分阕惯例,且随 fontScale 自动缩放;
    // 末阕的 gap 落在内容左缘之外,置 0 防画布出现幽灵留白。
    stanzaBlocks.forEachIndexed { k, stanza ->
        add(
            KhetiVerticalBlockSpec(
                stanza.joinToString("\n"), bodyStyle, SongciColors.nearBlack,
                gapPx = if (k == stanzaBlocks.lastIndex) 0f else gapStanzaPx,
            ),
        )
    }
}

/**
 * 竖排正文样式:列宽 = 行高 = **1.7×字号**(2026-09-17 真机评审定:旧 1.25 是旧字符矩阵的容量计算口径,
 * 非 DESIGN.md 语义;竖排的「行距」即列距,取 heti 上游竖排 1.5em 与横排 2.05em 之间的折中,
 * 列间空白 ≈0.7em,与横排疏朗感呼应;代价为同屏列数略少、横滑稍多)。
 */
@Composable
internal fun rememberKhetiVerticalBodyStyle(scale: Float, fontStyle: FontStyle): KhetiTextStyle {
    val font = poemFamily(fontStyle)
    return remember(scale, font) {
        val fontSize = 18.sp * scale
        KhetiTextStyle(fontSize = fontSize, lineHeight = fontSize * 1.7f, fontFamily = font)
    }
}

@Composable
private fun rememberKhetiVerticalTitleStyle(wide: Boolean, fontStyle: FontStyle): KhetiTextStyle {
    val font = poemFamily(fontStyle)
    return remember(wide, font) {
        val fontSize = if (wide) 46.sp else 36.sp
        KhetiTextStyle(fontSize = fontSize, lineHeight = fontSize * 1.2f, fontFamily = font, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun rememberKhetiVerticalAuthorStyle(fontStyle: FontStyle): KhetiTextStyle {
    val font = poemFamily(fontStyle)
    return remember(font) {
        KhetiTextStyle(fontSize = 15.sp, lineHeight = 22.5.sp, fontFamily = font)
    }
}

/**
 * 竖排词作正文(KhetiVerticalEngine 自绘,替代旧 VerticalPoemBody 字符矩阵)。
 * 结构与旧版一致:1dp 顶部横线 + 竖排区(weight,有限视口内分列)+ 底部「左右滑动」提示;
 * 竖排区仅横向滚动,初始滚到最右(首列右起)。
 * [showGrid]:竖排网格——按每个**列盒边界**画竖线(类似传统竖排书信纸;对应 kheti 竖排网格;仅竖排有,横排不画)。
 */
@Composable
internal fun KhetiVerticalPoemBody(
    stanzas: List<List<String>>,
    title: String,
    author: String,
    fontStyle: FontStyle,
    scale: Float,
    wide: Boolean,
    modifier: Modifier = Modifier,
    showGrid: Boolean = false,
) {
    val density = LocalDensity.current
    val titleStyle = rememberKhetiVerticalTitleStyle(wide, fontStyle)
    val authorStyle = rememberKhetiVerticalAuthorStyle(fontStyle)
    val bodyStyle = rememberKhetiVerticalBodyStyle(scale, fontStyle)
    val scrollState = rememberScrollState()
    val hasTitle = title.isNotBlank()
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val engine = remember(measurer, density) { KhetiVerticalEngine(measurer, density) }
        // 预留底部提示(~30dp)+ 收尾 8dp:防末列悬挂标点压提示(旧版同口径)
        val viewportPx = with(density) { maxHeight.toPx() }
        val reservePx = with(density) { (30.dp + 8.dp).toPx() }
        val limit = (viewportPx - reservePx).toInt().coerceAtLeast(1)
        val specs = remember(stanzas, title, author, titleStyle, authorStyle, bodyStyle, density) {
            khetiVerticalBlockSpecs(
                title, author, stanzas, titleStyle, authorStyle, bodyStyle,
                // kheti 引擎吃 px:dp 值一律经 density 换算(直接传字面量 = 单位 bug,真机 3x 下间距缩小为 1/3)
                gapMetaPx = with(density) { 8.dp.toPx() },
                // 阕距 = 「空开一列」:一个列宽(colWidth = 竖排行高),随 fontScale 缩放
                gapStanzaPx = with(density) { bodyStyle.lineHeight.toPx() },
            )
        }
        val blockLayout = remember(specs, limit) {
            layoutKhetiVerticalBlocks(engine, specs, limit, KhetiRegion.CN, KhetiHang.LineEnd, spacing = true)
        }
        // 首列右起:内容变化时执行一次(maxValue==0 期间让帧,等首帧布局出滚动范围)
        LaunchedEffect(stanzas, title, author) {
            while (scrollState.maxValue == 0) withFrameNanos {}
            scrollState.scrollTo(scrollState.maxValue)
        }
        Column {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SongciColors.line),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentWidth()
                        .horizontalScroll(scrollState)
                        .padding(top = if (wide) 36.dp else 28.dp, bottom = 8.dp),
                ) {
                    // 词牌 primary 竖线在画布右缘外 16px 处(旧版 Row 内装饰等距等宽);画布宽度随之扩
                    val ruleGapPx = with(density) { 16.dp.toPx() }
                    val ruleWpx = with(density) { 3.dp.toPx() }
                    val ruleExtra = if (hasTitle) (ruleGapPx + ruleWpx).toInt() else 0
                    val canvasW = blockLayout.width + ruleExtra
                    val titleBlockH = if (hasTitle) blockLayout.blocks.first().layout.size.height else 0
                    val semantic = buildString {
                        if (hasTitle) append(title).append('\n')
                        if (author.isNotBlank()) append(author).append('\n')
                        stanzas.forEach { s -> s.forEach { append(it).append('\n') } }
                    }.trimEnd()
                    Canvas(
                        Modifier
                            .layout { measurable, _ ->
                                val w = canvasW.coerceAtLeast(1)
                                val h = blockLayout.height.coerceAtLeast(1)
                                val placeable = measurable.measure(
                                    Constraints(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h),
                                )
                                layout(w, h) { placeable.placeRelative(0, 0) }
                            }
                            .semantics { text = AnnotatedString(semantic) },
                    ) {
                        if (showGrid) {
                            // 列盒边界竖线(垫底);块右缘补一条闭合线,与 kheti 竖排网格一致
                            val rule = SongciColors.stone.copy(alpha = 0.35f)
                            blockLayout.blocks.forEach { b ->
                                val h = b.layout.size.height.toFloat()
                                b.layout.columns.forEach { col ->
                                    val x = b.x + col.x
                                    drawLine(rule, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                                }
                                val right = b.x + b.layout.size.width
                                drawLine(rule, Offset(right, 0f), Offset(right, h), strokeWidth = 1f)
                            }
                        }
                        blockLayout.blocks.forEach { b ->
                            drawKhetiVerticalText(
                                measurer = measurer,
                                layout = b.layout,
                                style = b.style,
                                color = b.color,
                                density = density,
                                region = KhetiRegion.CN,
                                left = b.x,
                            )
                        }
                        if (hasTitle) {
                            drawRect(
                                color = SongciColors.primary,
                                topLeft = Offset(blockLayout.width + ruleGapPx, 0f),
                                size = Size(ruleWpx, titleBlockH.toFloat()),
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "◀ 左右滑动翻阅",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongciColors.stone,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}
