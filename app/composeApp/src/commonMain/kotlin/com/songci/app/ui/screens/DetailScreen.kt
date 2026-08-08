package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
        }
        if (wide) {
            WideDetail(
                vm = vm,
                poem = current,
                scale = scale,
                favorite = favorite,
                onToggleFavorite = { vm.setFavorite(current, !favorite); favorite = !favorite },
                onOpenAuthor = onOpenAuthor,
                onOpenRhythmic = onOpenRhythmic,
            )
        } else {
            NarrowDetail(
                vm = vm,
                poem = current,
                scale = scale,
                favorite = favorite,
                onToggleFavorite = { vm.setFavorite(current, !favorite); favorite = !favorite },
                onOpenAuthor = onOpenAuthor,
                onOpenRhythmic = onOpenRhythmic,
            )
        }
    }
}

@Composable
private fun NarrowDetail(
    vm: AppViewModel,
    poem: Poem,
    scale: Float,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenAuthor: (Long) -> Unit,
    onOpenRhythmic: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp, vertical = 24.dp),
    ) {
        Kicker(width = 40.dp, height = 4.dp)
        Text(
            poem.rhythmic,
            style = MaterialTheme.typography.headlineMedium,
            color = SongciColors.primary,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(poem.authorName, style = MaterialTheme.typography.titleMedium, color = SongciColors.stone)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp).height(1.dp).background(SongciColors.line))
        PoemLines(poem.content, scale, gap = 34.dp, spec = vm.matchedSpec(poem.rhythmic, poem.content))
        DetailActions(vm, favorite, onToggleFavorite, poem, onOpenAuthor, onOpenRhythmic)
    }
}

@Composable
private fun WideDetail(
    vm: AppViewModel,
    poem: Poem,
    scale: Float,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenAuthor: (Long) -> Unit,
    onOpenRhythmic: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp)) {
        Kicker(width = 56.dp, height = 5.dp)
        Text(
            poem.rhythmic,
            style = MaterialTheme.typography.headlineLarge,
            color = SongciColors.primary,
            modifier = Modifier.padding(top = 30.dp),
        )
        Text(poem.authorName, style = MaterialTheme.typography.titleMedium, color = SongciColors.stone)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp).height(1.dp).background(SongciColors.line))
        val segments = Segmenter.segment(poem.content, vm.matchedSpec(poem.rhythmic, poem.content))
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
}

@Composable
private fun StanzaColumn(lines: List<String>, scale: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        lines.forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale),
                color = SongciColors.nearBlack,
            )
        }
    }
}

@Composable
private fun PoemLines(content: String, scale: Float, gap: androidx.compose.ui.unit.Dp,
                      spec: com.songci.app.data.RhythmicSpec?) {
    val segments = Segmenter.segment(content, spec)
    Column {
        segments.forEachIndexed { i, seg ->
            if (i > 0) Box(modifier = Modifier.height(gap))
            seg.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale),
                    color = SongciColors.nearBlack,
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
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, SongciColors.primary)
                    .clickable(onClick = onToggleFavorite)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
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
            Text(
                "作者 · ${poem.authorName} →",
                style = MaterialTheme.typography.labelLarge,
                color = SongciColors.primary,
                modifier = Modifier.padding(start = 24.dp).clickable { poem.authorId?.let(onOpenAuthor) },
            )
            Text(
                "词牌 · ${vm.cleanRhythmic(poem.rhythmic)} →",
                style = MaterialTheme.typography.labelLarge,
                color = SongciColors.primary,
                modifier = Modifier.padding(start = 24.dp)
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
