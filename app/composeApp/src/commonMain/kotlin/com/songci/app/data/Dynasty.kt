package com.songci.app.data

/**
 * 朝代映射(author_id -> 朝代)+ 年份证据(横线年份区间),由 app/data/tools/dynasty.py 生成。
 * 数据来源为 authors.long_desc 的文本推导(关键词/年号/年份),无法判定的作者兜底「宋」
 * (全宋词语料上位类,无「未知」);证据仅供作者行展示,便于用户反馈错分。
 */
class Dynasty internal constructor(
    private val map: Map<Long, String>,
    private val evidence: Map<Long, String>,
) {

    fun of(authorId: Long?): String = authorId?.let { map[it] } ?: UNKNOWN

    /** 分类证据年份(如 "1037-1101"),无则 null。 */
    fun evidenceOf(authorId: Long?): String? = authorId?.let { evidence[it] }

    companion object {
        const val UNKNOWN = "未知"

        suspend fun load(): Dynasty {
            val map = parseMap(
                songci.composeapp.generated.resources.Res.readBytes("files/dynasty_map.json").decodeToString()
            )
            val evidence = parseMap(
                songci.composeapp.generated.resources.Res.readBytes("files/dynasty_evidence.json").decodeToString()
            )
            return Dynasty(map, evidence)
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
