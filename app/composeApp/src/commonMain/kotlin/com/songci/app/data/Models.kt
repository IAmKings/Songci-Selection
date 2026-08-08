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
)

data class Favorite(
    val poem: Poem,
    val createdAt: String,
)
