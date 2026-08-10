package com.songci.app

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(initialPoemId: Long? = null, deepLinkToken: Int = 0) =
    ComposeUIViewController { App(initialPoemId = initialPoemId, deepLinkToken = deepLinkToken) }
