package com.songci.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.songci.app.data.AppContextHolder
import com.songci.app.data.Poem
import com.songci.app.data.createDatabaseDriver
import com.songci.app.data.db.SongciDb

/**
 * 宋词 Widget 原型:同进程直读应用 db 随机一首词(词牌+作者+词句)。
 * Glance render 在应用进程执行 → 可复用 SQLDelight 查询(前置验证 B)。
 */
class SongciWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val poem = randomPoem()   // 同进程直读 db(provideGlance 为 suspend, 先取后渲染)
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(0xFFF5F4ED.toInt())).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${poem?.rhythmic ?: "宋词"}${poem?.authorName?.let { " · $it" } ?: ""}",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(0xFF002046.toInt())),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                Text(
                    poem?.content?.lineSequence()?.firstOrNull()?.take(14) ?: "随机一词",
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(0xFF605E59.toInt())),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
        }
    }

    /** 同进程直读应用 db(原型验证:复用 SQLDelight 驱动与查询)。 */
    private suspend fun randomPoem(): Poem? {
        val ctx = AppContextHolder.context
        return try {
            val db = SongciDb(createDatabaseDriver())
            db.songciDbQueries.randomPoems(1L).executeAsList().firstOrNull()
                ?.let { Poem(it.id, it.rhythmic, it.content, null, "") }
        } catch (e: Exception) {
            null
        }
    }
}

class SongciWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SongciWidget()
}
