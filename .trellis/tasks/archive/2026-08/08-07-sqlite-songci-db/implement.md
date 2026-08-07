# 宋词 SQLite 数据库 — 执行计划

## Implementation Checklist

### Step 1: 创建构建脚本 `db/build.py`

- [x] 读取 `data/ciauthor.json`，构建 `{name: id}` 映射字典
- [x] 读取 `data/ci.json`，逐条通过 author 名字查找 author_id
- [x] 创建 SQLite 数据库，启用 WAL 模式和 foreign keys
- [x] 批量 INSERT authors（使用 executemany 优化）
- [x] 批量 INSERT poems（包含 is_favorite 默认值 0）
- [x] 创建普通索引：authors.name, poems.author_id, poems.rhythmic, poems.is_favorite
- [x] 创建 FTS5 虚拟表并填充数据
- [x] 处理 "富ノ" 边界情况（author_id = NULL）
- [x] 输出迁移统计信息（记录数、不匹配数、耗时）

### Step 2: 验证

- [x] 确认 authors 表 1,564 条记录
- [x] 确认 poems 表 21,050 条记录
- [x] 确认 poems_fts 表与 poems 记录数一致
- [x] 抽样验证：查"苏轼"→ 返回其词作列表
- [x] FTS 搜索验证："明月" → 返回匹配结果
- [x] 收藏字段验证：所有记录的 is_favorite = 0
- [x] 文件大小 ≤ 20MB

### Step 3: 文档

- [x] 在脚本头部添加使用说明注释
- [x] 记录依赖项（仅 Python 3 标准库，无第三方依赖）

## Validation Commands

```bash
# 构建数据库
cd db && python3 build.py

# 快速验证
python3 -c "
import sqlite3
db = sqlite3.connect('db/songci.db')
print('Authors:', db.execute('SELECT COUNT(*) FROM authors').fetchone()[0])
print('Poems:', db.execute('SELECT COUNT(*) FROM poems').fetchone()[0])
print('FTS:', db.execute('SELECT COUNT(*) FROM poems_fts').fetchone()[0])
print('苏轼 poems:', db.execute(\"SELECT COUNT(*) FROM poems p JOIN authors a ON p.author_id=a.id WHERE a.name='苏轼'\").fetchone()[0])
print('File size:', __import__('os').path.getsize('db/songci.db'), 'bytes')
db.close()
"
```

## File Manifest

| 文件 | 类型 | 说明 |
|------|------|------|
| `db/build.py` | 新建 | 构建脚本 |
| `db/songci.db` | 生成 | SQLite 数据库（gitignore） |
| `data/ci.json` | 源数据 | 词作数据 |
| `data/ciauthor.json` | 源数据 | 诗人数据 |
| `.gitignore` | 新建 | 排除生成文件 |

## Dependencies

- Python 3.6+（标准库：json, sqlite3）
- SQLite ≥ 3.9.0（macOS 自带满足，Linux 通常满足）

## Risks & Rollback

- **低风险**：构建脚本只生成新文件，不修改源 JSON 数据
- **回滚**：删除 `db/songci.db`，修复脚本后重新运行即可
