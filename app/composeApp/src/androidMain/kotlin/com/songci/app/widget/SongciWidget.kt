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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.songci.app.R
import com.songci.app.data.Poem
import com.songci.app.data.containsGarbled
import com.songci.app.data.createDatabaseDriver
import com.songci.app.data.db.SongciDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 注意:ColorProvider 的 Int 构造器是 colorRes(资源 ID),直接传 0xFFF5F4ED.toInt() 会被当资源引用,
// launcher 渲染报 "No package ID ff found for resource ID 0xfff5f4ed" → 显示加载失败。
// 必须包一层 compose Color 才表示 ARGB 值。以下 token 对齐 design/widgets 设计稿色板。
private val BG = ColorProvider(Color(0xFFF5F4ED))           // surface-container-low
private val PRIMARY = ColorProvider(Color(0xFF002046))      // primary(色条/品牌)
private val PRIMARY_CONTAINER = ColorProvider(Color(0xFF1B365D))  // 词牌/图标主色
private val PRIMARY_CONTAINER_80 = ColorProvider(Color(0xCC1B365D))  // 词牌 80% 透明度(2x2 首句,设计稿 text-opacity-80)
private val SURFACE_80 = ColorProvider(Color(0xCCFBF9F2))  // surface 80%(4x4 底部操作栏,设计稿 bg-surface/80)
private val SECONDARY = ColorProvider(Color(0xFF605E59))    // secondary(作者/元信息)
private val STONE = ColorProvider(Color(0xFF6B6A64))        // stone(收藏图标)
private val ON_SURFACE = ColorProvider(Color(0xFF1B1C18))   // on-surface(正文)
private val ERROR = ColorProvider(Color(0xFFBA1A1A))        // error(印章红)
private val WARM_SAND = ColorProvider(Color(0xFFE8E6DC))    // warm-sand(分隔线)

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
    val driver = createDatabaseDriver()
    try {
        SongciDb(driver).songciDbQueries.randomPoems(1L).executeAsList()
            .firstOrNull { !it.content.containsGarbled() }   // 乱码词排除
            ?.let { Poem(it.id, it.rhythmic, it.content, null, "") }
    } finally {
        driver.close()   // 泄漏连接会耗尽 SQLite 连接池(上限4),后续刷新失效
    }
} catch (e: Exception) {
    null
}

/** 词句行(共用的截断)。 */
private fun poemLines(poem: Poem?, max: Int?): List<String> {
    val lines = poem?.content?.lines()?.filter { it.isNotBlank() } ?: listOf("随机一词")
    return if (max != null) lines.take(max) else lines
}

// 截断规范:词牌最长 15 字、首句最长 77 字(db 实测)。Glance 1.1.1 无省略号,
// maxLines=1 直接裁剪尾部;超长场景再叠加字符级 take 双保险。
private val SERIF = TextStyle(fontFamily = FontFamily.Serif)

/** 顶部 4px 品牌色条(kicker)。 */
@Composable
private fun Kicker(color: ColorProvider) {
    Box(GlanceModifier.fillMaxWidth().height(4.dp).background(color)) {}
}

/** 竖向分隔线(warm-sand)。 */
@Composable
private fun DividerV(modifier: GlanceModifier = GlanceModifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(WARM_SAND)) {}
}

/** 竖排文本:每字一 Text(vertical-rl 逐字下行),超限字符裁剪(与设计稿 overflow-hidden 一致)。 */
@Composable
private fun VerticalText(text: String, style: TextStyle, maxChars: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        text.take(maxChars).forEach { ch ->
            Text(ch.toString(), style = style)
        }
    }
}

/** 四角装饰线:L 形(横条 12×2 + 竖条 2×10),设计稿 warm-sand 50% → 固定色近似。start=左/右端,top=上/下排。 */
@Composable
private fun CornerLine(start: Boolean, top: Boolean) {
    Column {
        if (top) Box(GlanceModifier.width(12.dp).height(2.dp).background(WARM_SAND)) {}
        Row {
            if (start) {
                Box(GlanceModifier.width(2.dp).height(10.dp).background(WARM_SAND)) {}
                Spacer(GlanceModifier.defaultWeight())
            } else {
                Spacer(GlanceModifier.defaultWeight())
                Box(GlanceModifier.width(2.dp).height(10.dp).background(WARM_SAND)) {}
            }
        }
        if (!top) Box(GlanceModifier.width(12.dp).height(2.dp).background(WARM_SAND)) {}
    }
}

/** 整卡点击包装:直达对应词(子级按钮优先)。 */
private fun GlanceModifier.openPoem(id: Long): GlanceModifier =
    clickable(actionRunCallback<OpenPoemAction>(actionParametersOf(POEM_KEY to id)))

/** 四规格共享内容:按 design/widgets 设计稿布局,整卡可点直达对应词。 */
@Composable
private fun WidgetContent(poem: Poem?, spec: WidgetSpec) {
    when (spec) {
        WidgetSpec.Small -> SmallContent(poem)
        WidgetSpec.Banner -> BannerContent(poem)
        WidgetSpec.Medium -> MediumContent(poem)
        WidgetSpec.Large -> LargeContent(poem)
    }
}

