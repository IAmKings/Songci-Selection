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

private fun com.songci.app.data.db.PoemsByAuthor.toPoem() =
    Poem(id, rhythmic, content, authorId = null, authorName = "")

private fun com.songci.app.data.db.PoemsByRhythmic.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.PoemsByRhythmicContains.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)

private fun com.songci.app.data.db.Search.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)
