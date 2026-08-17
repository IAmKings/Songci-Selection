package com.songci.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songci.app.data.Author
import com.songci.app.data.Dynasty
import com.songci.app.data.Poem
import com.songci.app.data.RhythmicIndex
import com.songci.app.data.SearchMatch
import com.songci.app.data.RhythmicSpec
import com.songci.app.data.Segmenter
import com.songci.app.data.SongciRepository
import com.songci.app.data.NotificationPrefs
import com.songci.app.data.loadFontScaleName
import com.songci.app.data.loadFontStyle
import com.songci.app.data.loadNotificationPrefs
import com.songci.app.data.rescheduleDailyNotification
import com.songci.app.data.saveFontScaleName
import com.songci.app.data.saveFontStyle
import com.songci.app.data.saveNotificationPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

/** 阅读字号档位(相对正文 18/20px 的比例)。 */
enum class FontScale(val label: String, val scale: Float) {
    SMALL("小", 0.9f),
    MEDIUM("中", 1.0f),
    LARGE("大", 1.15f),
}

/** 词文字体风格:楷体(LXGW WenKai,默认)/ 宋体(霞鹜新致宋)。widget 无法使用自定义字体(平台限制),仅应用内生效。 */
enum class FontStyle(val label: String) {
    KAITI("楷体"),
    SONGTI("宋体"),
    MINGTI("明体"),
}

/** 当日日期(本地时区)。datetime 0.7 转换 API 为 Instant.toLocalDateTime(TimeZone) 扩展。 */
@OptIn(kotlin.time.ExperimentalTime::class)
fun todayLocalDate(): kotlinx.datetime.LocalDate {
    val now = kotlin.time.Clock.System.now()
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    return now.toLocalDateTime(tz).date
}

/**
 * 距下一个本地午夜(0 点)的毫秒数:首页跨天精确刷新用。
 * 纯算术:本地 0 点 = localMillis 对齐 86400000(任何时区恒成立;DST 只影响 1-4 点,0 点不动)。
 * 不依赖 datetime 0.7 的 plus/atStartOfDayIn(API 变更频繁)。
 */
@OptIn(kotlin.time.ExperimentalTime::class)
fun msUntilNextMidnight(): Long {
    val now = kotlin.time.Clock.System.now()
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val offsetSec = tz.offsetAt(now).totalSeconds.toLong()
    val localNow = now.toEpochMilliseconds() + offsetSec * 1000L
    return 86_400_000L - localNow % 86_400_000L
}

class AppViewModel(private val repo: SongciRepository) : ViewModel() {

    val dynasty: Dynasty = repo.dynasty

    private val _randomPoems = MutableStateFlow<List<Poem>>(emptyList())
    val randomPoems: StateFlow<List<Poem>> = _randomPoems

    private val _dailyPoems = MutableStateFlow<List<Poem>>(emptyList())
    val dailyPoems: StateFlow<List<Poem>> = _dailyPoems

    private val _favorites = MutableStateFlow<List<com.songci.app.data.Favorite>>(emptyList())
    val favorites: StateFlow<List<com.songci.app.data.Favorite>> = _favorites

    private val _recentViews = MutableStateFlow<List<Poem>>(emptyList())
    val recentViews: StateFlow<List<Poem>> = _recentViews

    private val _authors = MutableStateFlow<List<Author>>(emptyList())
    val authors: StateFlow<List<Author>> = _authors

    private val _rhythmics = MutableStateFlow<List<RhythmicIndex>>(emptyList())
    val rhythmics: StateFlow<List<RhythmicIndex>> = _rhythmics

    private val _knownDynasties = MutableStateFlow<List<String>>(emptyList())
    val knownDynasties: StateFlow<List<String>> = _knownDynasties

    var fontScale by mutableStateOf(
        FontScale.entries.firstOrNull { it.name == loadFontScaleName() } ?: FontScale.MEDIUM
    )
        private set

    var fontStyle by mutableStateOf(
        FontStyle.entries.firstOrNull { it.name == loadFontStyle() } ?: FontStyle.KAITI
    )
        private set

    // 每日一词通知设置(本地通知,无服务器)
    var notificationPrefs by mutableStateOf(loadNotificationPrefs())
        private set

    // 调试暗门(会话内保持):设置页连点版本号 5 次显示数据库信息;VM 单例存活期内保持,重启重置
    var showDbInfo by mutableStateOf(false)

    // 搜索状态
    var searchQuery by mutableStateOf("")
        private set
    private val _searchResults = MutableStateFlow<List<SearchMatch>>(emptyList())
    val searchResults: StateFlow<List<SearchMatch>> = _searchResults
    var searchDynasty by mutableStateOf("")   // "" = 全部
        private set

