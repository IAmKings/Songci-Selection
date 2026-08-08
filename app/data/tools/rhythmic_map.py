#!/usr/bin/env python3
"""词牌名清洗 + 钦定词谱格律映射,输出 rhythmic_map.json + unmapped_rhythmics.json。

流水线(与 dynasty.py 同模式,生成物不入 git):
1. 清洗: `A·B` 取主词牌、引号词题剥离、⿰ 前缀剥离、全角空格规范化
2. 映射: 清洗名直配 Ci_Tunes.json 键;未命中再查 desc 别名索引(「词名《X》」「亦名《X》」)
3. 未映射分类: placeholder(含⿰,交还原专项) / alias-mismatch(形近异写,人工策展) / missing-in-source
4. 输出: rhythmic_map.json(格律数据)+ unmapped_rhythmics.json(活档案,还原后重跑消解)

用法: python3 rhythmic_map.py [--source /path/to/Ci_Tunes.json]
数据源: git clone --depth 1 https://github.com/charlesix59/chinese_word_rhyme /tmp/cwr
"""
import argparse
import difflib
import json
import re
import sqlite3
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]          # app/
DB = ROOT / ".." / "db" / "songci.db"
OUT_MAP = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "rhythmic_map.json"
OUT_UNMAPPED = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "unmapped_rhythmics.json"
DEFAULT_SOURCE = Path("/tmp/cwr/data/Ci_Tunes.json")

# 清洗规则: 引号词题 → 剥离; `A·B` 惯例为「别名·正名」取 B,但存在反向(B 不命中回退 A);
# ⿰ 占位符剥离(原字由还原专项负责); 全角空格 → 去
QUOTED = re.compile(r'^["“」][^"“」]*["”」]')
def clean(rhythmic: str) -> str:
    s = rhythmic.strip().replace("　", "").replace(" ", "")
    s = QUOTED.sub("", s)
    parts = s.split("·")
    s = parts[-1] if len(parts) > 1 else s
    s = s.replace("⿰", "")
    return s.strip()

def clean_with_fallback(rhythmic: str, specs_keys: set) -> str:
    """清洗; `A·B` 且 B 清洗后不命中数据源时回退取 A(11 条反向条目,如 一剪梅·一翦梅)。"""
    s = rhythmic.strip().replace("　", "").replace(" ", "")
    s = QUOTED.sub("", s)
    parts = s.split("·")
    if len(parts) > 1:
        b = parts[-1].replace("⿰", "").strip()
        if b in specs_keys:
            return b
        a = parts[0].replace("⿰", "").strip()
        return a if a in specs_keys else b
    return s.replace("⿰", "").strip()

# 别名提取: 「蔡伸词名《苍梧谣》」「袁去华词亦名《归字谣》」;排除书名/作品名语境(无「名」前置)
ALIAS_RE = re.compile(r'(?:词)?(?:亦)?名《([^》]{1,8})》')

