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
)

data class Favorite(
    val poem: Poem,
    val createdAt: String,
)
