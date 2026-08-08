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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songci.app.data.Poem
import com.songci.app.theme.SongciColors

/** 词作卡片(首页推荐流 / 搜索结果):词牌 + 作者 + 首句摘录 + 阅读全文。 */
@Composable
fun PoemCard(poem: Poem, onClick: () -> Unit) {
    val excerpt = poem.content.lineSequence().firstOrNull()?.take(18) ?: ""
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
            Text(poem.rhythmic, style = MaterialTheme.typography.headlineMedium)
            Text(poem.authorName, style = MaterialTheme.typography.titleMedium, color = SongciColors.stone)
        }
        Text(
            "「$excerpt」",
            style = MaterialTheme.typography.bodyMedium,
            color = SongciColors.onBackground,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            "阅读全文 →",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.primary,
            modifier = Modifier.padding(top = 12.dp),
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
fun PoemList(poems: List<Poem>, onClick: (Poem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
    ) {
        items(poems, key = { it.id }) { poem ->
            PoemCard(poem, onClick = { onClick(poem) })
        }
    }
}
