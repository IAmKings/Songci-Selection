package com.songci.app.data

/**
 * 词牌格律(句式摘要 + 逐字平仄谱),由 app/data/tools/rhythmic_map.py 生成,
 * 数据源为钦定词谱系 Ci_Tunes.json(818 调)。未映射词牌(未映射清单)返回 null。
 */
class Rhythmic internal constructor(private val map: Map<String, String>) {

    /** 格律摘要:sketch/chars/forms/tune(平仄串)/rhythm(标记串),无则 null。 */
    fun of(rhythmic: String): RhythmicSpec? = map[rhythmic]?.let(::parseSpec)

    companion object {
        const val FILE = "files/rhythmic_map.json"

        suspend fun load(): Rhythmic = Rhythmic(
            parseMap(songci.composeapp.generated.resources.Res.readBytes(FILE).decodeToString())
        )

        /** 解析 {"词牌名":"sketch|chars|forms|tune|rhythm"} —— 对齐 Dynasty.parseMap 极简风格。 */
        internal fun parseMap(raw: String): Map<String, String> {
            val body = raw.trim().removePrefix("{").removeSuffix("}")
            if (body.isBlank()) return emptyMap()
            return body.split(",").mapNotNull { entry ->
                val parts = entry.trim().split(":", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                parts[0].trim().trim('"') to parts[1].trim().trim('"')
            }.toMap()
        }

        internal fun parseSpec(value: String): RhythmicSpec? {
            val parts = value.split("|")
            if (parts.size != 6) return null
            val sketch = parts[0]; val chars = parts[1]; val forms = parts[2]
            val tune = parts[3]; val rhythm = parts[4]; val segs = parts[5]
            if (tune.length != rhythm.length) return null
            val segEnds = segs.split("/").mapNotNull { it.toIntOrNull() }
            if (segEnds.isEmpty() || segEnds.last() != tune.length - 1) return null
            return RhythmicSpec(sketch, chars.toIntOrNull() ?: 0, forms.toIntOrNull() ?: 0,
                tune, rhythm, segEnds)
        }
    }
}

/** 首体格律:句式摘要/字数/体数/平仄谱(逐字) + 标记(句/韵位置) + 段末字索引(含全词末)。 */
data class RhythmicSpec(
    val sketch: String,
    val chars: Int,
    val forms: Int,
    val tune: String,
    val rhythm: String,   // 与 tune 等长: '-' 普通 / 'J' 句末 / 'Y' 韵脚
    val segEnds: List<Int>,   // 段末字索引,如 [20, 41](浣溪沙前段 21 字/后段 21 字)
) {
    /** 平仄谱按句切行(句末 J/韵 Y 换行),段边界(segEnds)处标记段尾。 */
    fun tuneLines(): List<TuneLine> {
        val lines = mutableListOf<TuneLine>()
        val current = mutableListOf<Pair<Char, Char>>()
        fun flush(segmentEnd: Boolean) {
            if (current.isNotEmpty()) {
                lines.add(TuneLine(current.toList(), segmentEnd))
                current.clear()
            }
        }
        tune.forEachIndexed { i, t ->
            current.add(t to rhythm[i])
            if (rhythm[i] == 'J' || rhythm[i] == 'Y' || i in segEnds) flush(i in segEnds)
        }
        flush(false)
        return lines
    }
}

/** 平仄谱一行:逐字 (tune, rhythm) 对;segmentEnd = 该行是段(阕)末。 */
data class TuneLine(val chars: List<Pair<Char, Char>>, val segmentEnd: Boolean)
