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
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / ".." / "db" / "songci.db"
CI = ROOT / ".." / "data" / "ci.json"
SPECS = Path("/tmp/cwr/data/Ci_Tunes.json")
DEFAULT_SRC = Path("/tmp/ps2/source/词")

def build_alias_index(specs: dict) -> dict:
    """词牌别名(desc 中「词名《X》」「亦名《X》」),与大江东去→念奴娇 同源。"""
    alias = {}
    for name, entry in specs.items():
        for a in re.findall(r'(?:词)?(?:亦)?名《([^》]{1,8})》', entry.get("desc") or ""):
            if a and a != name and a not in alias:
                alias[a] = name
    return alias

def parse_rhythmic(title: str, spec_keys: set, alias: dict) -> str:
    """title → 词牌:「木兰花令·鹤儿…」取主词牌;粘连(「乌夜啼留别…」/「大江东去和答…」)
    最长前缀匹配,先主键后别名(大江东去→念奴娇、望月婆罗门引→婆罗门引)。"""
    t = title.split("·")[0].strip()
    if t in spec_keys:
        return t
    if t in alias:
        return alias[t]
    best, best_kind = "", None
    for k in spec_keys:
        if len(k) > len(best) and t.startswith(k):
            best, best_kind = k, "key"
    for k in alias:
        if len(k) > len(best) and t.startswith(k):
            best, best_kind = k, "alias"
    return alias[best] if best_kind == "alias" else best or title.split("·")[0].strip()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SRC)
    parser.add_argument("--refix", action="store_true",
                        help="只修正已追加记录(value>原库)的词牌名,不新增(合并后别名改进重跑)")
    args = parser.parse_args()

    conn = sqlite3.connect(DB)
    no_poems = set(r[0] for r in conn.execute(
        "SELECT a.name FROM authors a WHERE NOT EXISTS "
        "(SELECT 1 FROM poems p WHERE p.author_id = a.id)"))
    specs = json.loads(SPECS.read_text(encoding="utf-8"))
    spec_keys = set(specs)
    alias = build_alias_index(specs)
    print(f"名录无词作作者 {len(no_poems)} 人")

    ci = json.loads(CI.read_text(encoding="utf-8"))
    records = ci["RECORDS"]
    if args.refix:
        # 仅修正已追加记录(原库 value ≤ 21050 不动)的词牌名
        fixed = 0
        for r in records:
            if int(r["value"]) <= 21050:
                continue
            new_r = parse_rhythmic(r["rhythmic"], spec_keys, alias)
            if new_r != r["rhythmic"]:
                print(f"  {r['value']}: {r['rhythmic']} → {new_r}")
                r["rhythmic"] = new_r
                fixed += 1
        CI.write_text(json.dumps(ci, ensure_ascii=False, indent=4), encoding="utf-8")
        print(f"refix 修正 {fixed} 条; 请重建 db")
        return

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
    next_value = max(int(r["value"]) for r in records) + 1
    added = 0
    for r in target:
        content = "\n".join(r["content"]) if isinstance(r["content"], list) else r["content"]
        if any(x["author"] == r["authorName"] and x["content"] == content
               for x in records):   # 去重(内容级)
            continue
        records.append({
            "value": str(next_value + added),
            "rhythmic": parse_rhythmic(r["title"], spec_keys, alias),
            "author": r["authorName"],
            "content": content,
        })
        added += 1
    ci["RECORDS"] = records
    CI.write_text(json.dumps(ci, ensure_ascii=False, indent=4), encoding="utf-8")
    print(f"追加 {added} 首(去重后); ci.json 现 {len(records)} 首; 请重建: python3 db/build.py && cd app/data/tools && python3 prepare_db.py")

if __name__ == "__main__":
    main()
