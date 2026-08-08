package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.PoemCard
import com.songci.app.theme.SongciColors

/** 首页:顶栏(目录索引/标题/搜索) + 推荐词流。 */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenIndex: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPoem: (Long) -> Unit,
) {
    val poems by vm.randomPoems.collectAsState()
    LaunchedEffect(Unit) { vm.refreshRandom() }

    Column(modifier = Modifier.fillMaxSize().background(SongciColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenIndex) {
                Icon(Icons.Filled.List, contentDescription = "目录索引", tint = SongciColors.primary)
            }
            Text(
                "宋词选粹",
                style = MaterialTheme.typography.headlineMedium,
                color = SongciColors.primary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = SongciColors.primary)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        ) {
            item {
                Text(
                    "推荐词作",
                    style = MaterialTheme.typography.labelMedium,
                    color = SongciColors.stone,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(poems, key = { it.id }) { poem ->
                PoemCard(poem, onClick = { onOpenPoem(poem.id) })
            }
        }
    }
}
