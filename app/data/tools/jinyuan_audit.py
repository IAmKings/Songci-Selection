#!/usr/bin/env python3
"""金元补全审计:97 位无词作作者分类 + 异体重复候选,输出 jinyuan_audit.json。

分类(按 desc 证据,自上而下):
  jinyuan   金/元词人(已知名单 + desc 强关键词)
  wudai     唐五代/花间/南唐(语料缺失,超出金元范围仅归类)
  variant   名录异体重复(同人两名,合并候选,自动检测 + 人工确认)
  stub      残缺名录行(单字名/残缺名,名录质量问题)
  other     其余
"""
import json
import sqlite3
from difflib import SequenceMatcher
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / ".." / "db" / "songci.db"
OUT = ROOT / ".." / "data" / "jinyuan_audit.json"

# 已知金元词人(全金元词名录核心)
GOLD = {
    "元好问", "元问好", "辛愿", "段克己", "段成己", "吴激", "党怀英", "元德明",
    "刘著", "李俊民", "王庭筠", "蔡松年", "赵秉文", "王若虚", "王寂", "王渥",
    "张弘范", "刘秉忠", "耶律楚材", "白朴", "许古", "赵可", "邓千江", "刘迎",
}
JY_EVIDENCE = ("金时", "金末", "金代", "金朝", "元初", "元代", "元朝", "入元", "金元", "辽金", "金泰和")
WUDAI_EVIDENCE = ("五代", "南唐", "花间", "后蜀", "前蜀", "吴越", "唐末", "西蜀")

def classify(name: str, desc: str) -> str:
    if name in GOLD:
        return "jinyuan"
    d = desc or ""
    if any(k in d for k in JY_EVIDENCE):
        return "jinyuan"
    if any(k in d for k in WUDAI_EVIDENCE):
        return "wudai"
    if len(name) == 1:
        return "stub"
    return "other"

def main():
    conn = sqlite3.connect(DB)
    no_poems = conn.execute(
        "SELECT a.id, a.name, a.long_desc FROM authors a "
        "WHERE NOT EXISTS (SELECT 1 FROM poems p WHERE p.author_id = a.id)").fetchall()
    all_names = [r[0] for r in conn.execute("SELECT name FROM authors")]

    # 异体候选:无词作名录名 vs 全名录(排除自身,限 2-5 字,ratio≥0.8 或互为包含)
    variant_cands = []
    for _, name, _ in no_poems:
        if len(name) < 2:
            continue
        for other in all_names:
            if name == other or len(other) < 2:
                continue
            if (name in other or other in name) or SequenceMatcher(None, name, other).ratio() >= 0.8:
                variant_cands.append([name, other])
    # 去重镜像 + 排序
    seen = set()
    variants = []
    for a, b in sorted(variant_cands):
        key = tuple(sorted((a, b)))
        if key not in seen:
            seen.add(key)
            variants.append({"name": a, "candidate": b})

    grouped = {}
    for i, n, d in no_poems:
        grouped.setdefault(classify(n, d or ""), []).append({
            "id": i, "name": n, "desc": (d or "")[:50]})
    audit = {
        "total_no_poems": len(no_poems),
        "by_category": {k: sorted(v, key=lambda x: x["name"]) for k, v in grouped.items()},
        "variant_candidates": variants,
    }
    OUT.write_text(json.dumps(audit, ensure_ascii=False, indent=1), encoding="utf-8")
    for k, v in grouped.items():
        print(f"[{k}] {len(v)} 人: {', '.join(x['name'] for x in v[:10])}")
    print(f"\n异体候选 {len(variants)} 对: " +
          ", ".join(f"{v['name']}~{v['candidate']}" for v in variants[:12]))
    print(f"写入: {OUT.name}")

if __name__ == "__main__":
    main()
