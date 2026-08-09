package com.songci.app

import androidx.compose.runtime.Composable
import com.songci.app.ui.SongciApp

@Composable
fun App(initialPoemId: Long? = null) {
    SongciApp(initialPoemId = initialPoemId)
}