/** 2x2:垂直水平居中——书本图标 + 词牌(24sp)+ 首句(14sp,80% 透明),圆角+边框背景。 */
@Composable
private fun SmallContent(poem: Poem?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(2.dp)
            .background(ImageProvider(R.drawable.widget_bg_round12)).padding(horizontal = 10.dp, vertical = 8.dp)
            .openPoem(poem?.id ?: 0L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,   // 设计稿 justify-center items-center
    ) {
        Image(ImageProvider(R.mipmap.ic_launcher), contentDescription = null,
              modifier = GlanceModifier.size(40.dp).padding(bottom = 10.dp))   // Logo:40dp 主视觉(自适应图标前景占 ~66%,24dp 实际过小)
        // 词牌:2x2 窄容器,超 6 字截断加省略(Glance 无自带省略号,手动拼接)
        val rhythmic = poem?.rhythmic ?: "宋词"
        Text(if (rhythmic.length > 6) rhythmic.take(6) + "…" else rhythmic,
             style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif), maxLines = 1,
             modifier = GlanceModifier.padding(bottom = 4.dp))   // 设计稿 24px + mb-1
        // 首句:超 20 字截断加省略,再 maxLines=2 兜底
        val firstLine = poem?.content?.lines()?.firstOrNull { it.isNotBlank() } ?: "随机一词"
        val trimmed = if (firstLine.length > 20) firstLine.take(20) + "…" else firstLine
        Text(trimmed,
             style = TextStyle(fontSize = 14.sp, color = PRIMARY_CONTAINER_80, fontFamily = FontFamily.Serif), maxLines = 2,
             modifier = GlanceModifier.padding(top = 2.dp))
    }
}

/** 4x1:顶部色条 + 三段式(词牌/作者 | 词句 | 刷新)。 */
@Composable
private fun BannerContent(poem: Poem?) {
    Column(modifier = GlanceModifier.fillMaxSize().background(BG)) {
        Kicker(PRIMARY)
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
                .openPoem(poem?.id ?: 0L),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.width(80.dp)) {
                Text(poem?.rhythmic ?: "宋词", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif), maxLines = 1)
                Text(poem?.authorName ?: "", style = TextStyle(fontSize = 12.sp, color = SECONDARY), maxLines = 1)
            }
            DividerV()
            Text(poem?.content?.lines()?.firstOrNull { it.isNotBlank() } ?: "随机一词",
                 style = TextStyle(fontSize = 14.sp, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic), maxLines = 1,
                 modifier = GlanceModifier.defaultWeight().padding(horizontal = 10.dp))
            Image(ImageProvider(R.drawable.ic_refresh), contentDescription = "刷新",
                  modifier = GlanceModifier.size(20.dp)
                      .clickable(actionRunCallback<RefreshAction>()))
        }
    }
}

/** 4x2:顶部色条 + 左品牌区 | 右词区(右上操作)。 */
@Composable
private fun MediumContent(poem: Poem?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(2.dp)
            .background(ImageProvider(R.drawable.widget_bg_round12)),
    ) {
        Kicker(PRIMARY)
        Row(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)
            .openPoem(poem?.id ?: 0L)) {
            // 左品牌区(设计稿 w-1/4:图标 + "宋词选粹")
            Column(
                modifier = GlanceModifier.width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(R.drawable.ic_book), contentDescription = null,
                      modifier = GlanceModifier.size(26.dp))
                Text("宋词选粹", style = TextStyle(fontSize = 14.sp, color = PRIMARY, fontFamily = FontFamily.Serif),
                     modifier = GlanceModifier.padding(top = 4.dp), maxLines = 1)
            }
            DividerV()
            // 右词区
            Column(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Spacer(GlanceModifier.defaultWeight())
                    Image(ImageProvider(R.drawable.ic_refresh_stone), contentDescription = "刷新",   // 设计稿 text-stone
                          modifier = GlanceModifier.size(18.dp)
                              .clickable(actionRunCallback<RefreshAction>()))
                    Image(ImageProvider(R.drawable.ic_favorite), contentDescription = "收藏",
                          modifier = GlanceModifier.size(18.dp).padding(start = 8.dp)
                              .clickable(actionRunCallback<FavoriteAction>(
                                  actionParametersOf(POEM_KEY to (poem?.id ?: 0L)))))
                }
                // 词牌(bold on-surface)+ 作者(secondary)同行,设计稿 items-baseline → CenterVertically 近似
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.padding(top = 4.dp)) {
                    Text(poem?.rhythmic ?: "宋词",
                         style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ON_SURFACE, fontFamily = FontFamily.Serif), maxLines = 1,
                         modifier = GlanceModifier.defaultWeight())
                    poem?.authorName?.let {
                        Text(it, style = TextStyle(fontSize = 12.sp, color = SECONDARY), maxLines = 1,
                             modifier = GlanceModifier.padding(start = 8.dp))
                    }
                }
                // 词句两行 14sp(设计稿 poem-body-mobile)
                poemLines(poem, 2).forEach { line ->
                    Text(line.take(14), style = TextStyle(fontSize = 14.sp, color = ON_SURFACE, fontFamily = FontFamily.Serif), maxLines = 1,
                         modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp))
                }
            }
        }
    }
}

