package com.songci.app.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** DESIGN.md 品牌签名:primary 色矩形条(桌面 5dp 高、宽 56dp;手机 4dp 高)。 */
@Composable
fun Kicker(
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 5.dp,
) {
    Box(modifier.width(width).height(height).background(SongciColors.primary))
}
