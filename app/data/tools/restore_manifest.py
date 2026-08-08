#!/usr/bin/env python3
"""生成 ⿰ 缺失字符待还原清单(restore_manifest.json),按作者聚合排序。

清单每条: {index, value, author, rhythmic, lines:[含⿰的句子], count}
index = ci.json RECORDS 列表下标(回填锚点);value = 原记录 value 字段(人工核对用)。
词牌名含 ⿰ 的单独列出(cleanup 字段),还原后重跑 rhythmic_map 消解。

用法: python3 restore_manifest.py [--out /path/manifest.json]
"""
import argparse
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]          # app/
CI = ROOT / ".." / "data" / "ci.json"
OUT = Path(__file__).resolve().parents[2] / ".." / "data" / "restore_manifest.json"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=OUT)
    args = parser.parse_args()
    records = json.loads(CI.read_text(encoding="utf-8"))["RECORDS"]

    by_author = defaultdict(list)
    total = 0
    for i, r in enumerate(records):
        content = r.get("content", "")
        n = content.count("⿰")
        if n == 0:
            continue
        total += n
        by_author[r.get("author", "无名氏")].append({
            "index": i, "value": r.get("value", ""), "rhythmic": r.get("rhythmic", ""),
            "lines": [ln.strip() for ln in content.split("\n") if "⿰" in ln],
            "count": n,
        })

    # 按作者词作数降序(高频作者优先还原)
    manifest = [
        {"author": a, "poems": len(items), "placeholders": sum(i["count"] for i in items),
         "items": items}
        for a, items in sorted(by_author.items(), key=lambda kv: -len(kv[1]))
    ]
    # 词牌名 ⿰(纯占位无主词牌)
    cleanup = sorted({r["rhythmic"] for r in records if "⿰" in r.get("rhythmic", "")})
    args.out.write_text(json.dumps({"total_placeholders": total, "total_poems": sum(
        len(m["items"]) for m in manifest), "by_author": manifest, "rhythmic_cleanup": cleanup},
        ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"⿰ 共 {total} 处 / {len(by_author)} 作者 / {sum(len(v) for v in by_author.values())} 首词")
    print(f"词牌名 ⿰: {cleanup}")
    print(f"写入: {args.out}")

if __name__ == "__main__":
    main()
