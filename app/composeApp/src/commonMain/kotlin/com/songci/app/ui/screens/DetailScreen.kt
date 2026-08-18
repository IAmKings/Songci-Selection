package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import com.songci.app.ui.BookmarkBorderIcon
import com.songci.app.ui.BookmarkIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.songci.app.data.Poem
import com.songci.app.data.Segmenter
import com.songci.app.data.VerticalColumn
import com.songci.app.data.splitVerticalColumns
import com.songci.app.theme.Kicker
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.EmptyState

/** 词作详情:窄屏单栏,宽屏(≥768dp)双栏并置。 */
@Composable
fun DetailScreen(
    vm: AppViewModel,
    poemId: Long,
    wide: Boolean,
    onBack: () -> Unit,
    onOpenAuthor: (Long) -> Unit,
    onOpenRhythmic: (String) -> Unit,
) {
    var poem by remember { mutableStateOf<Poem?>(null) }
    var favorite by remember { mutableStateOf(false) }
    // 详情页临时横/竖排:初值=设置默认,页内切换只影响本次浏览,退出详情重置为设置默认(不写回持久化)
    var localVertical by remember { mutableStateOf(vm.verticalLayout) }
    LaunchedEffect(poemId) {
        poem = vm.poem(poemId)
        favorite = vm.isFavorite(poemId)
    }

    val current = poem
    if (current == null) {
        EmptyState("加载中…")
        return
    }
    val scale = vm.fontScale.scale
    Column(modifier = Modifier.fillMaxSize().background(SongciColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = SongciColors.primary,
                modifier = Modifier.padding(end = 8.dp).clickable(onClick = onBack),
            )
            Text("词作详情", style = MaterialTheme.typography.labelMedium, color = SongciColors.stone)
            Spacer(modifier = Modifier.weight(1f))
            // 详情页横/竖排临时切换(localVertical,退出详情恢复设置默认,不持久化)
            Text(
                if (localVertical) "↕横排" else "竖排",
                style = MaterialTheme.typography.labelSmall,
                color = SongciColors.primary,
                modifier = Modifier
                    .border(1.dp, SongciColors.primary)
                    .clickable { localVertical = !localVertical }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        DetailBody(
            vm = vm, poem = current, scale = scale, wide = wide,
            vertical = localVertical,
            favorite = favorite,
            onToggleFavorite = { vm.setFavorite(current, !favorite); favorite = !favorite },
            onOpenAuthor = onOpenAuthor,
            onOpenRhythmic = onOpenRhythmic,
        )
    }
}

/**
 * 词作内容体(与页面壳解耦):全屏详情与双栏右侧共用。
 * wide = 布局密度(全屏详情传屏宽判断;双栏右侧传 false 防窄区挤压)。
 */
@Composable
fun DetailBody(
    vm: AppViewModel,
    poem: Poem,
    scale: Float,
    wide: Boolean,
    vertical: Boolean,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenAuthor: (Long) -> Unit,
    onOpenRhythmic: (String) -> Unit,
) {
    // 竖排模式:竖排区必须占用「有界可见高度」视口(否则嵌在 verticalScroll 里 maxHeight=无限,
    // 导致 maxChars 巨大、分列失效、单列下滑)。故竖排不走外层 verticalScroll,改为固定布局:
    // 标题行 + 竖排区(weight 拿剩余有限高度) + 操作区;竖排区内部只横向滚动。
    if (vertical) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (wide) 64.dp else 30.dp, vertical = 24.dp)) {
            // 竖排:词牌名/作者作为竖排最右 2 列(移除顶部横排标题区,沉浸不重复)。
            // 竖排区:weight(1f) 占据剩余有限高度,内部 BoxWithConstraints 拿到真实视口高 → 正确分列
            VerticalPoemBody(
                poem.content, scale, vm.matchedSpec(poem.rhythmic, poem.content),
                poem.rhythmic, poem.authorName, wide,
                Modifier.weight(1f).fillMaxWidth(),
            )
            DetailActions(
                vm, favorite, onToggleFavorite, poem, onOpenAuthor, onOpenRhythmic,
                Modifier.padding(top = 16.dp),
            )
        }
        return
    }
    if (wide) {
        // 宽版:词句。必须可滚动——长词/大字号会把底部操作区(收藏/作者/词牌链接)挤出视口
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 64.dp, vertical = 40.dp)) {
            Kicker(width = 56.dp, height = 5.dp)
            Text(
                poem.rhythmic,
                style = MaterialTheme.typography.headlineLarge,
                color = SongciColors.primary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 30.dp),
            )
            Text(poem.authorName, style = MaterialTheme.typography.titleMedium, color = SongciColors.stone)
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp).height(1.dp).background(SongciColors.line))
            val spec = vm.matchedSpec(poem.rhythmic, poem.content)
            val segments = Segmenter.segment(poem.content, spec)
            if (segments.size == 2) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StanzaColumn(segments[0], scale, Modifier.weight(1f).padding(end = 56.dp))
                    Box(modifier = Modifier.width(1.dp).fillMaxSize().background(SongciColors.line))
                    StanzaColumn(segments[1], scale, Modifier.weight(1f).padding(start = 56.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    segments.forEach { StanzaColumn(it, scale) }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 36.dp)) {
                DetailActions(vm, favorite, onToggleFavorite, poem, onOpenAuthor, onOpenRhythmic)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = 24.dp),
        ) {
            Kicker(width = 40.dp, height = 4.dp)
            // 词牌名最长 15 字(含变体全称),超长截断防整词换行
            Text(
                poem.rhythmic,
                style = MaterialTheme.typography.headlineMedium,
                color = SongciColors.primary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(poem.authorName, style = MaterialTheme.typography.titleMedium, color = SongciColors.stone)
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp).height(1.dp).background(SongciColors.line))
            PoemLines(poem.content, scale, gap = 34.dp, spec = vm.matchedSpec(poem.rhythmic, poem.content))
            // 窄屏:词内容与操作栏间隔(与宽屏 36dp 语义一致,窄屏密度略紧凑)
            DetailActions(
                vm, favorite, onToggleFavorite, poem, onOpenAuthor, onOpenRhythmic,
                Modifier.padding(top = 24.dp),
            )
        }
    }
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

