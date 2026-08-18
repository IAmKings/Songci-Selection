package com.songci.app.data

/**
 * 词作内容分段:用格律段边界(segEnds)把按句分行的文本切成段(上下阕)。
 * 词作去标点字数与格律首体一致 → 按段边界逐句切分;否则兜底(行数对半/单段)。
 * 纯函数,详情页窄/宽双路径共用。
 */
object Segmenter {

    /** 去标点/空白(与 rhythmic_map.py 的 chars 一致)。 */
    fun chars(s: String): String =
        s.filter { !it.isWhitespace() && it !in "，。！？、；：·「」『』（）()…—～" }

    /** 词作 → 段句子列表(每段内保持原句含标点)。无格律或字数不符走兜底。 */
    fun segment(content: String, spec: RhythmicSpec?): List<List<String>> {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val s = spec ?: return fallback(lines)
        if (lines.sumOf { chars(it).length } != s.chars) return fallback(lines)
        if (s.segEnds.size <= 1) return listOf(lines)   // 单调(单段)

        // 累计句字符位置 → 段间边界(segEnds 排除全词末)映射到句子索引
        val lineLens = lines.map { chars(it).length }
        val targets = s.segEnds.dropLast(1)
        val segBreaks = targets.map { target ->
            var acc = 0
            var idx = 0
            while (idx < lineLens.size - 1 && acc + lineLens[idx] < target) {
                acc += lineLens[idx]; idx++
            }
            idx
        }
        // 切分:段边界在句末(包含该句);单调增去重
        val breaks = segBreaks.filter { it < lines.size - 1 }.distinct().sorted()
        if (breaks.isEmpty()) return listOf(lines)
        val result = mutableListOf<List<String>>()
        var start = 0
        breaks.forEach { end ->
            result.add(lines.subList(start, end + 1))
            start = end + 1
        }
        result.add(lines.subList(start, lines.size))
        return result
    }

    /** 兜底:行数 ≥4 按行数对半(近似),否则单段。 */
    private fun fallback(lines: List<String>): List<List<String>> {
        if (lines.size < 4) return listOf(lines)
        val mid = lines.size / 2
        return listOf(lines.take(mid), lines.drop(mid))
    }
}

/** 竖排单列块:标点优先截断后的一列;ownerStanza 用于跨阕空列,chars 为该列逐字。 */
data class VerticalColumn(val ownerStanza: Int, val chars: List<Char>)

/**
 * 竖排按列高截断:以"句子"为硬性一列单元——每个句子独占一列,绝不合并。
 * 短句(≤maxChars)直接成列;超长句(>maxChars)内部按标点贪心拆段为连续多列(段间向左续)。
 * 跨阕交界保留 ownerStanza 变化以插入空列。
 */
fun splitVerticalColumns(segments: List<List<String>>, maxChars: Int): List<VerticalColumn> {
    if (maxChars <= 0) return emptyList()
    val punct = "，。、；：·！？…"
    val columns = mutableListOf<VerticalColumn>()
    segments.forEachIndexed { stanza, lines ->
        lines.forEach { line ->
            if (line.length > maxChars) {
                // 超长句:内部按标点贪心拆段,每段独立列,段间连续
                val chars = line.toList()
                var i = 0
                while (i < chars.size) {
                    val window = chars.subList(i, minOf(i + maxChars, chars.size))
                    val cut = findPunctCut(window, punct)
                    val take = if (cut.first.isNotEmpty()) cut.first.size
                               else (i + maxChars).coerceAtMost(chars.size) - i   // 无标点兜底整窗
                    columns.add(VerticalColumn(stanza, chars.subList(i, i + take)))
                    i += take
                }
            } else {
                // 短句:独占一列,不与其他句合并(句号分列)
                columns.add(VerticalColumn(stanza, line.toList()))
            }
        }
    }
    return columns
}

/** 在缓冲字符中寻找最接近末尾的标点作为截断点,避免在词中硬切。返回(截断前, 剩余)。 */
private fun findPunctCut(buf: List<Char>, punct: String): Pair<List<Char>, List<Char>> {
    for (i in buf.indices.reversed()) {
        if (buf[i] in punct) return buf.subList(0, i + 1) to buf.subList(i + 1, buf.size)
    }
    return buf to emptyList()   // 无标点,整段截断
}
