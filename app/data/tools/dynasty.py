#!/usr/bin/env python3
"""从 authors.long_desc 推导朝代,输出 dynasty_map.json (author_id -> 朝代)。

规则(按文本中首次出现的朝代线索为准):
1. 朝代关键词(北宋/南宋/五代/唐/金/元/清/明 等)
2. 宋朝年号映射(建炎/绍兴 -> 南宋;熙宁/庆历/宣和 -> 北宋)
3. 宋代皇帝庙号(宋太祖..宋钦宗 -> 北宋;宋高宗.. -> 南宋)
4. 策展表(CURATED):无年份无关键词的知名作者(花间/南唐词人,如李煜/和凝/薛昭蕴)
5. 关键词为北宋/南宋 但生卒年不跨入北宋(卒年 ≤ 960,如"宋初《钓矶立谈》评其"出自
   后世引文的南唐冯延巳 903-960) -> 按生年改判;生卒明确跨入北宋(如王禹偁 954-1001)保留关键词
6. 无关键词时取首个可信年份(≥700):<900 唐,900-959 五代,960-1126 北宋,1127-1279 南宋
7. 其余(含无年份) -> 宋(全宋词语料作者兜底上位类)
"""
import json
import re
import sqlite3
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]          # app/
DB = ROOT / ".." / "db" / "songci.db"
OUT = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "dynasty_map.json"
OUT_EV = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "dynasty_evidence.json"

# 无年份无关键词的知名作者人工策展(花间/南唐词人,数据源 long_desc 缺失)
CURATED = {
    "李煜": "五代",
    "和凝": "五代",
    "薛昭蕴": "五代",
}

# (朝代, 线索列表) —— 线索按文本出现位置比较,列表顺序仅作同位置 tie-break
CLUES = [
    ("北宋", [
        "北宋", "宋初", "赵宋初",
        "建隆", "乾德", "开宝", "太平兴国", "雍熙", "端拱", "淳化", "至道",
        "咸平", "景德", "大中祥符", "天禧", "乾兴", "天圣", "明道", "景祐",
        "宝元", "康定", "庆历", "皇祐", "至和", "嘉祐", "治平", "熙宁", "元丰",
        "元祐", "绍圣", "元符", "建中靖国", "崇宁", "大观", "政和", "宣和", "靖康",
        "宋太祖", "宋太宗", "宋真宗", "宋仁宗", "宋英宗", "宋神宗", "宋哲宗", "宋徽宗", "宋钦宗",
    ]),
    ("南宋", [
        "南宋", "南渡", "宋南渡后",
        "建炎", "绍兴", "隆兴", "乾道", "淳熙", "绍熙", "庆元", "嘉泰", "开禧",
        "嘉定", "宝庆", "绍定", "端平", "嘉熙", "淳祐", "宝祐", "开庆", "景定",
        "咸淳", "德祐", "德佑", "景炎", "祥兴",
        "宋高宗", "宋孝宗", "宋光宗", "宋宁宗", "宋理宗", "宋度宗", "宋端宗", "宋恭帝",
    ]),
    ("五代", ["五代", "南唐", "后唐", "后晋", "后汉", "后周", "吴越", "前蜀", "后蜀", "闽国"]),
    ("唐", ["唐代", "唐朝", "盛唐", "晚唐", "初唐", "中唐"]),
    ("金", ["金代", "金朝", "金末", "金元"]),
    ("元", ["元代", "元朝", "入元", "元末"]),
    ("明", ["明代", "明朝"]),
    ("清", ["清代", "清朝"]),
]


# 词人年代下限为李白(701),更早的年份必为数据噪声
# (如唐珏条"（247―？）"残缺年份、吴淑姬条《永乐大典》"卷808"卷号)
MIN_YEAR = 700

# 年份线索:横线前后各取(须保留双侧收集 —— 单侧区间如 "960―?" 也有真实年份)
_YEAR_RE = re.compile(r"(?:约|公[元]?)?(\d{3,4})\s*[-–—~―]")
_YEAR_BEFORE_RE = re.compile(r"[-–—~―]\s*(?:约|公[元]?)?(\d{3,4})")
_PLAIN_YEAR_RE = re.compile(r"(\d{3,4})")


def first_years(desc: str, dashed_only: bool = False) -> list[int]:
    """可信年份(≥MIN_YEAR)升序。dashed_only:仅横线连接的年份(生卒年区间),供关键词改判用。"""
    years = [int(y) for y in _YEAR_RE.findall(desc)] + \
            [int(y) for y in _YEAR_BEFORE_RE.findall(desc)]
    if not years and not dashed_only:
        years = [int(y) for y in _PLAIN_YEAR_RE.findall(desc)]
    return sorted({y for y in years if y >= MIN_YEAR})


def by_year(year: int) -> str:
    if year < 900:
        return "唐"
    if year < 960:
        return "五代"
    if year <= 1126:
        return "北宋"
    if year <= 1279:
        return "南宋"
    return "宋"


def derive(name: str, desc: str) -> str:
    if name in CURATED:
        return CURATED[name]
    if not desc or desc.strip() in ("", "--"):
        return "宋"
    best = (None, None)  # (index, dynasty)
    for dynasty, words in CLUES:
        for w in words:
            i = desc.find(w)
            if i >= 0 and (best[0] is None or i < best[0]):
                best = (i, dynasty)
    if best[1]:
        # 关键词给出宋内朝代,但生卒年不跨入北宋(卒年 ≤ 960,如"宋初"出自后世引文的
        # 南唐冯延巳 903-960) -> 按生年改判;生卒跨入北宋(如王禹偁 954-1001)则保留关键词。
        # 仅用横线连接的年份,避免《永乐大典》"卷808"等卷号误作年份。
        years = first_years(desc, dashed_only=True)
        if best[1] in ("北宋", "南宋") and years and years[0] < 960 \
                and (len(years) < 2 or years[-1] <= 960):
            return by_year(years[0])
        return best[1]
    years = first_years(desc)
    if years:
        return by_year(years[0])
    return "宋"  # 全宋词语料作者兜底:宋代(上位类)


def evidence(desc: str) -> str:
    """横线连接的年份区间(作者行展示用,如 "1037-1101");无则空。"""
    years = first_years(desc, dashed_only=True)
    if not years:
        return ""
    return f"{years[0]}-{years[-1]}" if len(years) >= 2 else str(years[0])


def main():
    con = sqlite3.connect(DB)
    rows = con.execute("SELECT id, name, long_desc FROM authors").fetchall()
    result = {str(aid): derive(name, desc) for aid, name, desc in rows}
    ev = {str(aid): v for aid, name, desc in rows if (v := evidence(desc))}   # 无证据作者不输出
    counts = Counter(result.values())
    print(f"authors: {len(rows)}, 分布: {dict(counts)}")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=0) + "\n", encoding="utf-8")
    OUT_EV.write_text(json.dumps(ev, ensure_ascii=False, indent=0) + "\n", encoding="utf-8")
    print(f"written: {OUT}")
    print(f"written: {OUT_EV} (有年份证据作者: {sum(1 for v in ev.values() if v)})")
    for name in ("苏轼", "李清照", "范仲淹", "辛弃疾", "温庭筠", "李煜", "巴谈"):
        row = con.execute("SELECT id FROM authors WHERE name=?", (name,)).fetchone()
        if row:
            print(f"  {name} -> {result[str(row[0])]}")


if __name__ == "__main__":
    main()
