package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.songci.app.data.Poem
import com.songci.app.data.Segmenter
import com.songci.app.theme.Kicker
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.EmptyState
import com.songci.app.ui.components.KhetiPoemStanza
import com.songci.app.ui.components.KhetiVerticalPoemBody
import com.songci.app.ui.components.rememberKhetiPoemStyle
import com.songci.app.ui.components.useKhetiRendering

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
 * 正文渲染:kheti 引擎(悬挂/中西文间距/标点挤压;竖排 KhetiVerticalEngine),
 * 回退开关 useKhetiRendering()=false 时走 DetailLegacy.kt 旧路径(仅 iOS 异常时启用)。
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
    val spec = vm.matchedSpec(poem.rhythmic, poem.content)
    val segments = Segmenter.segment(poem.content, spec)   // List<List<String>>:上/下阕(格律段边界)
    // 竖排模式:竖排区必须占用「有界可见高度」视口(否则嵌在 verticalScroll 里 maxHeight=无限,
    // 导致分列失效、单列下滑)。故竖排不走外层 verticalScroll,改为固定布局:
    // 标题行 + 竖排区(weight 拿剩余有限高度) + 操作区;竖排区内部只横向滚动。
    if (vertical) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (wide) 64.dp else 30.dp, vertical = 24.dp)) {
            // 竖排:词牌名/作者作为竖排最右 2 列(移除顶部横排标题区,沉浸不重复)。
            // 竖排区:weight(1f) 占据剩余有限高度,内部 BoxWithConstraints 拿到真实视口高 → 正确分列
            if (useKhetiRendering()) {
                KhetiVerticalPoemBody(
                    segments, poem.rhythmic, poem.authorName, vm.fontStyle, scale, wide,
                    Modifier.weight(1f).fillMaxWidth(),
                    showGrid = vm.khetiGrid,
                )
            } else {
                VerticalPoemBodyLegacy(
                    poem.content, scale, spec, poem.rhythmic, poem.authorName, wide,
                    Modifier.weight(1f).fillMaxWidth(),
                )
            }
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
            if (segments.size == 2) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PoemStanzaBody(vm, segments[0], scale, wide,
                        Modifier.weight(1f).padding(end = 56.dp))
                    Box(modifier = Modifier.width(1.dp).fillMaxSize().background(SongciColors.line))
                    PoemStanzaBody(vm, segments[1], scale, wide,
                        Modifier.weight(1f).padding(start = 56.dp))
                }
            } else {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp)) {
                    segments.forEach { PoemStanzaBody(vm, it, scale, wide, Modifier.fillMaxWidth()) }
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
            // 窄屏:每阕一个 kheti 块,阕间 34dp(与旧 PoemLines 节奏一致)
            segments.forEachIndexed { i, seg ->
                if (i > 0) Box(modifier = Modifier.height(34.dp))
                PoemStanzaBody(vm, seg, scale, wide = false, Modifier.fillMaxWidth())
            }
            // 窄屏:词内容与操作栏间隔(与宽屏 36dp 语义一致,窄屏密度略紧凑)
            DetailActions(
                vm, favorite, onToggleFavorite, poem, onOpenAuthor, onOpenRhythmic,
                Modifier.padding(top = 24.dp),
            )
        }
    }
}

/** 单阕正文:kheti 引擎渲染(默认)或 iOS 回退旧路径。 */
@Composable
private fun PoemStanzaBody(
    vm: AppViewModel,
    stanza: List<String>,
    scale: Float,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    if (useKhetiRendering()) {
        KhetiPoemStanza(
            stanza,
            rememberKhetiPoemStyle(wide, scale, vm.fontStyle),
            SongciColors.nearBlack,
            modifier,
        )
    } else {
        StanzaColumnLegacy(
            stanza,
            scale,
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale,
            ),
            modifier,
        )
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
