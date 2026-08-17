package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songci.app.data.Author
import com.songci.app.data.Poem
import com.songci.app.data.RhythmicIndex
import com.songci.app.data.RhythmicSpec
import com.songci.app.ui.components.AlphabetIndexBar
import com.songci.app.ui.components.PoemCard
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import kotlinx.coroutines.launch
import com.songci.app.ui.components.EmptyState
import com.songci.app.ui.components.PoemList
import com.songci.app.ui.components.SimpleListScreen

/** 文本行列表通用实现(目录索引/朝代/词牌共用;词牌/作者传 heads 启用拼音分组+索引条)。 */
@Composable
private fun TextRowList(
    title: String,
    back: (() -> Unit)?,
    rows: List<Pair<String, String>>,   // (label, value)
    onClick: (String) -> Unit,
    trailing: ((String) -> String?)? = null,   // 行尾小字标签(如词牌字数),默认无
    heads: Map<String, String> = emptyMap(),   // label → 拼音首字母分组;非空则启用分组+索引条
) {
    SimpleListScreen(title = title, back = back) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        // 按拼音首字母分组(head 排序:0 < A < … < #);heads 为空 = 不分组(目录/朝代)
        val grouped: List<Pair<String, List<Pair<String, String>>>> = remember(heads, rows) {
            if (heads.isEmpty()) emptyList()
            else rows.groupBy { heads[it.first] ?: "#" }.toSortedMap().map { it.key to it.value }
        }
        // 分组起始 item index(含 header item),供索引条跳转
        val groupStart: Map<String, Int> = remember(grouped) {
            val map = mutableMapOf<String, Int>()
            var idx = 0
            grouped.forEach { (head, items) ->
                map[head] = idx
                idx += 1 + items.size
            }
            map
        }
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(20.dp),
            ) {
                if (grouped.isEmpty()) {
                    items(rows) { (label, value) -> TextIndexRow(label, value, onClick, trailing) }
                } else {
                    grouped.forEach { (head, items) ->
                        item(key = "header-$head") { HeadHeader(head) }
                        items(items, key = { it.first }) { (label, value) ->
                            TextIndexRow(label, value, onClick, trailing)
                        }
                    }
                }
            }
            if (grouped.isNotEmpty()) {
                AlphabetIndexBar(
                    present = groupStart.keys,
                    onSelect = { head ->
                        groupStart[head]?.let { scope.launch { listState.scrollToItem(it) } }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

/** 拼音分组 header(字母小标题,与列表卡片同风格)。 */
@Composable
private fun HeadHeader(head: String) {
    Text(
        head,
        style = MaterialTheme.typography.labelMedium,
        color = SongciColors.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 0.dp),
    )
}

/** 单行:label(词牌名等,超长单行省略)+ 行尾小字标签。 */
@Composable
private fun TextIndexRow(
    label: String,
    value: String,
    onClick: (String) -> Unit,
    trailing: ((String) -> String?)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SongciColors.surfaceContainerLow)
            .border(1.dp, SongciColors.line)
            .clickable { onClick(value) }
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // label 最长 15 字(词牌):weight(fill=false) 限定宽约束,超宽单行省略;
        // 否则按内容宽测量,行尾 tag(如"102字")被挤到 0 宽竖排
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = SongciColors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        val tag = trailing?.invoke(label)
        if (tag != null) {
            Text(tag, style = MaterialTheme.typography.labelMedium,
                 color = SongciColors.outline, maxLines = 1)
        }
    }
}

