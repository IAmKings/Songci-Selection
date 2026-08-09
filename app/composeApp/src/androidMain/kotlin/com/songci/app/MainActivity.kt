package com.songci.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.songci.app.data.AppContextHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppContextHolder.context = applicationContext
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // deep link: songci://poem/{id}(小组件阅读全文)
        val initialPoemId = intent?.data?.let { uri ->
            if (uri.scheme == "songci" && uri.host == "poem") uri.lastPathSegment?.toLongOrNull() else null
        }
        setContent {
            App(initialPoemId = initialPoemId)
        }
    }
}