    init {
        // 随机词流由 HomeScreen 进入时刷新(refreshRandom),此处不预载
        viewModelScope.launch { _favorites.value = repo.favorites() }
        viewModelScope.launch { _recentViews.value = repo.recentViews() }
        viewModelScope.launch { _authors.value = repo.authors() }
        viewModelScope.launch { _rhythmics.value = repo.rhythmics() }
        viewModelScope.launch { _knownDynasties.value = repo.knownDynasties() }
    }

    fun updateFontScale(scale: FontScale) {
        fontScale = scale
        saveFontScaleName(scale.name)
    }

    fun updateFontStyle(style: FontStyle) {
        fontStyle = style
        saveFontStyle(style.name)
    }

    /** 保存通知设置并触发平台重排(开启→排期,关闭→取消)。 */
    fun updateNotificationPrefs(prefs: NotificationPrefs) {
        notificationPrefs = prefs
        saveNotificationPrefs(prefs)
        rescheduleDailyNotification(prefs)
    }

    fun refreshRandom() {
        viewModelScope.launch { _randomPoems.value = repo.randomPoems(20) }
    }

    /** 每日推荐池(当天固定 20 首,0 点/首次进入刷新)。日期可注入(测试/0 点轮询)。 */
    fun refreshDaily(date: kotlinx.datetime.LocalDate = todayLocalDate()) {
        viewModelScope.launch {
            _dailyPoems.value = repo.dailyPool(date.toEpochDays())
        }
    }

    fun rhythmicSpec(rhythmic: String): RhythmicSpec? = repo.rhythmic.of(rhythmic)

    /** 词牌显示归并:含 ⿰ 的异常词牌 → 主词牌(跳转/链接显示用,源数据不动)。 */
    fun cleanRhythmic(raw: String): String = repo.cleanRhythmic(raw)

    /** 按词作字数匹配格律体(分段用);无匹配体回退首体。 */
    fun matchedSpec(rhythmic: String, content: String): RhythmicSpec? =
        repo.rhythmic.matchBody(rhythmic, Segmenter.chars(content).length)
            ?: repo.rhythmic.of(rhythmic)

    /** 全部体(卡片切换用)。 */
    fun bodiesOf(rhythmic: String): List<RhythmicSpec> = repo.rhythmic.bodiesOf(rhythmic)

    suspend fun author(authorId: Long): Author? = repo.authorById(authorId)

    suspend fun poem(id: Long): Poem? = repo.poemById(id)

    suspend fun poemsByAuthor(authorId: Long): List<Poem> = repo.poemsByAuthor(authorId)

    suspend fun poemsByRhythmic(rhythmic: String): List<Poem> = repo.poemsByRhythmic(rhythmic)

    suspend fun authorsByDynasty(dynasty: String): List<Author> = repo.authorsByDynasty(dynasty)

    fun search(query: String) {
        searchQuery = query
        runSearch()
    }

    fun updateSearchDynasty(dynasty: String) {
        searchDynasty = dynasty
        runSearch()
    }

    /** 搜索协程句柄:每次输入取消旧查询,防止慢查询后到覆盖新结果(竞态)。 */
    private var searchJob: Job? = null

    private fun runSearch() {
        searchJob?.cancel()   // ① 取消旧查询(旧协程不再写入结果)
        searchJob = viewModelScope.launch {
            val q = searchQuery.trim()
            val results = if (q.isEmpty()) emptyList<SearchMatch>() else repo.searchWithMatch(q)
            // ② 过期校验:查询已变(用户继续输入/清除)时丢弃本次结果,只允许最后一次输入落盘
            if (searchQuery.trim() != q) return@launch
            val filtered = if (searchDynasty.isEmpty()) results
            else results.filter { repo.dynasty.of(it.poem.authorId) == searchDynasty }
            _searchResults.value = filtered
        }
    }

    /** 按目标状态写入收藏(幂等),避免「读当前态再取反」的竞态。 */
    fun setFavorite(poem: Poem, favorite: Boolean) {
        viewModelScope.launch {
            if (favorite) repo.addFavorite(poem.id) else repo.removeFavorite(poem.id)
            _favorites.value = repo.favorites()
        }
    }

    fun removeFavorite(poemId: Long) {
        viewModelScope.launch {
            repo.removeFavorite(poemId)
            _favorites.value = repo.favorites()
        }
    }

    suspend fun isFavorite(poemId: Long): Boolean =
        repo.favorites().any { it.poem.id == poemId }

    /** 进入详情页即记录浏览(openPoem 统一入口);列表去重置顶,上限 30 条。 */
    fun recordView(poemId: Long) {
        viewModelScope.launch {
            repo.recordView(poemId)
            _recentViews.value = repo.recentViews()
        }
    }
}
