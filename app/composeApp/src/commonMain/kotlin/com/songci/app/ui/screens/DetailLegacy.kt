package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.songci.app.data.RhythmicSpec
import com.songci.app.theme.SongciColors

/**
 * 详情页旧排版路径(iOS 回退副本):kheti 接入前的手写实现原样搬移,仅供 [useKhetiRendering]=false 平台使用。
 * 回退开关见 ui/components/KhetiAvailability.ios.kt;iOS 真机验证通过后本文件整体删除(implement.md 步骤 6)。
 */

/** 竖排单列块:标点优先截断后的一列;ownerStanza 用于跨阕空列,chars 为该列逐字。 */
private data class VerticalColumn(val ownerStanza: Int, val chars: List<Char>)

/**
 * 竖排按列高截断:以"句子"为硬性一列单元——每个句子独占一列,绝不合并。
 * 短句(≤maxChars)直接成列;超长句(>maxChars)内部按标点贪心拆段为连续多列(段间向左续)。
 * 跨阕交界保留 ownerStanza 变化以插入空列。
 */
private fun splitVerticalColumns(segments: List<List<String>>, maxChars: Int): List<VerticalColumn> {
    if (maxChars <= 0) return emptyList()
    val punct = "，。、；：·！？…"
    val columns = mutableListOf<VerticalColumn>()
    segments.forEachIndexed { stanza, lines ->
        lines.forEach { line ->
            if (line.length > maxChars) {
                // 超长句:内部按标点贪心拆段,每段独立列,段间连续
                val chars = line.toList()
                var i = 0
                while (i < chars.size) {
                    val window = chars.subList(i, minOf(i + maxChars, chars.size))
                    val cut = findPunctCut(window, punct)
                    val take = if (cut.first.isNotEmpty()) cut.first.size
                               else (i + maxChars).coerceAtMost(chars.size) - i   // 无标点兜底整窗
                    columns.add(VerticalColumn(stanza, chars.subList(i, i + take)))
                    i += take
                }
            } else {
                // 短句:独占一列,不与其他句合并(句号分列)
                columns.add(VerticalColumn(stanza, line.toList()))
            }
        }
    }
    return columns
}

/** 在缓冲字符中寻找最接近末尾的标点作为截断点,避免在词中硬切。返回(截断前, 剩余)。 */
private fun findPunctCut(buf: List<Char>, punct: String): Pair<List<Char>, List<Char>> {
    for (i in buf.indices.reversed()) {
        if (buf[i] in punct) return buf.subList(0, i + 1) to buf.subList(i + 1, buf.size)
    }
    return buf to emptyList()   // 无标点,整段截断
}

/**
 * 将横排单句按中文标点优先断行,返回便于逐行渲染的分段(每段末为标点,避免拆词)。
 * 若整句不超 maxWidthPx 则返回单元素;否则贪心拼接 token,超宽即断在标点后;
 * 单个 token 仍超宽时(罕见过长)整段返回,交由 Text 兜底断行。
 */
private fun punctuatedLines(
    line: String,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    maxWidthPx: Int,
): List<String> {
    val whole = textMeasurer.measure(AnnotatedString(line), style)
    if (whole.size.width <= maxWidthPx) return listOf(line)
    // 按标点切成 token(保留标点)。正则:标点后切开。
    val tokens = line.split(Regex("(?<=[，。、；：·！？…])"))
        .filter { it.isNotEmpty() }
    if (tokens.size <= 1) return listOf(line)   // 无标点可断,兜底 Text 自行断行
    val rows = mutableListOf<String>()
    var cur = ""
    for (t in tokens) {
        val trial = cur + t
        if (trial.isNotBlank() && textMeasurer.measure(AnnotatedString(trial), style).size.width > maxWidthPx && cur.isNotBlank()) {
            rows += cur
            cur = t
        } else {
            cur = trial
        }
    }
    if (cur.isNotBlank()) rows += cur
    return rows
}

/** 旧横排单阕渲染:逐句 + 标点贪心断行(无悬挂/无中西文间距)。 */
@Composable
internal fun StanzaColumnLegacy(
    lines: List<String>,
    scale: Float,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    var maxWidthPx by remember { mutableStateOf(0) }
    Column(
        modifier = modifier.onSizeChanged { maxWidthPx = it.width },
    ) {
        lines.forEach { line ->
            val rows = if (maxWidthPx > 0) punctuatedLines(line, style, textMeasurer, maxWidthPx) else listOf(line)
            rows.forEach { seg ->
                Text(
                    seg,
                    style = style,
                    color = SongciColors.nearBlack,
                )
            }
        }
    }
}

/**
 * 旧竖排诗词正文(方案 A:字符矩阵 + Column)。
 * 每句拆成单字符 → 逐字纵向 Column(一列 = 一句);列高=视口高度,超长句按标点截断换列(避免纵向滚动)。
 * 上/下阕交界处空开一列(跨阕大间距)。多句多列从右向左排(首句最右);初始滚到最右;底部提示横滑。
 * 滚动:仅外层横向(多列),列高受视口约束,无需纵向滚动阅列。
 */
