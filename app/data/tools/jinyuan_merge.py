#!/usr/bin/env python3
"""金元词人补全:仅补充名录内无词作作者的词作(不全量合并数据源)。

流程:
1. 名录无词作作者(97 人审计) ∩ 数据源金/元卷作者 → 目标集合
2. 每首解析词牌:title 含 `·` 取主词牌;否则与钦定词谱调名最长前缀匹配
3. 追加 ci.json(RECORDS 尾部,value 续号) → db/build.py 重建

用法: python3 jinyuan_merge.py [--source /path/to/poetry-source/source]
数据源: git clone --depth 1 --filter=blob:none --sparse https://github.com/snowtraces/poetry-source /tmp/ps
        (需 sparse-checkout 词/金 + 词/元)
"""
import argparse
import glob
import json
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / ".." / "db" / "songci.db"
CI = ROOT / ".." / "data" / "ci.json"
SPECS = Path("/tmp/cwr/data/Ci_Tunes.json")
DEFAULT_SRC = Path("/tmp/ps2/source/词")

def parse_rhythmic(title: str, spec_keys: set) -> str:
    """title → 词牌:「木兰花令·鹤儿…」取主词牌;粘连(「乌夜啼留别…」)最长前缀匹配。"""
    t = title.split("·")[0].strip()
    if t in spec_keys:
        return t
    best = ""
    for k in spec_keys:
        if len(k) > len(best) and t.startswith(k):
            best = k
    return best or title.split("·")[0].strip()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SRC)
    args = parser.parse_args()

    conn = sqlite3.connect(DB)
    no_poems = set(r[0] for r in conn.execute(
        "SELECT a.name FROM authors a WHERE NOT EXISTS "
        "(SELECT 1 FROM poems p WHERE p.author_id = a.id)"))
    spec_keys = set(json.loads(SPECS.read_text(encoding="utf-8")))
    print(f"名录无词作作者 {len(no_poems)} 人")

    # 数据源金/元词,按目标作者过滤
    target = []
    for dynasty in ("金", "元"):
        for f in glob.glob(str(args.source / dynasty / "ci.*.json")):
            if "base" in f or "pinyin" in f:
                continue
            for r in json.load(open(f, encoding="utf-8")):
                if r.get("authorName") in no_poems:
                    target.append(r)
    print(f"目标词作 {len(target)} 首, 作者 {len({r['authorName'] for r in target})} 人")

    # 追加 ci.json
    ci = json.loads(CI.read_text(encoding="utf-8"))
    records = ci["RECORDS"]
    next_value = max(int(r["value"]) for r in records) + 1
    added = 0
    for r in target:
        content = "\n".join(r["content"]) if isinstance(r["content"], list) else r["content"]
        if any(x["author"] == r["authorName"] and x["content"] == content
               for x in records):   # 去重(内容级)
            continue
        records.append({
            "value": str(next_value + added),
            "rhythmic": parse_rhythmic(r["title"], spec_keys),
            "author": r["authorName"],
            "content": content,
        })
        added += 1
    ci["RECORDS"] = records
    CI.write_text(json.dumps(ci, ensure_ascii=False, indent=4), encoding="utf-8")
    print(f"追加 {added} 首(去重后); ci.json 现 {len(records)} 首; 请重建: python3 db/build.py && cd app/data/tools && python3 prepare_db.py")

if __name__ == "__main__":
    main()
