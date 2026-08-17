package com.songci.app.data

const val DB_FILE_NAME = "songci.db"

data class Poem(
    val id: Long,
    val rhythmic: String,
    val content: String,
    val authorId: Long?,
    val authorName: String,
)

data class Author(
    val id: Long,
    val name: String,
    val longDesc: String = "",   // 作者简介(生卒/字号/籍贯,db long_desc),无则空
    val head: String = "#",      // 拼音首字母分组: 0/A-X/#(数据层预计算)
)

/** 词牌索引条目(拼音分组,数据层预计算)。 */
data class RhythmicIndex(
    val rhythmic: String,
    val head: String,
)

/**
 * 搜索结果匹配标记:指明命中的字段与高亮子串(UI 层用背景色标注)。
 * - 中文 LIKE:命中子串(如 query 出现在词牌/作者/内容中)
 * - 拼音缩写/异名展开:命中整字段(词牌全名/作者全名)
 */
data class SearchMatch(
    val poem: Poem,
    val rhythmicHighlight: String? = null,
    val authorHighlight: String? = null,
    val contentHighlight: String? = null,
)

data class Favorite(
    val poem: Poem,
    val createdAt: String,
)
