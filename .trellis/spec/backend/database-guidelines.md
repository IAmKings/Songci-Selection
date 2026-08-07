# Database Guidelines

> Database patterns and conventions for this project.

---

## Overview

- **Database**: SQLite 3 (≥ 3.9.0 for FTS5 support)
- **ORM**: None — raw SQL via Python `sqlite3` stdlib
- **Migrations**: Build script (`db/build.py`) — DROP core tables then recreate; app tables preserved
- **Encoding**: UTF-8
- **File**: `db/songci.db` (generated artifact, gitignored)
- **Source data**: `data/ciauthor.json`, `data/ci.json`

---

## Scenario: 宋词数据库 Schema

### 1. Scope / Trigger

- **Trigger**: Database schema for Song Ci poetry corpus (1,564 poets, 21,050 poems).
- **Source data**: `ciauthor.json` + `ci.json` → `songci.db`.

### 2. Signatures

```sql
-- === 核心数据表（源 JSON 派生，每次重建） ===

CREATE TABLE authors (
    id          INTEGER PRIMARY KEY,
    name        TEXT    NOT NULL,
    long_desc   TEXT
);

CREATE TABLE poems (
    id          INTEGER PRIMARY KEY,
    author_id   INTEGER REFERENCES authors(id),
    rhythmic    TEXT    NOT NULL,
    content     TEXT    NOT NULL,
    FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE VIRTUAL TABLE poems_fts USING fts5(
    content,
    tokenize='unicode61'
);

-- === 应用数据表（用户数据，首次创建，重建时保留） ===

CREATE TABLE favorites (
    poem_id    INTEGER PRIMARY KEY,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (poem_id) REFERENCES poems(id)
);
CREATE INDEX idx_authors_name ON authors(name);
CREATE INDEX idx_poems_author_id ON poems(author_id);
CREATE INDEX idx_poems_rhythmic ON poems(rhythmic);
CREATE INDEX idx_poems_favorite ON poems(is_favorite);
```

### 3. Contracts

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `authors.id` | INTEGER | PK | From `ciauthor.json` `value` |
| `authors.name` | TEXT | NOT NULL | 诗人姓名 |
| `authors.long_desc` | TEXT | nullable | 生平简介 |
| `poems.id` | INTEGER | PK | From `ci.json` `value` |
| `poems.author_id` | INTEGER | FK → authors(id), nullable | NULL for unmatched poets |
| `poems.rhythmic` | TEXT | NOT NULL | 词牌名 |
| `poems.content` | TEXT | NOT NULL | 词作全文 |
| `favorites.poem_id` | INTEGER | PK, FK → poems(id) | 用户收藏的诗作 |
| `favorites.created_at` | TEXT | DEFAULT (datetime('now')) | 收藏时间 |

**PRAGMA settings on connect:**
- `foreign_keys = ON`
- `journal_mode = WAL`

### 4. Validation & Error Matrix

| Condition | Behavior |
|-----------|----------|
| Author name in `ci.json` not found in `ciauthor.json` | `poems.author_id` = NULL, log warning, continue |
| Duplicate author names | Not present in source data; pre-check logs if detected |
| `ci.json` record missing `content` | KeyError — script fails fast |
| Re-run on existing `db/songci.db` | DROP 核心表后重建，应用表保留不动 |

### 5. Good/Base/Bad Cases

**Good**: 苏轼 (name in both files) → `author_id = 1`, 362 poems linked.
```sql
SELECT p.rhythmic, p.content FROM poems p
JOIN authors a ON p.author_id = a.id
WHERE a.name = '苏轼' LIMIT 3;
```

**Base**: "富ノ" (name only in ci.json) → `author_id = NULL`, poem retained.
```sql
SELECT id, rhythmic, content FROM poems WHERE author_id IS NULL;
```

**Bad**: Searching without FTS index — full table scan on 21K rows, ~50ms vs <5ms with FTS.

### 6. Tests Required

- [ ] Build script runs idempotently (rebuild produces identical core data)
- [ ] 1,564 authors, 21,050 poems, 21,050 FTS rows
- [ ] 苏轼 → 362 poems
- [ ] "富ノ" → author_id IS NULL
- [ ] FTS search "明月" < 100ms
- [ ] favorites 表存在，外键关联 poems.id
- [ ] 重建后 favorites 数据保留不变
- [ ] File size ≤ 20MB
- [ ] Foreign key integrity: 0 violations

### 7. Wrong vs Correct

#### Wrong
```python
# ❌ 直接字符串拼接 SQL，SQL 注入风险
cursor.execute(f"SELECT * FROM poems WHERE content LIKE '%{keyword}%'")

# ❌ 用户数据列混入核心表 — 重建数据库丢失用户数据
conn.execute("ALTER TABLE poems ADD COLUMN is_favorite INTEGER DEFAULT 0")

# ❌ 重建时删除整个 db 文件 — 用户数据全丢
os.remove("songci.db")
```

#### Correct
```python
# ✅ 参数化查询
cursor.execute("SELECT * FROM poems_fts WHERE poems_fts MATCH ?", (keyword,))

# ✅ 用户数据独立表 — 重建核心表不影响用户数据
conn.execute("""
    CREATE TABLE favorites (
        poem_id INTEGER PRIMARY KEY REFERENCES poems(id),
        created_at TEXT NOT NULL DEFAULT (datetime('now'))
    )
""")

# ✅ 精确重建核心表，保留应用表
conn.execute("PRAGMA foreign_keys = OFF")
conn.execute("DROP TABLE IF EXISTS poems")
conn.execute("DROP TABLE IF EXISTS authors")
conn.execute("PRAGMA foreign_keys = ON")
# ... 重建核心表，favorites 表完好无损
```

---

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Table names | lowercase plural | `authors`, `poems` |
| Column names | lowercase snake_case | `author_id`, `long_desc` |
| Index names | `idx_<table>_<column>` | `idx_poems_author_id` |
| FTS tables | `<table>_fts` | `poems_fts` |
| Primary keys | `id` (INTEGER) | — |
| Foreign keys | `<referenced_table>_id` | `author_id` |

---

## Common Mistakes

1. **Forgetting `PRAGMA foreign_keys = ON`** — SQLite disables FK enforcement by default.
2. **Using LIKE for Chinese search** — No index utilization. Use FTS5 MATCH instead.
3. **Committing `songci.db` to git** — Generated artifact; add to `.gitignore`.
4. **Re-running without deleting old db** — Build script DROPs core tables automatically; do not try to merge manually.
5. **Mixing user data into core tables** — Adding columns like `is_favorite` to `poems` means user data gets destroyed on every rebuild. Always use separate app tables with FK references to core tables.
