#!/usr/bin/env python3
"""
一次性生成 data/pinyin_map.json:词库唯一汉字 → 无声调全拼映射。

用法(需 venv 安装 pypinyin):
    python3 -m venv /tmp/pyenv && /tmp/pyenv/bin/pip install pypinyin
    /tmp/pyenv/bin/python scripts/gen_pinyin_map.py

输出为纯数据资产,提交入库;运行时(dep build.py)只读该文件,零依赖。
来源:pypinyin 0.55.0(词典基于《新华字典》等权威拼音数据);多音字校正见 OVERRIDES。
"""
import json
import sqlite3
import sys

from pypinyin import Style, lazy_pinyin

ROOT = "/Users/pauldeman/Documents/ai_workspace/songci"
DB = f"{ROOT}/db/songci.db"
OUT = f"{ROOT}/data/pinyin_map.json"

# 多音字校正:词牌/作者名上下文读音(覆盖 pypinyin 默认单字音)
# 格式: {字: 全拼} —— 按词库实际语境人工核对(2026-08-17)
OVERRIDES = {
    "乐": "yue",      # 词牌「乐章集」「醉太平」作者「乐雷发」;姓氏/词牌读音 yuè
    "长": "chang",    # 词牌「长相思」「长亭怨慢」(cháng);非 zhǎng
    "重": "chong",    # 词牌「重叠金」「阳台重」常见 chóng
    "还": "huan",     # 词牌「还京乐」(huán)
    "朝": "chao",     # 词牌「朝中措」「朝天子」(cháo);非 zhāo(拂晓义)
    "曲": "qu",       # 词牌「曲江秋」等曲调义 qǔ;「九曲」qū 少见
    "行": "xing",     # 词牌「行香子」「少年游·行」;姓氏 xíng
    "单": "dan",      # 词牌「单州」「单于」dan;姓氏 shàn 少见
    "调": "diao",     # 词牌「水调歌头」(diào)
    "占": "zhan",     # 词牌「占春芳」zhān
    "降": "jiang",    # 词牌「降仙舞」jiàng
    "解": "jie",      # 词牌「解连环」「解语花」jiě
    "系": "xi",       # 词牌「系裙腰」xì
    "教": "jiao",     # 词牌「教池回」jiào
    "卷": "juan",     # 词牌「卷珠帘」juǎn
    "分": "fen",      # 词牌「八声甘州·分韵」fēn
    "度": "du",       # 词牌「渡江云·度」dù
    "数": "shu",      # 词牌「数花风」shù
    "更": "geng",     # 词牌「更漏子」(gēng)
    "横": "heng",     # 词牌「横塘路」héng
    "兴": "xing",     # 词牌「中兴乐」xīng
    "应": "ying",     # 词牌「应天长」(yìng)
    "弹": "tan",      # 词牌「弹人娇」tán
    "梳": "shu",      # 词牌「梳洗罢」
    "藏": "cang",     # 词牌「藏春坞」cáng
    "传": "chuan",    # 词牌「传言玉女」chuán
    "间": "jian",     # 词牌「人间词话」jiān
    "强": "qiang",    # 词牌「强村」qiáng
    "结": "jie",      # 词牌「结带巾」jié
    "着": "zhuo",     # 「着意」zhuó
    "相": "xiang",    # 词牌「相见欢」(xiāng)
    "将": "jiang",    # 词牌「将进酒」jiāng
    "塞": "sai",      # 词牌「塞孤」sài
    "难": "nan",      # 词牌「难唤」nán
    "干": "gan",      # 词牌「干荷叶」gān
    "舍": "she",      # 词牌「舍利」shè
    "发": "fa",       # 作者「乐雷发」fā
    "区": "ou",       # 姓氏「区」ōu
    "曾": "zeng",     # 作者「曾巩」zēng
    "华": "hua",      # 词牌「华胥引」huá
    "和": "he",       # 词牌「和州」hé
    "得": "de",       # 「得」dé
    "石": "shi",      # 词牌「石州慢」shí
    "看": "kan",      # 词牌「看花回」kàn
    "空": "kong",     # 词牌「空相忆」kōng
    "中": "zhong",    # 词牌「中兴乐」zhōng
}


def cjk_chars(text: str) -> list[str]:
    """提取 CJK 统一表意文字(含扩展 A/B;不含假名/全角符号)。"""
    out = []
    for ch in text:
        o = ord(ch)
        if 0x4E00 <= o <= 0x9FFF or 0x3400 <= o <= 0x4DBF or 0x20000 <= o <= 0x2A6DF:
            out.append(ch)
    return out


def main() -> None:
    con = sqlite3.connect(DB)
    cur = con.cursor()
    chars: set[str] = set()
    cur.execute("SELECT name FROM authors")
    for (name,) in cur.fetchall():
        chars.update(cjk_chars(name))
    cur.execute("SELECT rhythmic, content FROM poems")
    for rhy, content in cur.fetchall():
        chars.update(cjk_chars(rhy))
        chars.update(cjk_chars(content))
    con.close()

    result: dict[str, str] = {}
    missing: list[str] = []
    for ch in sorted(chars):
        if ch in OVERRIDES:
            result[ch] = OVERRIDES[ch]
            continue
        py = lazy_pinyin(ch, style=Style.NORMAL, errors="ignore")
        if py and py[0]:
            result[ch] = py[0]
        else:
            missing.append(ch)

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, sort_keys=True, indent=1)
    print(f"唯一汉字 {len(chars)} -> 已映射 {len(result)}, 缺映射 {len(missing)}")
    if missing:
        print("缺映射:", "".join(missing))
    print(f"输出: {OUT} ({len(json.dumps(result, ensure_ascii=False)) // 1024} KB)")


if __name__ == "__main__":
    sys.exit(main())
