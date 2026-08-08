package com.songci.app.data

import com.songci.app.data.db.SongciDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongciRepository(
    val db: SongciDb,
    val dynasty: Dynasty,
) {
    private val q = db.songciDbQueries

    suspend fun randomPoems(limit: Int = 20): List<Poem> = withContext(Dispatchers.Default) {
        q.randomPoems(limit.toLong()).executeAsList().map { it.toPoem() }
    }

    suspend fun poemById(id: Long): Poem? = withContext(Dispatchers.Default) {
        q.poemById(id).executeAsOneOrNull()?.toPoem()
    }

    suspend fun authors(): List<Author> = withContext(Dispatchers.Default) {
        q.allAuthors().executeAsList().map { Author(it.id, it.name) }
    }

    suspend fun authorsByDynasty(dynastyName: String): List<Author> = withContext(Dispatchers.Default) {
        authors().filter { dynasty.of(it.id) == dynastyName }
    }

    suspend fun poemsByAuthor(authorId: Long): List<Poem> = withContext(Dispatchers.Default) {
        q.poemsByAuthor(authorId).executeAsList().map { it.toPoem() }
    }

    suspend fun rhythmics(): List<String> = withContext(Dispatchers.Default) {
        q.allRhythmics().executeAsList()
    }

    suspend fun poemsByRhythmic(rhythmic: String, limit: Int = 100): List<Poem> =
        withContext(Dispatchers.Default) {
            q.poemsByRhythmic(rhythmic, limit.toLong()).executeAsList().map { it.toPoem() }
        }

    suspend fun search(query: String, limit: Int = 100): List<Poem> =
        withContext(Dispatchers.Default) {
            q.search(query, query, query, limit.toLong()).executeAsList().map { it.toPoem() }
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
        private val DYNASTY_ORDER = listOf("唐", "五代", "北宋", "南宋", "金", "元", "明", "清")
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

private fun com.songci.app.data.db.Search.toPoem() =
    Poem(id, rhythmic, content, author_id, author_name)
