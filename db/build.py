#!/usr/bin/env python3
"""
宋词 SQLite 数据库构建脚本

从 data/ciauthor.json 和 data/ci.json 生成 db/songci.db。

核心数据表（每次重建）：
  - authors：诗人信息（含拼音首字母 pinyin_head / 全拼 pinyin_full,用于索引拼音排序）
  - poems：词作信息（含诗人外键）
  - poems_fts：FTS5 中文全文搜索索引
  - rhythmic_index：词牌拼音索引（归并 ⿰ 后主词牌 → 首字母/全拼）

应用数据表（首次创建，重建时保留不动）：
  - favorites：用户收藏（poem_id）

使用方式：
    cd db && python3 build.py

依赖：仅 Python 3 标准库（json, sqlite3）+ data/pinyin_map.json（拼音映射数据资产）
"""

import json
import sqlite3
import os
import time

# --- 配置 ---
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "..", "data")
AUTHOR_FILE = os.path.join(DATA_DIR, "ciauthor.json")
CI_FILE = os.path.join(DATA_DIR, "ci.json")
DB_FILE = os.path.join(BASE_DIR, "songci.db")

# 核心表（每次重建时 DROP + CREATE）
CORE_TABLES = ["authors", "poems", "poems_fts", "rhythmic_index"]


def load_json(path):
    """加载 JSON 文件，返回 RECORDS 列表。"""
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return data["RECORDS"]


def load_pinyin_map():
    """加载拼音映射数据资产（data/pinyin_map.json,由 scripts/gen_pinyin_map.py 生成）。"""
    path = os.path.join(DATA_DIR, "pinyin_map.json")
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def pinyin_full(name, pinyin_map):
    """全拼串：逐字符查映射，非汉字/缺映射跳过（排序键，同组内按全拼）。"""
    return "".join(pinyin_map.get(ch, "") for ch in name)


def pinyin_abbr(name, pinyin_map):
    """拼音首字母缩写：每字拼音首字母拼接（非汉字跳过）。水调歌头 → sdgt；苏轼 → ss。"""
    return "".join(pinyin_map.get(ch, "")[:1] for ch in name)


def pinyin_head(name, pinyin_map):
    """首字母分组：数字开头 → '0'；汉字（映射命中）→ 首字母大写；其他（⿰/符号/乱码）→ '#'。"""
    first = name[0] if name else ""
    if first.isdigit():
        return "0"
    full = pinyin_map.get(first)
    if full:
        return full[0].upper()
    return "#"


def clean_rhythmic(raw):
    """显示归并：仅含 ⿰ 的词牌「A·B」取主词牌 B（与 SongciRepository.cleanRhythmic 一致）；普通原样。"""
    if "\u2ff0" not in raw:
        return raw
    return raw.replace("\u2ff0", "").split("·")[-1].strip() or raw


