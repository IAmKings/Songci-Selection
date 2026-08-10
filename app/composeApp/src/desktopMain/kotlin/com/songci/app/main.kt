package com.songci.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.awt.desktop.OpenURIHandler

fun main() = application {
    // 小组件深链: songci://poem/{id}(widgetURL 点击触发,冷启动/运行中均回调)
    var poemId by mutableStateOf<Long?>(null)
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().setOpenURIHandler(OpenURIHandler { e ->
            val uri = e.uri
            if (uri.scheme == "songci" && uri.host == "poem") {
                uri.path.trimStart('/').toLongOrNull()?.let { poemId = it }
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
        App(initialPoemId = poemId)
    }
}
