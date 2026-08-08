package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songci.app.data.Poem
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.components.EmptyState
import com.songci.app.ui.components.PoemList

/** 下划线输入框(底色 surfaceContainerLow,聚焦 primary 底线)。 */
@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = SongciColors.stone) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = SongciColors.nearBlack),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SongciColors.surfaceContainerLow,
            unfocusedContainerColor = SongciColors.surfaceContainerLow,
            focusedIndicatorColor = SongciColors.primary,
            unfocusedIndicatorColor = SongciColors.stone,
        ),
    )
}

/** 搜索:关键词(作者/词牌/诗句) + 朝代筛选 + 词牌精确筛选。 */
@Composable
fun SearchScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenPoem: (Poem) -> Unit,
) {
    val results by vm.searchResults.collectAsState()
    val dynasties by vm.knownDynasties.collectAsState()

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
            Text("搜索", style = MaterialTheme.typography.labelMedium, color = SongciColors.stone)
        }

        SearchField(vm.searchQuery, vm::search, "搜索作者、词牌、诗句...")

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            FilterChip(
                selected = vm.searchDynasty.isEmpty(),
                onClick = { vm.updateSearchDynasty("") },
                label = { Text("全部", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.padding(end = 8.dp),
            )
            dynasties.forEach { d ->
                FilterChip(
                    selected = vm.searchDynasty == d,
                    onClick = { vm.updateSearchDynasty(d) },
                    label = { Text(d, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        SearchField(vm.searchRhythmic, vm::updateSearchRhythmic, "词牌筛选(精确,可空)")

        if (results.isEmpty()) {
            EmptyState("输入关键词开始搜索")
        } else {
            PoemList(results, onClick = onOpenPoem)
        }
    }
}
