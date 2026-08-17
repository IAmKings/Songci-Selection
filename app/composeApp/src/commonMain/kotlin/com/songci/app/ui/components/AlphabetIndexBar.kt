package com.songci.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.songci.app.theme.SongciColors

/** 拼音首字母全序列:0(数字) + A-X + #(异常字符)。 */
private val HEADS = listOf("0") + ('A'..'X').map { it.toString() } + listOf("#")

/**
 * 右侧字母快捷导航条(通讯录式)。
 *
 * 交互优化(2026-08-17):
 * - 整条高度均分 28 个单元,每个字母点击区域 = 整条 1/28(远大于文字本身)
 * - 支持按住上下拖动连续跳转分组(标准通讯录手势)
 * - 按下的字母以胶囊背景高亮
 *
 * @param present 列表中存在分组的字母(可点高亮);其余置灰不可点
 * @param onSelect 选中某个字母分组回调(点击/拖动均触发)
 */
@Composable
fun AlphabetIndexBar(
    present: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var active by remember { mutableStateOf<String?>(null) }   // 当前按下的字母(高亮)
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.width(28.dp)) {
        val cellPx = with(density) { maxHeight.toPx() } / HEADS.size
        fun selectAt(y: Float) {
            val idx = (y / cellPx).toInt().coerceIn(0, HEADS.lastIndex)
            val head = HEADS[idx]
            active = head
            if (head in present) onSelect(head)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(present) { detectTapGestures { selectAt(it.y) } }
                .pointerInput(present) { detectVerticalDragGestures { change, _ -> selectAt(change.position.y) } },
        ) {
            HEADS.forEach { head ->
                val enabled = head in present
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),   // 单元级区域;手势由父级 detectTapGestures/drag 统一处理
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        head,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) SongciColors.primary else SongciColors.stone.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active == head) SongciColors.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
