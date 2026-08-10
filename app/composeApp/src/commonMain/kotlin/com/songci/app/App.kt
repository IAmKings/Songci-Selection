package com.songci.app

import androidx.compose.runtime.Composable
import com.songci.app.ui.SongciApp

@Composable
fun App(initialPoemId: Long? = null, deepLinkToken: Int = 0, deepLinkQueue: kotlinx.coroutines.channels.Channel<Long>? = null) {
    SongciApp(initialPoemId = initialPoemId, deepLinkToken = deepLinkToken, deepLinkQueue = deepLinkQueue)
}
