package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.EmptyState
import com.songci.app.ui.components.PoemList

/** 收藏:收藏词作列表,卡片可移除。 */
@Composable
fun FavoritesScreen(
    vm: AppViewModel,
    onOpenPoem: (Long) -> Unit,
) {
    val favorites by vm.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(SongciColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("我的收藏", style = MaterialTheme.typography.headlineMedium)
        }
        if (favorites.isEmpty()) {
            EmptyState("还没有收藏的词作")
        } else {
            PoemList(favorites.map { it.poem }) { poem -> onOpenPoem(poem.id) }
        }
    }
}
