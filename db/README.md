# 宋词数据库

基于 `data/` 目录下的宋词 JSON 数据生成的 SQLite 数据库。

## 快速开始

```bash
cd db && python3 build.py
```

生成 `songci.db`（16.9 MB），依赖仅 Python 3 标准库。

## 数据库结构

### 核心数据表（每次运行 `build.py` 重建）

| 表 | 记录数 | 说明 |
|----|--------|------|
| `authors` | 1,564 | 诗人（id, name, long_desc） |
| `poems` | 21,050 | 词作（id, author_id, rhythmic, content） |
| `poems_fts` | 21,050 | FTS5 中文全文搜索索引 |

### 应用数据表（首次创建，重建时保留不动）

| 表 | 说明 |
|----|------|
| `favorites` | 用户收藏（poem_id, created_at） |

## 常用查询

```sql
-- 诗人所有词作
SELECT p.rhythmic, p.content
FROM poems p JOIN authors a ON p.author_id = a.id
WHERE a.name = '苏轼';

-- 全文搜索
SELECT a.name, p.rhythmic, p.content
FROM poems_fts f
JOIN poems p ON f.rowid = p.id
JOIN authors a ON p.author_id = a.id
WHERE poems_fts MATCH '明月'
ORDER BY rank;

-- 收藏查询
SELECT a.name, p.rhythmic, p.content, f.created_at
FROM favorites f
JOIN poems p ON f.poem_id = p.id
JOIN authors a ON p.author_id = a.id
ORDER BY f.created_at DESC;

-- 添加/取消收藏
INSERT INTO favorites (poem_id) VALUES (42);
DELETE FROM favorites WHERE poem_id = 42;
```

## 架构原则

**核心与应用分离：** 源 JSON 数据 → 核心表（可重建），用户数据 → 应用表（永久保留）。

添加新功能时，创建独立的应用表，不修改核心表。重建数据库不会丢失用户数据。