def build_db():
    start_time = time.time()

    # 加载数据
    print("读取 data/ciauthor.json ...")
    authors_raw = load_json(AUTHOR_FILE)
    print(f"  诗人记录: {len(authors_raw)}")

    print("读取 data/ci.json ...")
    poems_raw = load_json(CI_FILE)
    print(f"  词作记录: {len(poems_raw)}")

    name_to_id = {a["name"]: int(a["value"]) for a in authors_raw}

    db_exists = os.path.exists(DB_FILE)

    # 连接数据库
    conn = sqlite3.connect(DB_FILE)
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA synchronous = NORMAL")
    # 与 SQLDelight Schema.version 同步:Android 驱动按 user_version 判等,不一致会执行建表
    # (预建库已有表 → "table already exists" 崩溃)。sq 变更时同步此值。
    conn.execute("PRAGMA user_version = 1")

    # --- 重建核心表 ---
    if db_exists:
        print("删除旧核心表 ...")
        conn.execute("PRAGMA foreign_keys = OFF")
        for table in CORE_TABLES:
            conn.execute(f"DROP TABLE IF EXISTS {table}")
        conn.execute("PRAGMA foreign_keys = ON")

    print("创建核心表 ...")
    conn.executescript("""
        CREATE TABLE authors (
            id          INTEGER PRIMARY KEY,
            name        TEXT    NOT NULL,
            long_desc   TEXT,
            pinyin_head TEXT,   -- 拼音首字母分组: 0/A-X/#(数字/汉字/异常字符)
            pinyin_full TEXT,   -- 全拼串(同组内排序键)
            pinyin_abbr TEXT    -- 拼音首字母缩写(搜索: sdgt → 水调歌头)
        );

        CREATE TABLE poems (
            id               INTEGER PRIMARY KEY,
            author_id        INTEGER,
            rhythmic         TEXT    NOT NULL,
            content          TEXT    NOT NULL,
            recommended_date INTEGER,   -- 每日推荐池标记:最近推荐日期(epoch day),非 NULL = 已推荐过
            FOREIGN KEY (author_id) REFERENCES authors(id)
        );

        CREATE TABLE rhythmic_index (
            rhythmic    TEXT PRIMARY KEY,   -- 归并后主词牌(clean_rhythmic 结果)
            pinyin_head TEXT NOT NULL,      -- 首字母分组: 0/A-X/#
            pinyin_full TEXT NOT NULL,      -- 全拼串(同组内排序键)
            pinyin_abbr TEXT NOT NULL       -- 拼音首字母缩写(搜索: sdgt → 水调歌头)
        );

        CREATE VIRTUAL TABLE poems_fts USING fts5(
            content,
            tokenize='unicode61'
        );
    """)

    # --- 应用数据表（仅首次创建） ---
    if not db_exists:
        print("创建应用数据表 ...")
        conn.execute("""
            CREATE TABLE favorites (
                poem_id    INTEGER PRIMARY KEY,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (poem_id) REFERENCES poems(id)
            )
        """)
        conn.execute("CREATE INDEX idx_favorites_poem_id ON favorites(poem_id)")

    # 每日推荐池:date(epoch day) → 20 首 poem_ids(逗号分隔);当天固定
    # 独立于 db_exists 分支:新增表对已有 db 幂等创建
    conn.execute("""
        CREATE TABLE IF NOT EXISTS recommendation_pool (
            date     INTEGER PRIMARY KEY,
            poem_ids TEXT NOT NULL
        )
    """)

    # 最近查看:进入详情页记录(openPoem 统一入口);viewed_at 为 epoch 毫秒(毫秒级去重置顶,
    # 秒级 datetime 在同秒多查看时排序不稳定)。应用层每次记录后裁剪至最近 30 条。
    conn.execute("""
        CREATE TABLE IF NOT EXISTS recent_views (
            poem_id   INTEGER PRIMARY KEY,
            viewed_at INTEGER NOT NULL DEFAULT ((julianday('now') - 2440587.5) * 86400000),
            FOREIGN KEY (poem_id) REFERENCES poems(id)
        )
    """)

    # --- 插入诗人数据 ---
    print("插入诗人数据 ...")
    pinyin_map = load_pinyin_map()
    author_rows = [
        (
            int(a["value"]),
            a["name"],
            a.get("long_desc", ""),
            pinyin_head(a["name"], pinyin_map),
            pinyin_full(a["name"], pinyin_map),
            pinyin_abbr(a["name"], pinyin_map),
        )
        for a in authors_raw
    ]
    conn.executemany(
        "INSERT INTO authors (id, name, long_desc, pinyin_head, pinyin_full, pinyin_abbr) VALUES (?, ?, ?, ?, ?, ?)",
        author_rows,
    )

    # --- 插入词作数据 ---
    print("插入词作数据 ...")
    unmatched = []
    poem_rows = []
    for p in poems_raw:
        author_name = p["author"]
        author_id = name_to_id.get(author_name)
        if author_id is None:
            unmatched.append(author_name)
        poem_rows.append((
            int(p["value"]),
            author_id,
            p["rhythmic"],
            p["content"],
        ))

    conn.executemany(
        "INSERT INTO poems (id, author_id, rhythmic, content) VALUES (?, ?, ?, ?)",
        poem_rows,
    )

    # --- 填充 FTS 索引 ---
    print("构建全文搜索索引 ...")
    conn.executemany(
        "INSERT INTO poems_fts (rowid, content) VALUES (?, ?)",
        [(int(p["value"]), p["content"]) for p in poems_raw],
    )

    # --- 填充词牌拼音索引(归并 ⿰ 后主词牌,按全拼排序) ---
    print("构建词牌拼音索引 ...")
    seen_rhythmic = set()
    rhythmic_rows = []
    for (rhy,) in conn.execute("SELECT DISTINCT rhythmic FROM poems"):
        clean = clean_rhythmic(rhy)
        if clean in seen_rhythmic:
            continue
        seen_rhythmic.add(clean)
        rhythmic_rows.append((
            clean,
            pinyin_head(clean, pinyin_map),
            pinyin_full(clean, pinyin_map),
            pinyin_abbr(clean, pinyin_map),
        ))
    conn.executemany(
        "INSERT INTO rhythmic_index (rhythmic, pinyin_head, pinyin_full, pinyin_abbr) VALUES (?, ?, ?, ?)",
        rhythmic_rows,
    )

    # --- 创建核心表索引 ---
    print("创建索引 ...")
    conn.executescript("""
        CREATE INDEX idx_authors_name ON authors(name);
        CREATE INDEX idx_authors_pinyin ON authors(pinyin_head, pinyin_full);
        CREATE INDEX idx_poems_author_id ON poems(author_id);
        CREATE INDEX idx_poems_rhythmic ON poems(rhythmic);
        CREATE INDEX idx_rhythmic_pinyin ON rhythmic_index(pinyin_head, pinyin_full);
    """)

    # --- 统计 ---
    conn.commit()

    author_count = conn.execute("SELECT COUNT(*) FROM authors").fetchone()[0]
    poem_count = conn.execute("SELECT COUNT(*) FROM poems").fetchone()[0]
    fts_count = conn.execute("SELECT COUNT(*) FROM poems_fts").fetchone()[0]
    rhythmic_count = conn.execute("SELECT COUNT(*) FROM rhythmic_index").fetchone()[0]
    null_author = conn.execute(
        "SELECT COUNT(*) FROM poems WHERE author_id IS NULL"
    ).fetchone()[0]
    fav_count = conn.execute("SELECT COUNT(*) FROM favorites").fetchone()[0]
    db_size = os.path.getsize(DB_FILE)

    conn.close()

    elapsed = time.time() - start_time

    # --- 输出统计 ---
    print()
    print("=" * 50)
    print("  迁移完成")
    print("=" * 50)
    print(f"  诗人:     {author_count}")
    print(f"  词作:     {poem_count}")
    print(f"  FTS 索引: {fts_count} 条")
    print(f"  词牌索引: {rhythmic_count} 条")
    print(f"  未匹配:   {null_author} 条")
    if unmatched:
        print(f"  未匹配作者名: {unmatched}")
    print(f"  收藏:     {fav_count} 条")
    print(f"  数据库:   {db_size:,} 字节 ({db_size / 1024 / 1024:.1f} MB)")
    print(f"  耗时:     {elapsed:.2f} 秒")

    if db_exists:
        print()
        print("  应用数据表（favorites）已保留，未受影响。")
    print("=" * 50)


if __name__ == "__main__":
    build_db()