/** 目录索引入口:朝代 / 作者 / 词牌(含格律,详情页内置平仄谱卡片)。tab 根页,无返回。 */
@Composable
fun IndexScreen(onOpen: (String) -> Unit) {
    TextRowList(
        title = "目录索引",
        back = null,
        rows = listOf(
            "朝代 →" to "index/dynasty",
            "作者 →" to "index/authors",
            "词牌 →" to "index/rhythmics",   // 含格律(平仄谱见词牌详情)
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // 按拼音首字母分组(head 排序:0 < A < … < #)
    val grouped: List<Pair<String, List<Author>>> = remember(authors) {
        authors.groupBy { it.head }.toSortedMap().map { it.key to it.value }
    }
    val groupStart: Map<String, Int> = remember(grouped) {
        val map = mutableMapOf<String, Int>()
        var idx = 0
        grouped.forEach { (head, items) ->
            map[head] = idx
            idx += 1 + items.size
        }
        map
    }
    Box {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        ) {
            grouped.forEach { (head, items) ->
                item(key = "header-$head") { HeadHeader(head) }
                items(items, key = { it.id }) { author ->
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
                            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp),
                        )
                    }
                }
            }
        }
        AlphabetIndexBar(
            present = groupStart.keys,
            onSelect = { head ->
                groupStart[head]?.let { scope.launch { listState.scrollToItem(it) } }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** 词牌列表(词牌与格律共用,行尾显示字数)。 */
@Composable
fun RhythmicsScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val rhythmics by vm.rhythmics.collectAsState()
    TextRowList(
        title = "词牌", back = onBack,
        rows = rhythmics.map { it.rhythmic to it.rhythmic },
        onClick = onOpen,
        trailing = { r -> vm.rhythmicSpec(r)?.let { "${it.chars}字" } },
        heads = rhythmics.associate { it.rhythmic to it.head },   // 拼音首字母分组 + 索引条
    )
}

/** 某作者的全部词作。 */
@Composable
fun AuthorPoemsScreen(
    vm: AppViewModel, authorId: Long, wide: Boolean, initialPoemId: Long?,
    onBack: () -> Unit, onPoem: (Long) -> Unit,
    onOpenAuthor: (Long, Long) -> Unit, onOpenRhythmic: (String, Long) -> Unit,
) {
    var author by remember { mutableStateOf<Author?>(null) }
    var poems by remember { mutableStateOf<List<Poem>?>(null) }
    LaunchedEffect(authorId) {
        author = vm.author(authorId)
        poems = vm.poemsByAuthor(authorId)
    }
    val list = poems
    if (wide) {
        MasterDetailPane(
            title = author?.name ?: "作者词作",
            onBack = onBack, vm = vm, initialPoemId = initialPoemId, list = list,
            onOpenAuthor = onOpenAuthor, onOpenRhythmic = onOpenRhythmic,
        ) {
            author?.longDesc?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SongciColors.tertiary,
                    modifier = Modifier.fillMaxWidth()
                        .background(SongciColors.surfaceContainerLow)
                        .border(1.dp, SongciColors.line)
                        .padding(16.dp),
                )
            }
        }
        return
    }
    SimpleListScreen(title = author?.name ?: "作者词作", back = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            author?.longDesc?.takeIf { it.isNotBlank() }?.let { desc ->
                item {
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SongciColors.tertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SongciColors.surfaceContainerLow)
                            .border(1.dp, SongciColors.line)
                            .padding(16.dp),
                    )
                }
            }
            when {
                list == null -> item { EmptyState("加载中…") }
                list.isEmpty() -> item { EmptyState("该作者暂无收录词作") }   // 名录含金/元等词人,语料未收录
                else -> items(list, key = { it.id }) { PoemCard(it) { onPoem(it.id) } }
            }
        }
    }
}

/** 某词牌下的全部词作(格律卡片为首 item,整页统一滚动;未映射词牌无卡片)。 */
@Composable
fun RhythmicPoemsScreen(
    vm: AppViewModel, rhythmic: String, wide: Boolean, initialPoemId: Long?,
    onBack: () -> Unit, onPoem: (Long) -> Unit,
    onOpenAuthor: (Long, Long) -> Unit, onOpenRhythmic: (String, Long) -> Unit,
) {
    var poems by remember { mutableStateOf<List<Poem>?>(null) }
    LaunchedEffect(rhythmic) { poems = vm.poemsByRhythmic(rhythmic) }
    val list = poems
    if (wide) {
        MasterDetailPane(
            title = "词牌 · $rhythmic",
            onBack = onBack, vm = vm, initialPoemId = initialPoemId, list = list,
            onOpenAuthor = onOpenAuthor, onOpenRhythmic = onOpenRhythmic,
        ) {
            vm.rhythmicSpec(rhythmic)?.let { spec ->
                RhythmicCard(spec, vm.bodiesOf(rhythmic).ifEmpty { listOf(spec) })
            }
        }
        return
    }
    SimpleListScreen(title = "词牌 · $rhythmic", back = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            vm.rhythmicSpec(rhythmic)?.let { spec ->
                item(key = "rhythmic") { RhythmicCard(spec, vm.bodiesOf(rhythmic).ifEmpty { listOf(spec) }) }
            }
            when {
                list == null -> item { EmptyState("加载中…") }
                list.isEmpty() -> item { EmptyState("该词牌暂无词作") }
                else -> items(list, key = { it.id }) { PoemCard(it) { onPoem(it.id) } }
            }
        }
    }
}

