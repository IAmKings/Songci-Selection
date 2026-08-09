package com.songci.app.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.songci.app.MainActivity
import com.songci.app.data.AppContextHolder
import com.songci.app.data.Poem
import com.songci.app.data.createDatabaseDriver
import com.songci.app.data.db.SongciDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 注意:ColorProvider 的 Int 构造器是 colorRes(资源 ID),直接传 0xFFF5F4ED.toInt() 会被当资源引用,
// launcher 渲染报 "No package ID ff found for resource ID 0xfff5f4ed" → 显示加载失败。
// 必须包一层 compose Color 才表示 ARGB 值。
private val BG = ColorProvider(Color(0xFFF5F4ED))
private val INK = ColorProvider(Color(0xFF002046))
private val STONE = ColorProvider(Color(0xFF605E59))

/** 宋词 Widget:四规格(2x2/4x1/4x2/4x4),同进程直读 db 随机词,刷新/收藏/阅读全文。 */
class SongciWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 250.dp), DpSize(250.dp, 520.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val poem = randomPoem()   // 同进程直读 db(先取后渲染)
        provideContent {
            val size = LocalSize.current
            val wide = size.width >= 200.dp   // 4 列规格(4x1/4x2/4x4)
            val tall = size.height >= 200.dp  // 2x2 以上(4x2/4x4)
            Column(
                modifier = GlanceModifier.fillMaxSize().background(BG).padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${poem?.rhythmic ?: "宋词"}${poem?.authorName?.let { " · $it" } ?: ""}",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = INK),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    if (wide && tall) {   // 4x2/4x4: 刷新 + 收藏
                        Text("↻", style = TextStyle(fontSize = 14.sp, color = INK),
                             modifier = GlanceModifier.padding(start = 6.dp)
                                 .clickable(actionRunCallback<RefreshAction>()))
                        Text("♡", style = TextStyle(fontSize = 14.sp, color = INK),
                             modifier = GlanceModifier.padding(start = 8.dp)
                                 .clickable(actionRunCallback<FavoriteAction>(
                                     actionParametersOf(SongciWidget.key to (poem?.id ?: 0L)))))
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                val lines = poem?.content?.lines()?.filter { it.isNotBlank() } ?: listOf("随机一词")
                val shown = when {
                    tall -> lines.take(3)
                    else -> lines.take(2)
                }
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    shown.forEach { line ->
                        Text(line.take(16), style = TextStyle(fontSize = 11.sp, color = STONE),
                             modifier = GlanceModifier.fillMaxWidth().padding(bottom = 3.dp))
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(if (wide && tall) "阅读全文 ›" else "›",
                     style = TextStyle(fontSize = 11.sp, color = INK),
                     modifier = GlanceModifier.clickable(
                         actionStartActivity<MainActivity>(
                             actionParametersOf(URI_KEY to "songci://poem/${poem?.id ?: 0}"))))
            }
        }
    }

    private suspend fun randomPoem(): Poem? = try {
        val db = SongciDb(createDatabaseDriver())
        db.songciDbQueries.randomPoems(1L).executeAsList().firstOrNull()
            ?.let { Poem(it.id, it.rhythmic, it.content, null, "") }
    } catch (e: Exception) {
        null
    }

    companion object {
        val key = ActionParameters.Key<Long>("poemId")
        private val URI_KEY = ActionParameters.Key<String>("uri")
    }
}

/** 刷新:重新随机。 */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SongciWidget().updateAll(context)
    }
}

/** 收藏:写 favorites 表(同进程直读 db 模式)。 */
class FavoriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val poemId = parameters[SongciWidget.key] ?: return
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val db = SongciDb(createDatabaseDriver())
                db.songciDbQueries.insertFavorite(poemId)
            }
        }
    }
}

class SongciWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SongciWidget()
}
