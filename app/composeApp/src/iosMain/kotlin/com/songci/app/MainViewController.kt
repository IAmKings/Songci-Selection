package com.songci.app

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(initialPoemId: Long? = null) = ComposeUIViewController { App(initialPoemId = initialPoemId) }
