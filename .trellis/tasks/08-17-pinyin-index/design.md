# 设计:词牌作者拼音排序与字母快捷导航

## 架构总览

**数据层预计算(用户已定)+ UI 层分组渲染**。拼音为派生数据,在 db 生成时写入列/表,三端只读;运行时零依赖、排序一致。

```
data/ci.json + ciauthor.json + data/pinyin_map.json(拼音映射数据资产)
        │ db/build.py(扩展:拼音计算模块)
        ▼
db/songci.db: authors.pinyin_head + rhythmic_index 表
        │ app/data/tools/prepare_db.py(现成,哈希判新)
        ▼
composeResources/files/songci.db → 三端 SQLDelight 只读
        ▼
SongciRepository: rhythmics()/authors() 返回带 head 的数据 → IndexScreens 分组渲染 + AlphabetIndexBar 跳转
```

## 拼音数据源(pinyin_map.json)

- **纯数据文件**(非代码依赖,符合零新依赖):汉字 → 全拼/首字母映射,一次生成后作为数据资产提交
- 来源注明权威拼音表(如 Unicode Unihan 拼音数据 / 权威词典),生成脚本可复现
- 覆盖范围:词库实际出现的汉字(脚本提取唯一字符集,~5,000 级),非全量 2 万字——控制体积
- 多音字:**数据准确性风险**,生成后人工抽查校正词牌/作者上下文(乐/长/重/调/行/还/朝 等),校正表随映射文件留档(数据治理:只信权威源)

## db 变更

### authors 表加列

```sql
ALTER TABLE authors ADD COLUMN pinyin_head TEXT;   -- 作者名首字母(大写,如 "晏几道"→Y)
```

- 生成规则:汉字开头取全拼首字母;数字开头 → "0";其余(⿰/符号)→ "#"
- 组内排序用全拼列:`pinyin_full TEXT`(完整拼音串,同组内排序键,码点兜底)

### 新增 rhythmic_index 表(词牌索引)

```sql
CREATE TABLE rhythmic_index (
    rhythmic     TEXT PRIMARY KEY,   -- 归并后主词牌(cleanRhythmic 结果)
    pinyin_head  TEXT NOT NULL,      -- 首字母分组(0/A-X/#)
    pinyin_full  TEXT NOT NULL       -- 全拼(组内排序键)
);
```

- 由 poems 表 DISTINCT rhythmic → cleanRhythmic 归并(与 `SongciRepository.rhythmics()` 现逻辑一致)生成
- `allRhythmics` 改为 JOIN rhythmic_index,返回 (rhythmic, pinyin_head);归并/排序逻辑收拢到数据层

### 版本机制

- 预建库 `user_version=1` 不变;db 内容哈希变 → `db_version.txt` 变 → 三端缓存自动替换(现成机制,无需迁移)
- `.sq` 文件同步新表/新列(SQLDelight 打开预建库时表结构与声明一致即可)

## UI 设计(IndexScreens.kt)

### 数据流

- `SongciRepository.rhythmics()` → `List<RhythmicIndex(rhythmic, head)>`,已按 (head, pinyin_full, 码点) 排序
- `authors()` → `List<AuthorIndex(name, head)>` 同理
- 列表侧:`groupBy { head }` → 有序分组 `List<Pair<String /*head*/, List<T>>>`;分组起始 itemIndex 预计算供跳转

### 新组件 AlphabetIndexBar(右侧字母条)

- 竖排 `0 / A / B / … / X / #`(通讯录式);点击 → `LazyListState.scrollToItem(groupStartIndex[head])`
- 样式 token:楷体/`stone`/`primary` 高亮,窄条(约 20dp)贴右侧,背景不遮挡列表
- 仅当列表长度超过阈值(如 >15 组或 scrollable)时显示
- 数字/异常分组:`0`、`#` 固定显示在条两端

### TextRowList 分组改造

- `TextRowList` 增加 `indexed: Boolean` 开关(词牌/作者开,目录索引/朝代关——朝代时间序短列表不需要)
- 分组 header item:`head` 字母(小卡片样式,与列表卡片一致)
- LazyColumn 带 `state`(供 scrollToItem)

## 排序规则(最终)

1. 分组:数字开头 → `0`;汉字 → 首字母大写(A-X);其他(⿰/符号/乱码/引号)→ `#`
2. 组内:按 `pinyin_full`(全拼)排序,同拼音按码点兜底(稳定)
3. 词牌以**归并后主词牌**计算拼音(「⿰⿰⿰·七娘子」→「七娘子」→ Q)

## 兼容与回滚

- 纯增量:新增表/列 + 新 UI 组件,不删改现有字段
- 回滚:改回 SQL 排序 + 隐藏组件即可;db 重建可逆(build.py 幂等)
- 旧数据:三端 db 自动替换,无需用户操作

## 风险

| 风险 | 缓解 |
|---|---|
| 多音字读音不准(乐/长/重/调/行/朝…) | 权威拼音表 + 生成后人工抽查校正,校正表留档 |
| pinyin_map.json 覆盖不全(生僻字) | 脚本按词库字符集生成,缺字归 `#`(不崩,不丢条目) |
| 词牌归并逻辑双份(数据层 + cleanRhythmic) | 收拢到数据层,`.sq`/Repository 同步改 |
| 索引条误触/遮挡 | 窄条 + 透明背景,仅长列表显示 |