/** 4x4:顶部色条 + 头部(品牌|印章)+ 竖排词牌/作者 + 竖排词句两栏 + 底部操作栏 + 四角装饰线。 */
@Composable
private fun LargeContent(poem: Poem?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(2.dp)
            .background(ImageProvider(R.drawable.widget_bg_round28)),
    ) {
        Kicker(PRIMARY_CONTAINER)
        // 顶部角线(左上/右上)
        Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp)) {
            CornerLine(start = true, top = true)
            Spacer(GlanceModifier.defaultWeight())
            CornerLine(start = false, top = true)
        }
        // 头部:品牌 24sp + 印章(设计稿 text-[24px] tracking-widest)
        Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp)) {
            Text("宋词选粹", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif))
            Spacer(GlanceModifier.defaultWeight())
            // 印章:红边框圆角 + 宋字
            Box(modifier = GlanceModifier.size(22.dp).background(ImageProvider(R.drawable.ic_seal)),
                contentAlignment = Alignment.Center) {
                Text("宋", style = TextStyle(fontSize = 10.sp, color = ERROR))
            }
        }
        // 主区:竖排词牌(28sp)+ 分隔 + 竖排词句两栏(14sp,vertical-rl:右栏上阕、左栏下阕)
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 6.dp)
                .openPoem(poem?.id ?: 0L),
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 竖排词牌:设计稿 28px;4x4 高度限制取前 6 字(28sp×1.2≈34dp/字,超出与设计稿同被裁剪)
                (poem?.rhythmic ?: "宋词").take(6).forEach { ch ->
                    Text(ch.toString(), style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif))
                }
                Box(GlanceModifier.width(1.dp).height(14.dp).background(WARM_SAND).padding(top = 4.dp)) {}
                (poem?.authorName ?: "").take(3).forEach { ch ->
                    Text(ch.toString(), style = TextStyle(fontSize = 12.sp, color = SECONDARY, fontFamily = FontFamily.Serif))
                }
            }
            DividerV()
            // 竖排词句两栏:行列表按上下阕切半,各自拼接逐字竖排,每栏限 10 字
            Row(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                val lines = poemLines(poem, 8)   // 限行防溢出(设计稿仅展示片段)
                val half = (lines.size + 1) / 2
                val lower = lines.drop(half).joinToString("")   // 下阕 → 左栏
                val upper = lines.take(half).joinToString("")   // 上阕 → 右栏
                Column(modifier = GlanceModifier.defaultWeight()) {
                    VerticalText(lower, TextStyle(fontSize = 14.sp, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif), 10)
                }
                Box(GlanceModifier.width(1.dp).fillMaxHeight().background(WARM_SAND).padding(horizontal = 6.dp)) {}
                Column(modifier = GlanceModifier.defaultWeight()) {
                    VerticalText(upper, TextStyle(fontSize = 14.sp, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif), 10)
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        // 底部操作栏(设计稿 h-52px bg-surface/80 + border-t warm-sand + 胶囊"阅读全文"按钮)
        Column {
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(WARM_SAND)) {}
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(SURFACE_80)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(R.drawable.ic_refresh), contentDescription = "刷新",
                      modifier = GlanceModifier.size(20.dp)
                          .clickable(actionRunCallback<RefreshAction>()))
                Image(ImageProvider(R.drawable.ic_bookmark), contentDescription = "收藏",
                      modifier = GlanceModifier.size(20.dp).padding(start = 14.dp)
                          .clickable(actionRunCallback<FavoriteAction>(
                              actionParametersOf(POEM_KEY to (poem?.id ?: 0L)))))
                Spacer(GlanceModifier.defaultWeight())
                // 阅读全文:胶囊按钮(圆角 16dp + warm-sand 边框),点击直达对应词
                Box(
                    modifier = GlanceModifier.background(ImageProvider(R.drawable.widget_btn_pill))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<OpenPoemAction>(
                            actionParametersOf(POEM_KEY to (poem?.id ?: 0L)))),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("阅读全文", style = TextStyle(fontSize = 12.sp, color = PRIMARY_CONTAINER, fontFamily = FontFamily.Serif))
                        Image(ImageProvider(R.drawable.ic_open_in_new), contentDescription = null,
                              modifier = GlanceModifier.size(14.dp).padding(start = 4.dp))
                    }
                }
            }
        }
        // 底部角线(左下/右下)
        Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp)) {
            CornerLine(start = true, top = false)
            Spacer(GlanceModifier.defaultWeight())
            CornerLine(start = false, top = false)
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
                val driver = createDatabaseDriver()
                try {
                    SongciDb(driver).songciDbQueries.insertFavorite(poemId)
                } finally {
                    driver.close()
                }
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
