#!/usr/bin/env python3
"""⿰ 还原回填:读人工还原 CSV,写回 data/ci.json,校验 ⿰ 清零。

CSV 格式(UTF-8, 含逗号/换行字段用引号包裹, python csv 模块):
  index,content           # 词作还原:index=RECORDS 下标, content=还原后全文
  (词牌名还原:6/7 已被清洗归并消解,纯占位 1 条见 restore_manifest rhythmic_cleanup 人工处理)

用法:
  python3 restore.py restore.csv            # 回填并备份 ci.json
  python3 restore.py --dry-run restore.csv  # 只校验不写回
回填后重建: python3 ../db/build.py && python3 prepare_db.py
"""
import argparse
import csv
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]          # app/
CI = ROOT / ".." / "data" / "ci.json"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("csv_path", type=Path, help="还原 CSV(index,content)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    records = json.loads(CI.read_text(encoding="utf-8"))["RECORDS"]
    rows = list(csv.reader(args.csv_path.open(encoding="utf-8")))
    if not rows or rows[0][:2] != ["index", "content"]:
        raise SystemExit("CSV 首行必须为表头: index,content (或 index,rhythmic)")
    data_rows = [r for r in rows[1:] if r and r[0].strip()]

    fixed = 0
    for row in data_rows:
        idx, field = int(row[0].strip()), row[1]
        if not (0 <= idx < len(records)):
            raise SystemExit(f"index 越界: {idx}")
        if "⿰" in field:
            raise SystemExit(f"index {idx} 还原后仍含 ⿰,拒绝写入")
        old = records[idx]["content"]
        if old != field:
            records[idx]["content"] = field
            fixed += 1

    remaining = sum(r["content"].count("⿰") for r in records)
    print(f"回填 {fixed} 首 | 剩余 ⿰ {remaining} 处")
    if args.dry_run:
        print("dry-run: 未写回")
        return
    if fixed:
        backup = CI.with_suffix(".json.bak")
        shutil.copyfile(CI, backup)
        CI.write_text(json.dumps({"RECORDS": records}, ensure_ascii=False, indent=4), encoding="utf-8")
        print(f"已写回 ci.json(备份 {backup.name});请重建: cd .. && python3 db/build.py && cd app/data/tools && python3 prepare_db.py")

if __name__ == "__main__":
    main()
