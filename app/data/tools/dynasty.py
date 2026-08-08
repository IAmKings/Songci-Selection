#!/usr/bin/env python3
"""从 authors.long_desc 推导朝代,输出 dynasty_map.json (author_id -> 朝代)。

规则(按文本中首次出现的朝代线索为准):
1. 朝代关键词(北宋/南宋/五代/唐/金/元/清/明 等)
2. 宋朝年号映射(建炎/绍兴 -> 南宋;熙宁/庆历/宣和 -> 北宋)
3. 宋代皇帝庙号(宋太祖..宋钦宗 -> 北宋;宋高宗.. -> 南宋)
4. 无关键词时取首个 4 位年份:960-1126 -> 北宋,1127-1279 -> 南宋
5. 无法判定 -> 未知
"""
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]          # app/
DB = ROOT / ".." / "db" / "songci.db"
OUT = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "dynasty_map.json"

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


def derive(desc: str) -> str:
    if not desc or desc.strip() in ("", "--"):
        return "未知"
    best = (None, None)  # (index, dynasty)
    for dynasty, words in CLUES:
        for w in words:
            i = desc.find(w)
            if i >= 0 and (best[0] is None or i < best[0]):
                best = (i, dynasty)
    if best[1]:
        return best[1]
    years = re.findall(r"(?:约|公[元]?)?(\d{3,4})\s*[-–—~]", desc)
    if not years:
        years = re.findall(r"(\d{4})", desc)
    for y in years:
        year = int(y)
        if 960 <= year <= 1126:
            return "北宋"
        if 1127 <= year <= 1279:
            return "南宋"
    return "未知"


def main():
    con = sqlite3.connect(DB)
    rows = con.execute("SELECT id, long_desc FROM authors").fetchall()
    result = {str(aid): derive(desc) for aid, desc in rows}
    from collections import Counter
    counts = Counter(result.values())
    print(f"authors: {len(rows)}, 分布: {dict(counts)}")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=0) + "\n", encoding="utf-8")
    print(f"written: {OUT}")
    for name in ("苏轼", "李清照", "范仲淹", "辛弃疾", "温庭筠", "李煜", "巴谈"):
        row = con.execute("SELECT id FROM authors WHERE name=?", (name,)).fetchone()
        if row:
            print(f"  {name} -> {result[str(row[0])]}")


if __name__ == "__main__":
    main()