@Composable
private fun StanzaColumn(lines: List<String>, scale: Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    var maxWidthPx by remember { mutableStateOf(0) }
    Column(
        modifier = modifier.onSizeChanged { maxWidthPx = it.width },
    ) {
        lines.forEach { line ->
            val style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale,
            )
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

@Composable
private fun PoemLines(content: String, scale: Float, gap: androidx.compose.ui.unit.Dp,
                      spec: com.songci.app.data.RhythmicSpec?) {
    val segments = Segmenter.segment(content, spec)
    val textMeasurer = rememberTextMeasurer()
    var maxWidthPx by remember { mutableStateOf(0) }
    Column(modifier = Modifier.onSizeChanged { maxWidthPx = it.width }) {
        segments.forEachIndexed { i, seg ->
            if (i > 0) Box(modifier = Modifier.height(gap))
            seg.forEach { line ->
                val style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale,
                )
                val rows = if (maxWidthPx > 0) punctuatedLines(line, style, textMeasurer, maxWidthPx) else listOf(line)
                rows.forEach { seg2 ->
                    Text(
                        seg2,
                        style = style,
                        color = SongciColors.nearBlack,
                    )
                }
            }
        }
    }
}

/**
 * 竖排诗词正文(方案 A:字符矩阵 + Column)。
 * 每句拆成单字符 → 逐字纵向 Column(一列 = 一句);列高=视口高度,超长句按标点截断换列(避免纵向滚动)。
 * 上/下阕交界处空开一列(跨阕大间距)。多句多列从右向左排(首句最右);初始滚到最右;底部提示横滑。
 * 滚动:仅外层横向(多列),列高受视口约束,无需纵向滚动阅列。
 */
@Composable
private fun VerticalPoemBody(
    content: String,
    scale: Float,
    spec: com.songci.app.data.RhythmicSpec?,
    title: String,
    author: String,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val segments = Segmenter.segment(content, spec)          // List<List<String>>:上/下阕
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

@Composable
private fun DetailActions(
    vm: AppViewModel,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    poem: Poem,
    onOpenAuthor: (Long) -> Unit,
    onOpenRhythmic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 收藏居左;文字链接成组靠右(Spacer 撑开),作者/词牌彼此贴近
            Row(
                modifier = Modifier
                    .border(1.dp, SongciColors.primary)
                    .clickable(onClick = onToggleFavorite)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (favorite) BookmarkIcon else BookmarkBorderIcon,   // 书签语义
                    contentDescription = null,
                    tint = SongciColors.primary,
                    modifier = Modifier.height(18.dp),
                )
                Text(
                    if (favorite) "已收藏" else "收藏",
                    style = MaterialTheme.typography.labelLarge,
                    color = SongciColors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "作者词作 →",
                style = MaterialTheme.typography.labelLarge,
                color = SongciColors.primary,
                modifier = Modifier.padding(start = 12.dp).clickable { poem.authorId?.let(onOpenAuthor) },
            )
            Text(
                "词牌词作 →",
                style = MaterialTheme.typography.labelLarge,
                color = SongciColors.primary,
                modifier = Modifier.padding(start = 8.dp)
                    .clickable { onOpenRhythmic(vm.cleanRhythmic(poem.rhythmic)) },
            )
        }
        Text(
            "db/songci.db · ${poem.id}",
            style = MaterialTheme.typography.labelSmall,
            color = SongciColors.stone,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
