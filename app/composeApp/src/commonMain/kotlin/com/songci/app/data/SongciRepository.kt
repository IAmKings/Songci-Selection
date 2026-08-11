package com.songci.app.data

import com.songci.app.data.db.SongciDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongciRepository(
    val db: SongciDb,
    val dynasty: Dynasty,
    val rhythmic: Rhythmic,
) {
    private val q = db.songciDbQueries

    suspend fun randomPoems(limit: Int = 20): List<Poem> = withContext(Dispatchers.Default) {
        q.randomPoems(limit.toLong()).executeAsList().map { it.toPoem() }
    }

    /**
     * 每日推荐池:当天固定 20 首(池缓存);未推荐+非异常候选随机,标记推荐日期;
     * 候选不足(池将尽)→ 重置标记循环。缺字词在应用层过滤(与子集化字体联动)。
     */
    suspend fun dailyPool(date: Long): List<Poem> = withContext(Dispatchers.Default) {
        q.poolByDate(date).executeAsOneOrNull()?.let { raw ->   // raw: 逗号分隔 poem_ids(String)
            return@withContext raw.split(',')
                .mapNotNull { it.toLongOrNull() }
                .mapNotNull { id -> q.poemById(id).executeAsOneOrNull()?.toPoem() }
        }
        // 异常字符过滤:⿰ 为汉字结构描述符(源数据合体字错误拆分,如"月⿰金");缺字字符渲染豆腐块
        // (与 scripts/font-charset.txt 联动);SQL 侧已滤 ⿰,此处双保险
        val missingChars = listOf("⿰", "𠴇", "𫍙")   // U+2FF0 / U+20D07 / U+2B359
        fun filterMissing(list: List<com.songci.app.data.db.DailyCandidates>): List<com.songci.app.data.db.DailyCandidates> =
            list.filter { row -> missingChars.none { row.content.contains(it) } }

        var candidates = filterMissing(q.dailyCandidates(200).executeAsList())
        if (candidates.size < 20) {
            q.resetRecommendations()   // 池将尽:重置循环
            candidates = filterMissing(q.dailyCandidates(200).executeAsList())
        }
        val picked = candidates.take(20)
        q.insertPool(date, picked.joinToString(",") { it.id.toString() })
        q.markRecommended(date, picked.map { it.id })
        picked.map { it.toPoem() }
    }

    suspend fun poemById(id: Long): Poem? = withContext(Dispatchers.Default) {
        q.poemById(id).executeAsOneOrNull()?.toPoem()
    }

    suspend fun authors(): List<Author> = withContext(Dispatchers.Default) {
        q.allAuthors().executeAsList().map { Author(it.id, it.name, it.long_desc ?: "") }
    }

    suspend fun authorById(id: Long): Author? = withContext(Dispatchers.Default) {
        q.authorById(id).executeAsOneOrNull()?.let { Author(it.id, it.name, it.long_desc ?: "") }
    }

    suspend fun authorsByDynasty(dynastyName: String): List<Author> = withContext(Dispatchers.Default) {
        authors().filter { dynasty.of(it.id) == dynastyName }
    }

    suspend fun poemsByAuthor(authorId: Long): List<Poem> = withContext(Dispatchers.Default) {
        q.poemsByAuthor(authorId).executeAsList().map { it.toPoem() }
    }

    /** 词牌列表:仅含 ⿰ 的异常词牌归并到主词牌(「⿰⿰⿰·七娘子」→「七娘子」),不单独成条目。 */
    suspend fun rhythmics(): List<String> = withContext(Dispatchers.Default) {
        q.allRhythmics().executeAsList().map(::cleanRhythmic).distinct().sorted()
    }

    /** 显示归并:仅 ⿰ 词牌「A·B」取主词牌 B;普通词牌名原样(历史原貌保留)。 */
    fun cleanRhythmic(raw: String): String {
        if ("⿰" !in raw) return raw
        return raw.replace("⿰", "").substringAfterLast("·").trim().ifBlank { raw }
    }

    suspend fun poemsByRhythmic(rhythmic: String, limit: Int = 100): List<Poem> =
        withContext(Dispatchers.Default) {
            q.poemsByRhythmic(rhythmic, rhythmic, limit.toLong()).executeAsList().map { it.toPoem() }
        }

    /** 词牌模糊筛选(「水」→水调歌头/水龙吟…)。 */
    suspend fun poemsByRhythmicContains(rhythmic: String, limit: Int = 100): List<Poem> =
        withContext(Dispatchers.Default) {
            q.poemsByRhythmicContains(rhythmic, limit.toLong()).executeAsList().map { it.toPoem() }
        }

    suspend fun search(query: String, limit: Int = 100): List<Poem> =
        withContext(Dispatchers.Default) {
            val base = q.search(query, query, query, query, limit.toLong()).executeAsList().map { it.toPoem() }
            // 异名展开: 搜词牌别名(出塞/大江东去)时并入同调词牌的词(仅 q 命中词牌时触发)
            val expanded = rhythmic.expand(query.trim())
            if (expanded.isEmpty() || expanded == listOf(query.trim())) return@withContext base
            val byRhythmic = expanded.mapNotNull { name ->
                q.poemsByRhythmic(name, name, limit.toLong()).executeAsList().map { it.toPoem() }
            }.flatten()
            (base + byRhythmic).distinctBy { it.id }.take(limit)
        }

    suspend fun favorites(): List<Favorite> = withContext(Dispatchers.Default) {
        q.favorites().executeAsList().map {
            Favorite(
                poem = Poem(it.id, it.rhythmic, it.content, it.author_id, it.author_name),
                createdAt = it.created_at,
            )
        }
    }

    suspend fun addFavorite(poemId: Long) = withContext(Dispatchers.Default) {
        q.insertFavorite(poemId)
    }

    suspend fun removeFavorite(poemId: Long) = withContext(Dispatchers.Default) {
        q.deleteFavorite(poemId)
    }

    /** 最近查看(去重置顶,上限 30):进入详情页时由 recordView 记录。 */
    suspend fun recentViews(limit: Int = 30): List<Poem> = withContext(Dispatchers.Default) {
        q.recentViews(limit.toLong()).executeAsList().map { it.toPoem() }
    }

    /** 记录一次浏览:同词刷新时间戳置顶(INSERT OR REPLACE),随后裁剪到最近 N 条。 */
    suspend fun recordView(poemId: Long, limit: Int = 30) = withContext(Dispatchers.Default) {
        q.recordView(poemId)
        q.trimRecentViews(limit.toLong())
    }

    /** 数据中实际存在的朝代(按时间序),用于筛选 chip。 */
    suspend fun knownDynasties(): List<String> = withContext(Dispatchers.Default) {
        val present = authors().mapNotNull { dynasty.of(it.id).takeIf { d -> d != Dynasty.UNKNOWN } }
        DYNASTY_ORDER.filter { it in present }
    }

    companion object {
        private val DYNASTY_ORDER = listOf("唐", "五代", "宋", "北宋", "南宋", "金", "元", "明", "清")
    }
}

private fun com.songci.app.data.db.RandomPoems.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.PoemById.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.DailyCandidates.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.PoemsByAuthor.toPoem() =
    Poem(id, rhythmic, content, authorId = null, authorName = "")

private fun com.songci.app.data.db.PoemsByRhythmic.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.PoemsByRhythmicContains.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.Search.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.RecentViews.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)
