package com.songci.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.EmptyState
import com.songci.app.ui.components.PoemList
import com.songci.app.ui.components.SimpleListScreen

/** 收藏:收藏词作列表。 */
@Composable
fun FavoritesScreen(vm: AppViewModel, onOpenPoem: (Long) -> Unit) {
    val favorites by vm.favorites.collectAsState()
    SimpleListScreen(title = "我的收藏") {
        if (favorites.isEmpty()) {
            EmptyState("还没有收藏的词作")
        } else {
            PoemList(favorites.map { it.poem }) { poem -> onOpenPoem(poem.id) }
        }
    }
}
