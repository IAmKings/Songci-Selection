package com.songci.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songci.app.data.Poem
import com.songci.app.data.SearchMatch
import com.songci.app.theme.SongciColors

/** 词作卡片(首页推荐流 / 搜索结果):词牌 + 作者 + 首句摘录 + 阅读全文。 */
@Composable
fun PoemCard(
    poem: Poem,
    rhythmicHighlight: String? = null,   // 命中子串(背景高亮);null = 不高亮
    authorHighlight: String? = null,
    contentHighlight: String? = null,
    onClick: () -> Unit,
) {
    val lines = poem.content.lineSequence()
    val rawExcerpt = lines.firstOrNull { contentHighlight == null || it.contains(contentHighlight) }
        ?: lines.firstOrNull { it.isNotBlank() }   // 匹配句优先;无匹配行(数据异常)回退首句
        ?: ""
    // 摘录:命中内容时以匹配词为中心截 18 字窗口(保证匹配可见),否则首句硬截补省略号
    val excerpt = when {
        contentHighlight != null && rawExcerpt.contains(contentHighlight) && rawExcerpt.length > 18 -> {
            val q = contentHighlight
            val idx = rawExcerpt.indexOf(q)
            val start = (idx - (18 - q.length) / 2).coerceAtLeast(0)
            val end = (start + 18).coerceAtMost(rawExcerpt.length)
            buildString {
                if (start > 0) append("…")
                append(rawExcerpt, start, end)
                if (end < rawExcerpt.length) append("…")
            }
        }
        rawExcerpt.length > 18 -> rawExcerpt.take(18).trimEnd() + "…"
        else -> rawExcerpt
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SongciColors.surfaceContainerLow)
            .border(1.dp, SongciColors.line)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 词牌最长 15 字:weight(fill=false) 限定宽约束,超宽真正触发单行省略;
            // 否则 Row 按内容宽测量,作者名被挤到 0 宽导致竖排(如"愁倚阑令·春光好 + 晏几道")
            Text(
                highlight(poem.rhythmic, rhythmicHighlight),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                highlight(poem.authorName, authorHighlight),
                style = MaterialTheme.typography.titleMedium,
                color = SongciColors.stone,
                maxLines = 1,
            )
        }
        Text(
            highlight("「$excerpt」", contentHighlight),
            style = MaterialTheme.typography.bodyMedium,
            color = SongciColors.onBackground,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            "阅读全文 →",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.primary,
            textAlign = TextAlign.End,   // 右对齐:与右上角作者名呼应,视觉对称
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

/** 列表通用脚手架:标题 + LazyColumn 内容。 */
@Composable
fun SimpleListScreen(
    title: String,
    back: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(SongciColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (back != null) {
                Text(
                    "‹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SongciColors.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable(onClick = back),
                )
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        content()
    }
}

@Composable
fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = SongciColors.stone)
    }
}

@Composable
fun PoemList(
    poems: List<Poem>,
    highlightFor: ((Poem) -> SearchMatch?)? = null,   // 搜索高亮;null = 无高亮
    onClick: (Poem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
    ) {
        items(poems, key = { it.id }) { poem ->
            val m = highlightFor?.invoke(poem)
            PoemCard(
                poem,
                rhythmicHighlight = m?.rhythmicHighlight,
                authorHighlight = m?.authorHighlight,
                contentHighlight = m?.contentHighlight,
                onClick = { onClick(poem) },
            )
        }
    }
}

/**
 * 命中子串背景高亮(primary 淡底,支持多次出现)。
 * query 为 null/空或未命中时返回原文本。
 */
private fun highlight(text: String, query: String?): AnnotatedString {
    if (query.isNullOrEmpty() || query !in text) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val idx = text.indexOf(query, start)
            if (idx < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, idx))
            withStyle(SpanStyle(background = SongciColors.primary.copy(alpha = 0.20f))) { append(query) }
            start = idx + query.length
        }
    }
}
