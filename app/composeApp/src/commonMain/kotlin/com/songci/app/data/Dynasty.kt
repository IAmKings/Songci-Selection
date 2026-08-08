package com.songci.app.data

/**
 * 朝代映射(author_id -> 朝代),由 app/data/tools/dynasty.py 生成。
 * 数据来源为 authors.long_desc 的文本推导,覆盖率约 15%,未覆盖作者归「未知」。
 */
class Dynasty internal constructor(private val map: Map<Long, String>) {

    fun of(authorId: Long?): String = authorId?.let { map[it] } ?: UNKNOWN

    companion object {
        const val UNKNOWN = "未知"

        suspend fun load(): Dynasty {
            val raw = songci.composeapp.generated.resources.Res.readBytes("files/dynasty_map.json")
                .decodeToString()
            val map = parseMap(raw)
            return Dynasty(map)
        }

        /** 解析自有格式 {"1":"北宋","2":"未知"} —— 键为数字,值为固定朝代标签。 */
        internal fun parseMap(raw: String): Map<Long, String> {
            val body = raw.trim().removePrefix("{").removeSuffix("}")
            if (body.isBlank()) return emptyMap()
            return body.split(",").mapNotNull { entry ->
                val parts = entry.trim().split(":", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val key = parts[0].trim().trim('"')
                val value = parts[1].trim().trim('"')
                key.toLongOrNull()?.let { it to value }
            }.toMap()
        }
    }
}
