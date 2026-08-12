package com.songci.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.songci.app.data.AppContextHolder

class MainActivity : ComponentActivity() {

    private var currentPoemId by mutableStateOf<Long?>(null)
    private var deepLinkToken by mutableStateOf(0)

    private fun parsePoemId(intent: Intent?): Long? = intent?.data?.let { uri ->
        if (uri.scheme == "songci" && uri.host == "poem") uri.lastPathSegment?.toLongOrNull() else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppContextHolder.context = applicationContext
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // deep link: songci://poem/{id}(小组件/每日通知)
        currentPoemId = parsePoemId(intent)
        setContent {
            App(initialPoemId = currentPoemId, deepLinkToken = deepLinkToken)
        }
    }

    /** 热启动深链(通知/小组件点击,进程存活时):token 递增强制重导航(同一词重复点击也生效)。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parsePoemId(intent)?.let { id ->
            currentPoemId = id
            deepLinkToken++
        }
    }
}
