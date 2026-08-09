package com.songci.app

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
        state = rememberWindowState(size = DpSize(480.dp, 760.dp))
    ) {
        App(initialPoemId = poemId)
    }
}
