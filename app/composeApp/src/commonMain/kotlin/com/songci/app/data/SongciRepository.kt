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
        // 多取 2 倍候选,应用层滤乱码后仍保证返回足量(乱码 23 首/20002 候选,单次命中概率低但需兜底)
        q.randomPoems((limit * 2L).coerceAtLeast(40)).executeAsList().map { it.toPoem() }
            .filter { !it.content.containsGarbled() }
            .take(limit)
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
        // (与 scripts/font-charset.txt 联动);SQL 侧已滤 ⿰,此处双保险;乱码(编码损坏)一并排除
        val missingChars = listOf("⿰", "𠴇", "𫍙")   // U+2FF0 / U+20D07 / U+2B359
        fun filterMissing(list: List<com.songci.app.data.db.DailyCandidates>): List<com.songci.app.data.db.DailyCandidates> =
            list.filter { row -> missingChars.none { row.content.contains(it) } && !row.content.containsGarbled() }

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
        q.allAuthors().executeAsList().map { Author(it.id, it.name, it.long_desc ?: "", it.pinyin_head ?: "#") }
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

    /** 词牌列表:拼音首字母分组(数据层 rhythmic_index 预计算排序;含 ⿰ 的异常词牌已归并主词牌)。 */
    suspend fun rhythmics(): List<RhythmicIndex> = withContext(Dispatchers.Default) {
        q.allRhythmics().executeAsList().map { RhythmicIndex(it.rhythmic, it.pinyin_head) }
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

    /** 搜索(兼容入口):返回词作列表,高亮标记见 [searchWithMatch]。 */
    suspend fun search(query: String, limit: Int = 100): List<Poem> =
        searchWithMatch(query, limit).map { it.poem }

    /**
     * 搜索并返回匹配标记(词牌/作者/内容命中高亮子串):
     * - 中文 LIKE:命中字段的子串;long_desc(作者号)命中 → 作者高亮
     * - 拼音首字母:命中整词牌/整作者;异名展开:展开词牌整名
     */
    suspend fun searchWithMatch(query: String, limit: Int = 100): List<SearchMatch> =
        withContext(Dispatchers.Default) {
            val qTrim = query.trim()
            val base = q.search(query, query, query, query, limit.toLong()).executeAsList().map { it.toPoem() }
                .map { p ->
                    SearchMatch(
                        poem = p,
                        rhythmicHighlight = qTrim.takeIf { it.isNotEmpty() && it in p.rhythmic },
                        authorHighlight = qTrim.takeIf { it.isNotEmpty() && (it in p.authorName || it in (p.authorName + p.rhythmic)) },
                        contentHighlight = qTrim.takeIf { it.isNotEmpty() && it in p.content },
                    )
                }
            var results: List<SearchMatch> = base
            // 拼音首字母匹配:纯字母输入 → 词牌缩写(sdgt→水调歌头)/作者缩写(ss→苏轼)
            // 均分配额:byRhythmic 与 byAuthor 各占一半,避免单源(如 ss 命中 23 词牌)挤掉另一类
            val qLower = qTrim.lowercase()
            if (qLower.isNotEmpty() && qLower.all { it in 'a'..'z' }) {
                val half = limit / 2
                // 拼音前缀匹配(缩写+全拼,非模糊):sdgt→水调歌头,shui→水调歌头
                val byRhythmic = q.rhythmicsByPinyinAbbr(qLower, qLower).executeAsList()
                    .mapNotNull { rhy ->
                        q.poemsByRhythmic(rhy, rhy, 20L).executeAsList().map { p ->
                            SearchMatch(
                                Poem(p.id, p.rhythmic, p.content, p.author_id, p.author_name),
                                rhythmicHighlight = rhy,   // 拼音命中:整词牌名高亮
                            )
                        }
                    }.flatten().distinctBy { it.poem.id }.take(half)
                val byAuthor = q.authorsByPinyinAbbr(qLower, qLower).executeAsList()
                    .mapNotNull { a ->
                        q.poemsByAuthor(a.id).executeAsList().map { p ->
                            SearchMatch(
                                p.toPoem(),   // PoemsByAuthor.toPoem():authorId/authorName 已带(勿硬编码空)
                                authorHighlight = a.name,   // 拼音命中:整作者名高亮
                            )
                        }
                    }.flatten().distinctBy { it.poem.id }.take(half)
                results = (base + byRhythmic + byAuthor).distinctBy { it.poem.id }.take(limit)
            }
            // 异名展开: 搜词牌别名(出塞/大江东去)时并入同调词牌的词(仅 q 命中词牌时触发)
            val expanded = rhythmic.expand(qTrim)
            if (expanded.isEmpty() || expanded == listOf(qTrim)) return@withContext results
            val byRhythmic = expanded.mapNotNull { name ->
                q.poemsByRhythmic(name, name, limit.toLong()).executeAsList().map { p ->
                    SearchMatch(
                        Poem(p.id, p.rhythmic, p.content, p.author_id, p.author_name),
                        rhythmicHighlight = name,   // 异名展开:展开词牌整名高亮
                    )
                }
            }.flatten()
            (results + byRhythmic).distinctBy { it.poem.id }.take(limit)
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
    Poem(id, rhythmic, content, authorId = author_id, authorName = author_name)

private fun com.songci.app.data.db.PoemsByRhythmic.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.PoemsByRhythmicContains.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.Search.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.RecentViews.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)
