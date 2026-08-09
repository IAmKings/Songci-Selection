package com.songci.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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

private val POEM_KEY = ActionParameters.Key<Long>("poemId")

/** 四规格定义,与 macOS WidgetKit 四规格一一对应(添加面板独立条目,固定尺寸不可拖拽)。 */
enum class WidgetSpec(
    val showActions: Boolean,   // ↻/♡
    val maxLines: Int?,         // null = 全文
    val banner: Boolean,        // 横幅布局(词句单行)
) {
    Small(false, 2, false),    // 2x2
    Banner(false, 1, true),    // 4x1
    Medium(true, 3, false),    // 4x2
    Large(true, null, false),  // 4x4
}

/** 同进程直读 db 随机一首词。失败返回 null → 渲染兜底文案。 */
private suspend fun randomPoem(): Poem? = try {
    val db = SongciDb(createDatabaseDriver())
    db.songciDbQueries.randomPoems(1L).executeAsList().firstOrNull()
        ?.let { Poem(it.id, it.rhythmic, it.content, null, "") }
} catch (e: Exception) {
    null
}

/** 四规格共享内容:按 spec 定制布局,整卡可点直达对应词(子级按钮优先)。 */
@Composable
private fun WidgetContent(poem: Poem?, spec: WidgetSpec) {
    Column(
        // 整卡可点直达对应词(actionStartActivity 的参数走 trampoline 变 extra,
        // 不会进 intent.data,故用 OpenPoemAction 显式发 ACTION_VIEW 深链)
        modifier = GlanceModifier.fillMaxSize().background(BG).padding(12.dp)
            .clickable(actionRunCallback<OpenPoemAction>(
                actionParametersOf(POEM_KEY to (poem?.id ?: 0L)))),
        verticalAlignment = Alignment.Top,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${poem?.rhythmic ?: "宋词"}${poem?.authorName?.let { " · $it" } ?: ""}",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = INK),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (spec.showActions) {
                Text("↻", style = TextStyle(fontSize = 14.sp, color = INK),
                     modifier = GlanceModifier.padding(start = 6.dp)
                         .clickable(actionRunCallback<RefreshAction>()))
                Text("♡", style = TextStyle(fontSize = 14.sp, color = INK),
                     modifier = GlanceModifier.padding(start = 8.dp)
                         .clickable(actionRunCallback<FavoriteAction>(
                             actionParametersOf(POEM_KEY to (poem?.id ?: 0L)))))
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        if (spec.banner) {
            // 横幅:词句单行
            Text(poem?.content?.lines()?.firstOrNull { it.isNotBlank() } ?: "随机一词",
                 style = TextStyle(fontSize = 11.sp, color = STONE),
                 modifier = GlanceModifier.fillMaxWidth())
        } else {
            val lines = poem?.content?.lines()?.filter { it.isNotBlank() } ?: listOf("随机一词")
            val shown = if (spec.maxLines != null) lines.take(spec.maxLines) else lines
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                shown.forEach { line ->
                    Text(line.take(20), style = TextStyle(fontSize = 11.sp, color = STONE),
                         modifier = GlanceModifier.fillMaxWidth().padding(bottom = 3.dp))
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (spec.maxLines == null) {   // 大规格:阅读全文提示
                Text("阅读全文 ›", style = TextStyle(fontSize = 11.sp, color = INK))
            }
        }
    }
}

/** 四规格基类:固定规格(尺寸由 provider XML 决定,SizeMode.Single),添加面板 4 个独立条目。 */
abstract class SongciWidgetBase(private val spec: WidgetSpec) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val poem = randomPoem()   // 同进程直读 db(先取后渲染)
        provideContent { WidgetContent(poem, spec) }
    }
}

class SongciWidgetSmall : SongciWidgetBase(WidgetSpec.Small)
class SongciWidgetBanner : SongciWidgetBase(WidgetSpec.Banner)
class SongciWidgetMedium : SongciWidgetBase(WidgetSpec.Medium)
class SongciWidgetLarge : SongciWidgetBase(WidgetSpec.Large)

/** 刷新:四规格各自重随机。 */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        listOf(SongciWidgetSmall(), SongciWidgetBanner(), SongciWidgetMedium(), SongciWidgetLarge())
            .forEach { it.updateAll(context) }
    }
}

/** 整卡点击:发 ACTION_VIEW 深链直达对应词详情。 */
class OpenPoemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val poemId = parameters[POEM_KEY] ?: return
        if (poemId <= 0) return   // db 失败兜底(null → id 0),不发无效深链
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("songci://poem/$poemId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** 收藏:写 favorites 表(同进程直读 db 模式)。 */
class FavoriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val poemId = parameters[POEM_KEY] ?: return
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val db = SongciDb(createDatabaseDriver())
                db.songciDbQueries.insertFavorite(poemId)
            }
        }
    }
}

/** 2x2 条目(沿用旧 receiver 名,已添加的旧组件不失效)。 */
class SongciWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SongciWidgetSmall()
}

class SongciWidgetBannerReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SongciWidgetBanner()
}

class SongciWidgetMediumReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SongciWidgetMedium()
}

class SongciWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SongciWidgetLarge()
}
