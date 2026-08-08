package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songci.app.data.Author
import com.songci.app.data.Poem
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.EmptyState
import com.songci.app.ui.components.PoemList
import com.songci.app.ui.components.SimpleListScreen

/** 文本行列表通用实现(目录索引/朝代/词牌共用)。 */
@Composable
private fun TextRowList(
    title: String,
    back: (() -> Unit)?,
    rows: List<Pair<String, String>>,   // (label, value)
    onClick: (String) -> Unit,
) {
    SimpleListScreen(title = title, back = back) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        ) {
            items(rows) { (label, value) ->
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SongciColors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SongciColors.surfaceContainerLow)
                        .border(1.dp, SongciColors.line)
                        .clickable { onClick(value) }
                        .padding(18.dp),
                )
            }
        }
    }
}

/** 目录索引入口:朝代 / 作者 / 词牌 / 格律(格律无独立数据,按词牌聚合)。 */
@Composable
fun IndexScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    TextRowList(
        title = "目录索引",
        back = onBack,
        rows = listOf(
            "朝代 →" to "index/dynasty",
            "作者 →" to "index/authors",
            "词牌 →" to "index/rhythmics",
            "格律 →" to "index/rhythmics",   // 数据缺口:格律按词牌聚合
        ),
        onClick = onOpen,
    )
}

/** 朝代列表(仅含数据中存在的朝代)。 */
@Composable
fun DynastyListScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val dynasties by vm.knownDynasties.collectAsState()
    TextRowList(title = "朝代", back = onBack, rows = dynasties.map { it to it }, onClick = onOpen)
}

/** 某朝代下的作者列表。 */
@Composable
fun DynastyAuthorsScreen(vm: AppViewModel, dynasty: String, onBack: () -> Unit, onAuthor: (Long) -> Unit) {
    var authors by remember { mutableStateOf<List<Author>?>(null) }
    LaunchedEffect(dynasty) { authors = vm.authorsByDynasty(dynasty) }
    val list = authors
    SimpleListScreen(title = "朝代 · $dynasty", back = onBack) {
        if (list == null) {
            EmptyState("加载中…")
        } else if (list.isEmpty()) {
            EmptyState("该朝代暂无收录作者")
        } else {
            AuthorList(
                list,
                dynastyOf = { vm.dynasty.of(it) },
                evidenceOf = { vm.dynasty.evidenceOf(it) },
                onAuthor = onAuthor,
            )
        }
    }
}

/** 全部作者列表(带朝代标签与年份证据)。 */
@Composable
fun AuthorsScreen(vm: AppViewModel, onBack: () -> Unit, onAuthor: (Long) -> Unit) {
    val authors by vm.authors.collectAsState()
    SimpleListScreen(title = "作者", back = onBack) {
        AuthorList(
            authors,
            dynastyOf = { vm.dynasty.of(it) },
            evidenceOf = { vm.dynasty.evidenceOf(it) },
            onAuthor = onAuthor,
        )
    }
}

@Composable
private fun AuthorList(
    authors: List<Author>,
    dynastyOf: (Long) -> String,
    evidenceOf: (Long) -> String?,
    onAuthor: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
    ) {
        items(authors, key = { it.id }) { author ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SongciColors.surfaceContainerLow)
                    .border(1.dp, SongciColors.line)
                    .clickable { onAuthor(author.id) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(author.name, style = MaterialTheme.typography.bodyLarge, color = SongciColors.nearBlack)
                Text(
                    dynastyOf(author.id) + (evidenceOf(author.id)?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = SongciColors.stone,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/** 词牌列表(词牌与格律共用)。 */
@Composable
fun RhythmicsScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val rhythmics by vm.rhythmics.collectAsState()
    TextRowList(title = "词牌", back = onBack, rows = rhythmics.map { it to it }, onClick = onOpen)
}

/** 某作者的全部词作。 */
@Composable
fun AuthorPoemsScreen(vm: AppViewModel, authorId: Long, onBack: () -> Unit, onPoem: (Long) -> Unit) {
    var poems by remember { mutableStateOf<List<Poem>?>(null) }
    LaunchedEffect(authorId) { poems = vm.poemsByAuthor(authorId) }
    val list = poems
    SimpleListScreen(title = "作者词作", back = onBack) {
        when {
            list == null -> EmptyState("加载中…")
            list.isEmpty() -> EmptyState("该作者暂无收录词作")   // 名录含金/元等词人,语料未收录
            else -> PoemList(list) { onPoem(it.id) }
        }
    }
}

/** 某词牌下的全部词作。 */
@Composable
fun RhythmicPoemsScreen(vm: AppViewModel, rhythmic: String, onBack: () -> Unit, onPoem: (Long) -> Unit) {
    var poems by remember { mutableStateOf<List<Poem>?>(null) }
    LaunchedEffect(rhythmic) { poems = vm.poemsByRhythmic(rhythmic) }
    val list = poems
    SimpleListScreen(title = "词牌 · $rhythmic", back = onBack) {
        when {
            list == null -> EmptyState("加载中…")
            list.isEmpty() -> EmptyState("该词牌暂无词作")
            else -> PoemList(list) { onPoem(it.id) }
        }
    }
}
