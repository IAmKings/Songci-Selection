package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.FontScale
import com.songci.app.ui.components.SimpleListScreen

/** 设置:阅读设置(字号)+ 关于;账号/通知/退出登录为占位。 */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    SimpleListScreen(title = "设置") {
        Text(
            "阅读设置",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.stone,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            FontScale.entries.forEach { scale ->
                val selected = vm.fontScale == scale
                Text(
                    scale.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) SongciColors.onPrimary else SongciColors.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .border(1.dp, SongciColors.primary)
                        .background(if (selected) SongciColors.primary else SongciColors.surfaceContainerLow)
                        .clickable { vm.updateFontScale(scale) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        Text(
            "关于宋词选粹",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.stone,
            modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 8.dp),
        )
        Text(
            "版本 0.1.0 · 词库 21,050 首 · 诗人 1,564 位",
            style = MaterialTheme.typography.bodyMedium,
            color = SongciColors.nearBlack,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "账号设置 / 通知设置 / 退出登录 —— 后续版本提供",
            style = MaterialTheme.typography.labelSmall,
            color = SongciColors.stone,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp),
        )
    }
}
