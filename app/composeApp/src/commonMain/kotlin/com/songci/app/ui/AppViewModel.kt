package com.songci.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songci.app.data.Author
import com.songci.app.data.Dynasty
import com.songci.app.data.Poem
import com.songci.app.data.RhythmicSpec
import com.songci.app.data.SongciRepository
import com.songci.app.data.loadFontScaleName
import com.songci.app.data.saveFontScaleName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 阅读字号档位(相对正文 18/20px 的比例)。 */
enum class FontScale(val label: String, val scale: Float) {
    SMALL("小", 0.9f),
    MEDIUM("中", 1.0f),
    LARGE("大", 1.15f),
}

class AppViewModel(private val repo: SongciRepository) : ViewModel() {

    val dynasty: Dynasty = repo.dynasty

    private val _randomPoems = MutableStateFlow<List<Poem>>(emptyList())
    val randomPoems: StateFlow<List<Poem>> = _randomPoems

    private val _favorites = MutableStateFlow<List<com.songci.app.data.Favorite>>(emptyList())
    val favorites: StateFlow<List<com.songci.app.data.Favorite>> = _favorites

    private val _authors = MutableStateFlow<List<Author>>(emptyList())
    val authors: StateFlow<List<Author>> = _authors

    private val _rhythmics = MutableStateFlow<List<String>>(emptyList())
    val rhythmics: StateFlow<List<String>> = _rhythmics

    private val _knownDynasties = MutableStateFlow<List<String>>(emptyList())
    val knownDynasties: StateFlow<List<String>> = _knownDynasties

    var fontScale by mutableStateOf(
        FontScale.entries.firstOrNull { it.name == loadFontScaleName() } ?: FontScale.MEDIUM
    )
        private set

    // 搜索状态
    var searchQuery by mutableStateOf("")
        private set
    private val _searchResults = MutableStateFlow<List<Poem>>(emptyList())
    val searchResults: StateFlow<List<Poem>> = _searchResults
    var searchRhythmic by mutableStateOf("")
        private set
    var searchDynasty by mutableStateOf("")   // "" = 全部
        private set

    init {
        // 随机词流由 HomeScreen 进入时刷新(refreshRandom),此处不预载
        viewModelScope.launch { _favorites.value = repo.favorites() }
        viewModelScope.launch { _authors.value = repo.authors() }
        viewModelScope.launch { _rhythmics.value = repo.rhythmics() }
        viewModelScope.launch { _knownDynasties.value = repo.knownDynasties() }
    }

    fun updateFontScale(scale: FontScale) {
        fontScale = scale
        saveFontScaleName(scale.name)
    }

    fun refreshRandom() {
        viewModelScope.launch { _randomPoems.value = repo.randomPoems(20) }
    }

    fun rhythmicSpec(rhythmic: String): RhythmicSpec? = repo.rhythmic.of(rhythmic)

    /** 词牌显示归并:含 ⿰ 的异常词牌 → 主词牌(跳转/链接显示用,源数据不动)。 */
    fun cleanRhythmic(raw: String): String = repo.cleanRhythmic(raw)

    suspend fun poem(id: Long): Poem? = repo.poemById(id)

    suspend fun poemsByAuthor(authorId: Long): List<Poem> = repo.poemsByAuthor(authorId)

    suspend fun poemsByRhythmic(rhythmic: String): List<Poem> = repo.poemsByRhythmic(rhythmic)

    suspend fun authorsByDynasty(dynasty: String): List<Author> = repo.authorsByDynasty(dynasty)

    fun search(query: String) {
        searchQuery = query
        runSearch()
    }

    fun updateSearchRhythmic(rhythmic: String) {
        searchRhythmic = rhythmic
        runSearch()
    }

    fun updateSearchDynasty(dynasty: String) {
        searchDynasty = dynasty
        runSearch()
    }

    private fun runSearch() {
        viewModelScope.launch {
            val q = searchQuery.trim()
            val results = when {
                q.isEmpty() && searchRhythmic.isEmpty() -> emptyList()
                q.isEmpty() -> repo.poemsByRhythmic(searchRhythmic)
                else -> repo.search(q)
            }
            val filtered = when {
                searchRhythmic.isNotEmpty() -> results.filter { it.rhythmic == searchRhythmic }
                searchDynasty.isEmpty() -> results
                else -> results.filter { repo.dynasty.of(it.authorId) == searchDynasty }
            }
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
}