/** 格律卡片:句式摘要 + 逐字平仄谱按句分行,阕(段)间空行(韵脚下划线);多体可切换。 */
@Composable
private fun RhythmicCard(spec: RhythmicSpec, bodies: List<RhythmicSpec>) {
    val tuneColor = mapOf('平' to SongciColors.primary, '仄' to SongciColors.error, '中' to SongciColors.outline)
    var selected by remember { mutableStateOf(0) }
    val current = bodies.getOrNull(selected) ?: spec
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SongciColors.surfaceContainerLow)
            .border(1.dp, SongciColors.line)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(current.sketch, style = MaterialTheme.typography.bodyMedium, color = SongciColors.tertiary)
        Text("${current.chars}字 · ${bodies.size}体",
             style = MaterialTheme.typography.labelMedium, color = SongciColors.outline)
        if (bodies.size > 1) {
            // FlowRow 换行:变体名可能很长(如一剪梅多体),单行 Row 会挤压溢出
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bodies.indices.forEach { i ->
                    val label = bodies[i].author.ifBlank { null }?.let { "${it}体" } ?: "体${i + 1}"
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (i == selected) SongciColors.primary else SongciColors.outline,
                        textDecoration = if (i == selected) TextDecoration.Underline else TextDecoration.None,
                        modifier = Modifier.clickable { selected = i },
                    )
                }
            }
        }
        if (spec.aliases.isNotEmpty()) {
            Text("异名：" + spec.aliases.joinToString(" · "),
                 style = MaterialTheme.typography.labelMedium, color = SongciColors.stone)
        }
        current.tuneLines().forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                line.chars.forEach { (t, m) ->
                    Text(t.toString(), style = MaterialTheme.typography.bodyMedium,
                         color = tuneColor[t] ?: SongciColors.tertiary,
                         textDecoration = if (m == 'Y') TextDecoration.Underline else TextDecoration.None)
                }
            }
            if (line.segmentEnd) Spacer(Modifier.height(12.dp))   // 阕间空行
        }
    }
}

/** 宽屏 master-detail 双栏:左列表(常驻浏览上下文)+ 右侧所选词详情。选词为内部状态,不导航。 */
@Composable
private fun MasterDetailPane(
    title: String,
    onBack: () -> Unit,
    vm: AppViewModel,
    initialPoemId: Long?,
    list: List<Poem>?,
    onOpenAuthor: (Long, Long) -> Unit,       // (authorId, 当前选中词 id,跳转携带)
    onOpenRhythmic: (String, Long) -> Unit,   // (rhythmic, 当前选中词 id)
    header: @Composable () -> Unit,
) {
    var selectedId by remember { mutableStateOf(initialPoemId) }
    Row(Modifier.fillMaxSize()) {
        // 左栏:列表(master)
        Column(Modifier.width(300.dp).fillMaxSize().background(SongciColors.surfaceContainerLow)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SongciColors.primary,
                    modifier = Modifier.padding(end = 8.dp).clickable(onClick = onBack),
                )
                Text(title, style = MaterialTheme.typography.labelMedium, color = SongciColors.stone)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
                item { header() }
                when {
                    list == null -> item { EmptyState("加载中…") }
                    list.isEmpty() -> item { EmptyState("暂无词作") }
                    else -> items(list, key = { it.id }) { PoemCard(it) { selectedId = it.id } }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxSize().background(SongciColors.line))
        // 右栏:所选词详情(detail);跳转回调携带当前选中词,保证新双栏页初始词衔接
        Box(Modifier.weight(1f)) {
            val id = selectedId
            if (id == null) EmptyState("点击左侧词作")
            else SelectedDetail(vm, id,
                onOpenAuthor = { authorId -> onOpenAuthor(authorId, id) },
                onOpenRhythmic = { r -> onOpenRhythmic(r, id) },
            )
        }
    }
}

/** 双栏右侧详情:加载所选词 + DetailBody(窄版密度,防窄区挤压)。 */
@Composable
private fun SelectedDetail(
    vm: AppViewModel,
    poemId: Long,
    onOpenAuthor: (Long) -> Unit,
    onOpenRhythmic: (String) -> Unit,
) {
    var poem by remember { mutableStateOf<Poem?>(null) }
    var favorite by remember { mutableStateOf(false) }
    LaunchedEffect(poemId) {
        poem = vm.poem(poemId)
        favorite = vm.isFavorite(poemId)
    }
    val current = poem
    if (current == null) {
        EmptyState("加载中…")
        return
    }
    DetailBody(
        vm = vm, poem = current, scale = vm.fontScale.scale, wide = false,
        favorite = favorite,
        onToggleFavorite = { vm.setFavorite(current, !favorite); favorite = !favorite },
        onOpenAuthor = onOpenAuthor, onOpenRhythmic = onOpenRhythmic,
    )
}
