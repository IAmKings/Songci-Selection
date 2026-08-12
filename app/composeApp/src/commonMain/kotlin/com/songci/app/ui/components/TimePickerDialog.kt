package com.songci.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.songci.app.theme.SongciColors

/**
 * 古典风时间选择器:时/分步进(「− 数字 +」,跨小时进位,23:59+1→00:00 循环),
 * 翻页钟形态。不走 Material3 TimePicker(现代圆盘与古典设计系统不搭)。
 */
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SongciColors.stone.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(SongciColors.surfaceContainerLow)
                .border(1.dp, SongciColors.line)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "每日一词 · 推送时间",
                style = MaterialTheme.typography.labelMedium,
                color = SongciColors.stone,
            )
            Row(
                modifier = Modifier.padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TimeStepper("时", hour, 24) { hour = it }
                Text("·", style = MaterialTheme.typography.displaySmall, color = SongciColors.stone)
                TimeStepper("分", minute, 60) { minute = it }
            }
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "取消",
                    style = MaterialTheme.typography.labelLarge,
                    color = SongciColors.stone,
                    modifier = Modifier
                        .border(1.dp, SongciColors.line)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Text(
                    "确定",
                    style = MaterialTheme.typography.labelLarge,
                    color = SongciColors.onPrimary,
                    modifier = Modifier
                        .background(SongciColors.primary)
                        .clickable { onConfirm(hour, minute) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** 单组步进:− 数字 +;wrap=基数(24/60)循环。 */
@Composable
private fun TimeStepper(label: String, value: Int, wrap: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SongciColors.stone,
        )
        Text(
            "−",
            style = MaterialTheme.typography.headlineMedium,
            color = SongciColors.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .border(1.dp, SongciColors.line)
                .clickable { onChange((value - 1 + wrap) % wrap) }
                .padding(horizontal = 18.dp, vertical = 2.dp),
        )
        Text(
            value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displaySmall,
            color = SongciColors.nearBlack,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            "＋",
            style = MaterialTheme.typography.headlineMedium,
            color = SongciColors.primary,
            modifier = Modifier
                .border(1.dp, SongciColors.line)
                .clickable { onChange((value + 1) % wrap) }
                .padding(horizontal = 18.dp, vertical = 2.dp),
        )
    }
}
