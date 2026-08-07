# 宋词 SQLite 数据库 — 技术设计

## Architecture Overview

```
data/ciauthor.json ──┐
                     ├──> db/build.py ──> db/songci.db (SQLite)
data/ci.json ────────┘
```

单向数据流：JSON → Python 构建脚本 → SQLite 数据库文件。数据库文件生成后为应用直接读取，无服务端。

### 核心/应用表分离

| 层 | 表 | 来源 | 重建行为 |
|----|-----|------|----------|
| 核心数据 | `authors`, `poems`, `poems_fts` | 源 JSON 派生 | DROP + 重建 |
| 应用数据 | `favorites`（及后续新增表） | 用户/应用运行时写入 | **保留不动** |

核心表每次运行构建脚本时重建，确保与源数据一致。应用表仅首次创建，后续重建不触碰，保护用户数据不丢失。

## Schema Design

### 表结构

```sql
-- === 核心数据表（每次重建） ===

CREATE TABLE authors (
    id          INTEGER PRIMARY KEY,   -- ciauthor.json value
    name        TEXT    NOT NULL,       -- 诗人姓名
    long_desc   TEXT                    -- 生平简介
);

CREATE TABLE poems (
    id          INTEGER PRIMARY KEY,   -- ci.json value
    author_id   INTEGER REFERENCES authors(id),
    rhythmic    TEXT    NOT NULL,       -- 词牌名
    content     TEXT    NOT NULL,       -- 词作全文
    FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE VIRTUAL TABLE poems_fts USING fts5(
    content,
    tokenize='unicode61'
);

-- === 应用数据表（首次创建，重建时保留） ===

CREATE TABLE favorites (
    poem_id    INTEGER PRIMARY KEY,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (poem_id) REFERENCES poems(id)
);
```

### 设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 词牌名存为字段 vs 独立表 | poems 字段 | 1,423 种词牌名但无需独立元数据；JOIN 减少，查询更快；后续可提取 |
| FTS tokenizer | unicode61 | 零外部依赖；CJK 逐字分词天然适合古诗词单字检索；短语搜索正确 |
| 外键约束 | 启用 FOREIGN KEY | 保证数据完整性；SQLite 默认需要 `PRAGMA foreign_keys = ON` |
| 收藏独立表 vs poems 列 | 独立 favorites 表 | 核心数据与用户数据分离；重建数据库不丢失收藏；后续新功能按同模式扩展 |
| 重建策略 | DROP 核心表 vs 删除文件 | 仅 DROP 核心表，保留应用表；用户数据不受重建影响 |

### 索引策略

```sql
-- 核心索引（每次重建）
CREATE INDEX idx_authors_name ON authors(name);
CREATE INDEX idx_poems_author_id ON poems(author_id);
CREATE INDEX idx_poems_rhythmic ON poems(rhythmic);

-- 应用索引（仅首次创建）
CREATE INDEX idx_favorites_poem_id ON favorites(poem_id);
```

### FTS5 搜索方案

**tokenizer 选择分析：**

- **unicode61**（选用）：内置，CJK 字符逐字切分。古诗词每字独立表意，逐字搜索最精确。"明月"短语搜索等价于 tokens["明", "月"] 连续匹配，天然正确。
- jieba 分词（不选用）：引入外部依赖；面向现代汉语分词，对古文分词精度存疑；增加构建复杂度。
- trigram（不选用）：索引体积约为 unicode61 的 3 倍；对古诗词无显著优势。

**查询模式：**

```sql
-- 简单关键词搜索
SELECT p.*, a.name AS author_name
FROM poems_fts f
JOIN poems p ON f.rowid = p.id
JOIN authors a ON p.author_id = a.id
WHERE poems_fts MATCH '明月'
ORDER BY rank;

-- 短语精确搜索
WHERE poems_fts MATCH '"明月几时有"'

-- 组合查询：某诗人的搜索结果
WHERE poems_fts MATCH '江山' AND p.author_id = <id>

-- 收藏查询：用户收藏的词作列表
SELECT p.*, a.name AS author_name
FROM favorites f
JOIN poems p ON f.poem_id = p.id
JOIN authors a ON p.author_id = a.id
ORDER BY f.created_at DESC;

-- 收藏 + 搜索组合：收藏中匹配关键词
SELECT p.*, a.name AS author_name
FROM favorites f
JOIN poems p ON f.poem_id = p.id
JOIN poems_fts ft ON ft.rowid = p.id
JOIN authors a ON p.author_id = a.id
WHERE poems_fts MATCH '明月'
ORDER BY rank;
```

## Data Migration Strategy

### 迁移流程

```
1. 读取 data/ciauthor.json → 构建 authors 字典 {name: id}
2. 读取 data/ci.json → 逐条匹配 author name → author_id
3. 若数据库已存在：PRAGMA foreign_keys=OFF → DROP 核心表 → PRAGMA foreign_keys=ON
   若不存在：跳过 DROP
4. CREATE 核心表（authors, poems, poems_fts）
5. 若首次创建：CREATE 应用表（favorites + 索引）
6. 批量 INSERT authors → INSERT poems → 填充 FTS5
7. CREATE 核心索引 → 提交事务 → 执行完整性校验
```

### 边界情况处理

| 情况 | 处理方式 |
|------|----------|
| "富ノ" 不匹配 | author_id 设为 NULL，诗词保留完整 |
| 同名诗人（数据中不存在，预检） | 如发现同名不同 ID，按 ID 精确匹配 |
| ci.json author 与多个诗人名匹配 | 精确匹配优先；模糊匹配记录警告 |

### 性能预估

| 指标 | 预估 |
|------|------|
| 原始 JSON 总大小 | ~8.8MB |
| SQLite 数据页 | ~6–8MB |
| FTS5 索引 | ~3–5MB |
| B-Tree 索引 | ~1–2MB |
| **数据库总大小** | **~12–15MB**（≤20MB 目标） |
| 全表扫描（无索引） | ~5–10ms |
| FTS 关键词搜索 | < 10ms |
| 按诗人+词牌组合查询 | < 5ms |

## Compatibility

- SQLite 版本要求：≥ 3.9.0（FTS5 引入版本，2015 年发布）
- 文件格式：标准 SQLite 3 数据库文件，跨平台通用
- 编码：UTF-8
- 无扩展依赖：不依赖 ICU、JSON1 等可选扩展
- 多平台适配：同一 `.db` 文件可直接用于 Python/Node.js/Swift/Android/Web(sql.js)

## Trade-offs

1. **词牌名非独立表**：牺牲了词牌名元数据扩展性，换取了更简单的查询。如需词牌名统计/描述，可通过 `SELECT DISTINCT rhythmic, COUNT(*) FROM poems GROUP BY rhythmic` 动态获取。

2. **unicode61 vs jieba**：牺牲了现代汉语语义分词精度，换取了零依赖和构建简化。古诗词逐字搜索在用户体验上差异极小。

3. **FTS 仅索引 content**：不索引诗人名和词牌名（这些通过普通 B-Tree 索引已足够快）。减少了 FTS 索引大小。

4. **核心/应用表分离**：牺牲了单表收藏查询的便利性（需 JOIN），换取了数据安全（重建不丢失用户数据）。后续新功能（书签、笔记、阅读记录）遵循同模式：独立应用表，核心表不动。
