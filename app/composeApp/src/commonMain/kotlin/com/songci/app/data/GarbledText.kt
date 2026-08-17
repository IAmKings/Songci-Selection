package com.songci.app.data

/**
 * 乱码检测:编码损坏词句(非 CJK 字符混入)。
 *
 * 覆盖区间(2026-08-16 全库扫描确认 23 行命中,0 误伤):
 * - 日文平假名 \u3040-\u309F / 片假名 \u30A0-\u30FF / 半角片假名 \uFF66-\uFF9F
 * - 全角拉丁 \uFF21-\uFF5A / 全角数字 \uFF10-\uFF19
 * - 带圈字符 \u2460-\u24FF / 拉丁扩展 \u0080-\u024F / 注音符号 \u3100-\u312F
 *
 * 用途:随机入口(首页推荐/小组件/每日通知)过滤乱码词;详情页等主入口
 * 保持忠实显示(与 ⿰ 缺失字符同一原则,待权威源数据人工校对后清洗)。
 */
private val GARBLED_REGEX = Regex(
    "[" +
        "\u3040-\u30FF" +   // 平假名 + 片假名
        "\uFF66-\uFF9F" +   // 半角片假名
        "\uFF21-\uFF5A" +   // 全角拉丁字母
        "\uFF10-\uFF19" +   // 全角数字
        "\u2460-\u24FF" +   // 带圈字符
        "\u0080-\u024F" +   // 拉丁扩展
        "\u3100-\u312F" +   // 注音符号
        "]"
)

/** 文本是否含乱码字符(编码损坏)。 */
fun CharSequence.containsGarbled(): Boolean = GARBLED_REGEX.containsMatchIn(this)