@Composable
internal fun VerticalPoemBodyLegacy(
    content: String,
    scale: Float,
    spec: RhythmicSpec?,
    title: String,
    author: String,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val segments = com.songci.app.data.Segmenter.segment(content, spec)          // List<List<String>>:上/下阕
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val compactFontSize = MaterialTheme.typography.bodyMedium.fontSize * scale
    val lineHeightPx = with(density) { (compactFontSize * 1.25f).toPx() }   // 与单字渲染行距一致,保列高不超屏
    // 方案 C:BoxWithConstraints 在组合期即得竖排区 maxHeight,首帧就是正确容量(无 0→重排 的逐字出现)
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val viewportPx = with(density) { maxHeight.toPx() }   // maxHeight 是 Dp → 转 px,与 lineHeightPx 同单位
        // 预留底部「左右滑动翻阅」提示高度(~30dp),避免列占满挤掉提示
        val hintPx = with(density) { 30.dp.toPx() }
        // 用 remember(segments) 锁定「首次确定的列容量」:返回动画/重组中 maxHeight 波动时不再重算,
        // 避免 columns 重建导致分列结构变化(排版跳变但文本一致——用户反馈的"视觉闪烁")。
        val stableMaxChars = remember(segments) {
            if (viewportPx - hintPx <= 0f || lineHeightPx <= 0f) 0
            else ((viewportPx - hintPx) / lineHeightPx).toInt().coerceAtLeast(1)
        }
        val columns = remember(segments, title, author) {
            val body = if (stableMaxChars <= 0) emptyList()
                       else splitVerticalColumns(segments, stableMaxChars)
            // 最右 2 列 = 词牌名、作者(ownerStanza=-1 元数据列,渲染时不触发跨阕空隙);
            // asReversed() 后它们落在 Row 最右端,即阅读起始第一、二列。
            buildList {
                if (title.isNotBlank()) add(VerticalColumn(-1, title.toList()))
                // 作者列开头追加 2 个空格字符,竖排时表现为作者名上方多 2 个空位,版式更和谐
                if (author.isNotBlank()) add(VerticalColumn(-1, ("  " + author).toList()))
                addAll(body)
            }
        }
        // 右起阅读:初始滚到最右,让首句(第一列)从右端可见,而非停留在最左(最后一句)。
        // 只在首次进入(或换词 segments 变化)时执行一次;返回动画中 columns 随 maxChars 重建时不重复滚动,
        // 否则会在 pop 过渡时把已滚位置重置到最右 → 闪烁/重排版再退出。
        var scrolledToEnd by remember(segments) { mutableStateOf(false) }
        LaunchedEffect(segments) {
            while (scrollState.maxValue == 0) withFrameNanos {}
            if (!scrolledToEnd) { scrollState.scrollTo(scrollState.maxValue); scrolledToEnd = true }
        }
        Column {
            // 竖排区:外层 Box 占 Column 剩余高(裁剪超高列,不挤掉提示);内部横线 + 列区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // 顶部横线:贴 Box 顶(fillMaxWidth 撑满、line 色);与词内容的间隔由 Row 的 padding(top) 承担,同横排(宽 36 / 窄 28)
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
                        .padding(top = if (wide) 36.dp else 28.dp, bottom = 8.dp),   // 顶间隔同横排;底部 8dp 收尾
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),  // 列间 8dp + 不足宽时靠右对齐
                ) {
                    // 列块右起排(reversed 让首列在右端);跨阕交界插更大空隙
                    var lastStanza = -1
                    val titleIdx = 0
                    val authorIdx = if (title.isNotBlank()) 1 else 0
                    columns.asReversed().forEachIndexed { revIdx, col ->
                        if (lastStanza >= 0 && col.ownerStanza != lastStanza) Spacer(modifier = Modifier.width(24.dp))
                        lastStanza = col.ownerStanza
                    val origIndex = columns.size - 1 - revIdx   // 原始 columns 中的位置(词牌=0,作者=1)
                    val isTitle = title.isNotBlank() && origIndex == titleIdx
                    val isAuthor = author.isNotBlank() && origIndex == authorIdx
                    val titleColHeightPx = remember { mutableStateOf(0) }
                    if (isTitle) {
                        // 词牌列(最右):Column 右侧附一条竖线(primary 色),从顶部横线到词牌列底,包住词牌名
                        Row(verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                   modifier = Modifier.onSizeChanged { titleColHeightPx.value = it.height }) {
                                col.chars.forEach { ch ->
                                    val style = if (wide) MaterialTheme.typography.headlineLarge
                                                else MaterialTheme.typography.headlineMedium
                                    Text(ch.toString(), style = style, color = SongciColors.primary)
                                }
                            }
                            // 竖线:3dp 加粗;距词牌 16dp;高 = 词牌列高 + 顶部到横线的间隔(wide 36 / 窄 28 dp)
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .width(3.dp)
                                    .height(with(density) {
                                        titleColHeightPx.value.toDp() +
                                            (if (wide) 36.dp else 28.dp)   // 顶部横线到词牌列顶的间隔(同横排)
                                    })
                                    .background(SongciColors.primary),
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            col.chars.forEach { ch ->
                                if (isAuthor) {
                                    // 作者列:沿用原顶部作者样式(titleMedium, stone)
                                    Text(
                                        ch.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SongciColors.stone,
                                    )
                                } else {
                                    Text(
                                        ch.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = compactFontSize,
                                            lineHeight = compactFontSize * 1.25f,   // 竖排单字紧凑行距(与容量计算一致)
                                        ),
                                        color = SongciColors.nearBlack,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }   // 闭外层 Box(weight 竖排区)
            // 提示固定在 Column 底部(Row 已用 weight 占剩余,提示紧随其后,不弹性伸缩)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
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