def build_alias_index(specs: dict) -> dict:
    alias = {}
    for name, entry in specs.items():
        for a in ALIAS_RE.findall(entry.get("desc") or ""):
            if a and a != name and a not in alias:
                alias[a] = name
    return alias

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    args = parser.parse_args()
    if not args.source.exists():
        raise SystemExit(f"数据源缺失: {args.source}\n请先: git clone --depth 1 "
                         "https://github.com/charlesix59/chinese_word_rhyme /tmp/cwr")
    specs = json.loads(args.source.read_text(encoding="utf-8"))
    specs_keys = set(specs)
    alias = build_alias_index(specs)
    print(f"数据源: {len(specs)} 调, 别名索引 {len(alias)} 条")

    conn = sqlite3.connect(DB)
    rows = conn.execute("SELECT rhythmic, COUNT(*) FROM poems GROUP BY rhythmic")
    rhythmics = {r: n for r, n in rows}

    # 清洗 + 映射
    mapped = {}          # 原始词牌名 -> (主词牌, 来源)
    unmapped = []
    cleaned_count = Counter()
    for raw, n in rhythmics.items():
        c = clean_with_fallback(raw, specs_keys)
        if c in specs:
            mapped[raw] = (c, "direct")
        elif c in alias:
            mapped[raw] = (alias[c], "alias")
        else:
            if "⿰" in raw:
                cat = "placeholder"
            elif any(difflib.SequenceMatcher(None, c, k).ratio() >= 0.8 for k in specs):
                cat = "alias-mismatch"
            else:
                cat = "missing-in-source"
            unmapped.append({"rhythmic": raw, "cleaned": c, "category": cat,
                             "poems": n, "status": "open"})

    # rhythmic_map.json: 扁平摘要 "词牌名" -> "sketch|chars|forms|tune|rhythm_codes|segments"
    # (对齐 dynasty_map.json 极简解析风格;rhythm 标记转单字符 -/J(句)/Y(韵);
    #  segments = 段末字索引(斜杠分隔,含全词末),由 shift 原始标记按段数推断真实边界。
    #  注: 早期 dict 字段 spec/source/desc 已弃用——词牌名 key 即映射目标,desc 无消费者)
    # 叶=叶韵/叠=叠韵/换=换韵,均为押韵标记,与「韵」同显下划线
    RHYTHM_CODE = {"": "-", "句": "J", "韵": "Y", "叶": "Y", "叠": "Y", "换": "Y"}

    def segment_ends(tunes: list) -> list:
        """段末字索引。shift 原始标记含段内韵脚(满江红 4 shift 实为 2 段),
        按 sketch 段数(单调/双调/三叠)选最接近等分点的 shift 作真实边界。"""
        shifts = [i for i, x in enumerate(tunes) if x.get("shift")]
        m = re.match(r"(单调|双调|三叠|四叠|双叠|三段|四段|单段)", f0["sketch"])
        n_seg = {"单调": 1, "双调": 2, "三叠": 3, "双叠": 2, "四叠": 4,
                 "三段": 3, "四段": 4, "单段": 1}.get(m.group(1) if m else "", 2)
        if not shifts:
            return [len(tunes) - 1]
        if n_seg == 1:
            return [shifts[-1]]
        # 贪心: 依序选最接近等分点的 shift 作段间边界, 末段结尾取最后 shift
        ends = []
        for k in range(1, n_seg):
            target = len(tunes) * k / n_seg
            best = min(shifts, key=lambda s: abs(s - target))
            ends.append(best)
        ends.append(shifts[-1])
        return sorted(set(ends))

    map_data = {}
    for raw, (spec_name, src) in mapped.items():
        entry = specs[spec_name]
        f0 = entry["formats"][0]
        tune_seq = "".join(x["tune"] for x in f0["tunes"])
        rhythm = "".join(RHYTHM_CODE.get(x.get("rhythm", ""), "-") for x in f0["tunes"])
        segs = "/".join(str(i) for i in segment_ends(f0["tunes"]))   # 斜杠分隔:JSON 条目以逗号拆分
        map_data[raw] = f"{f0['sketch']}|{len(f0['tunes'])}|{len(entry['formats'])}|{tune_seq}|{rhythm}|{segs}"
    OUT_MAP.parent.mkdir(parents=True, exist_ok=True)
    OUT_MAP.write_text(json.dumps(map_data, ensure_ascii=False) + "\n", encoding="utf-8")
    OUT_UNMAPPED.write_text(json.dumps(unmapped, ensure_ascii=False) + "\n", encoding="utf-8")

    # 覆盖报告
    total = len(rhythmics)
    n_mapped = len(mapped)
    poems_mapped = sum(rhythmics[raw] for raw in mapped)
    total_poems = sum(rhythmics.values())
    cats = Counter(u["category"] for u in unmapped)
    print(f"词牌 {total} | 映射 {n_mapped} ({n_mapped/total*100:.1f}%) | "
          f"词作覆盖 {poems_mapped}/{total_poems} ({poems_mapped/total_poems*100:.1f}%)")
    print(f"未映射 {len(unmapped)}: " + ", ".join(f"{c} {n}" for c, n in cats.most_common()))
    print(f"写入: {OUT_MAP.name} + {OUT_UNMAPPED.name}")

if __name__ == "__main__":
    main()
