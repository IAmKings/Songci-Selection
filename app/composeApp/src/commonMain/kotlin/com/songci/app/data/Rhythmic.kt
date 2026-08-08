package com.songci.app.data

/**
 * 词牌格律(句式摘要 + 逐字平仄谱),由 app/data/tools/rhythmic_map.py 生成,
 * 数据源为钦定词谱系 Ci_Tunes.json(818 调)。未映射词牌(未映射清单)返回 null。
 */
class Rhythmic internal constructor(
    private val map: Map<String, String>,
    private val bodies: Map<String, String> = emptyMap(),
) {
    // 预索引(load 时解析一次): spec → 同调词牌名列表; 别名 → spec
    private val specToKeys: Map<String, List<String>>
    private val aliasToSpec: Map<String, String>

    init {
        val s2k = mutableMapOf<String, MutableList<String>>()
        val a2s = mutableMapOf<String, String>()
        map.forEach { (name, value) ->
            val spec = parseSpec(value) ?: return@forEach
            s2k.getOrPut(spec.spec) { mutableListOf() }.add(name)
            spec.aliases.forEach { a2s.getOrPut(it) { spec.spec } }   // 首个条目优先(原生无 putIfAbsent)
        }
        specToKeys = s2k.mapValues { it.value.sorted() }
        aliasToSpec = a2s
    }

    /** 格律摘要:sketch/chars/forms/tune(平仄串)/rhythm(标记串),无则 null。 */
    fun of(rhythmic: String): RhythmicSpec? = map[rhythmic]?.let(::parseSpec)

    /** 全部体(含首体),无则空。 */
    fun bodiesOf(rhythmic: String): List<RhythmicSpec> =
        bodies[rhythmic]?.split(";")?.mapNotNull { parseBody(it) } ?: emptyList()

    /** 按字数匹配体(分段用):体序即数据源顺序,首体优先;无匹配 null。 */
    fun matchBody(rhythmic: String, charCount: Int): RhythmicSpec? =
        bodiesOf(rhythmic).firstOrNull { it.chars == charCount }

    /** 异名展开:q(词牌名或别名) → 同调全部 db 词牌名(搜索用,预索引查表 O(1))。 */
    fun expand(q: String): List<String> {
        val qSpec = of(q)?.spec ?: aliasToSpec[q] ?: return emptyList()
        return specToKeys[qSpec] ?: emptyList()
    }

    companion object {
        const val FILE = "files/rhythmic_map.json"
        const val BODIES_FILE = "files/rhythmic_bodies.json"

        suspend fun load(): Rhythmic = Rhythmic(
            parseMap(songci.composeapp.generated.resources.Res.readBytes(FILE).decodeToString()),
            parseMap(songci.composeapp.generated.resources.Res.readBytes(BODIES_FILE).decodeToString()),
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
            if (parts.size != 8) return null
            val sketch = parts[0]; val chars = parts[1]; val forms = parts[2]
            val tune = parts[3]; val rhythm = parts[4]; val segs = parts[5]
            val aliases = parts[6]; val spec = parts[7]
            if (tune.length != rhythm.length) return null
            val segEnds = segs.split("/").mapNotNull { it.toIntOrNull() }
            if (segEnds.isEmpty() || segEnds.last() != tune.length - 1) return null
            return RhythmicSpec(sketch, chars.toIntOrNull() ?: 0, forms.toIntOrNull() ?: 0,
                tune, rhythm, segEnds, aliases.split("/").filter { it.isNotBlank() }, spec)
        }

        /** 多体条目:"sketch|chars|tune|rhythm|segs|author"(无 forms/aliases/spec)。 */
        internal fun parseBody(value: String): RhythmicSpec? {
            val parts = value.split("|")
            if (parts.size != 6) return null
            val sketch = parts[0]; val chars = parts[1]; val tune = parts[2]
            val rhythm = parts[3]; val segs = parts[4]; val author = parts[5]
            if (tune.length != rhythm.length) return null
            val segEnds = segs.split("/").mapNotNull { it.toIntOrNull() }
            if (segEnds.isEmpty() || segEnds.last() != tune.length - 1) return null
            return RhythmicSpec(sketch, chars.toIntOrNull() ?: 0, 0, tune, rhythm, segEnds,
                emptyList(), "", author)
        }
    }
}

/** 首体格律:句式摘要/字数/体数/平仄谱(逐字) + 标记(句/韵位置) + 段末字索引 + 异名列表 + 主词牌名 + 体作者。 */
data class RhythmicSpec(
    val sketch: String,
    val chars: Int,
    val forms: Int,
    val tune: String,
    val rhythm: String,   // 与 tune 等长: '-' 普通 / 'J' 句末 / 'Y' 韵脚
    val segEnds: List<Int>,   // 段末字索引,如 [20, 41](浣溪沙前段 21 字/后段 21 字)
    val aliases: List<String>,   // 异名(出塞/大江东去…),无则空
    val spec: String,   // 主词牌名(钦定词谱调名)
    val author: String = "",   // 体作者(毛滂体/苏轼体),多体条目有
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
