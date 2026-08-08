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
