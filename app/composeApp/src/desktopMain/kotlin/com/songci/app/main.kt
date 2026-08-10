package com.songci.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.awt.desktop.OpenURIHandler
import kotlinx.coroutines.channels.Channel

fun main() = application {
    // 小组件深链事件通道:OpenURIHandler 在 AppKit 线程,只 trySend 不写状态;
    // 组合内挂起迭代消费(组合作用域内处理才可靠——application 块 state + 事件线程写
    // 会导致快照跟踪脱节:写值正确但组合读旧值/不重组)
    val deepLinkChannel = Channel<Long>(capacity = Channel.UNLIMITED)
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().setOpenURIHandler(OpenURIHandler { e ->
            val uri = e.uri
            if (uri.scheme == "songci" && uri.host == "poem") {
                uri.path.trimStart('/').toLongOrNull()?.let(deepLinkChannel::trySend)
            }
        })
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "宋词选粹",
        state = rememberWindowState(size = DpSize(480.dp, 760.dp)),
    ) {
        // 最小窗口尺寸:防止过窄导致排版错位(窄屏路径按 768dp 断点,360 像素约为最小可用)
        // WindowScope.window 为公开属性(ComposeWindow)
        LaunchedEffect(Unit) { window.minimumSize = java.awt.Dimension(360, 520) }

        // 深链事件直接传通道给 common 层:组合内挂起迭代+导航,
        // 不经参数传递(state 参数传递层快照脱节,已实证 token 恒 0)
        App(deepLinkQueue = deepLinkChannel)
    }
}
