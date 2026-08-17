# 词牌作者拼音排序与字母快捷导航

## Goal

索引列表(词牌、作者)从 **Unicode 码点序**改为**拼音首字母序**,列表右侧新增**字母快捷导航条**(`0`/`A-X`/`#`)可点击跳转分组,列表内分组处显示字母 header。用户可按拼音首字母快速定位词牌/作者。

用户价值:码点序对中文无感知规律(「晏几道」排在「苏轼」前);拼音首字母是中文用户标准查找习惯;长列表(词牌 ~2,300、作者 1,564)可一键跳转。

## Key Decisions(2026-08-17 用户确认)

1. **拼音方案 = 数据层预计算**:`pinyin_map.json`(数据资产)+ `db/build.py` 扩展,写入 `authors.pinyin_head` 与新增 `rhythmic_index` 表;三端只读列,运行时零依赖。
   - 排除 TinyPinyin / C2Pinyin:均为 JVM-only,本项目 iOS(Kotlin/Native)无法加载;应用层引入需 expect/actual + iOS 自实现,违背项目零新依赖硬约束(对比表见设计文档)。
2. **导航形态 = 右侧字母条 + 分组 header**:`0`/`A-X`/`#` 竖排索引条,点击 `scrollToItem` 跳转;列表内分组字母 header。
3. **组内排序 = 全拼**,码点兜底(通讯录惯例);词牌以归并后主词牌计算拼音(「⿰⿰⿰·七娘子」→「七娘子」→ Q)。

## Requirements

- 分组规则:数字开头 → `0`;汉字 → 拼音首字母大写(`A-X`,直接映射声母,无 zh/ch/sh 特殊分组);其他(⿰/符号/引号/乱码/字母)→ `#`
- 词牌列表与作者列表启用拼音序 + 索引条;目录索引/朝代列表维持现状(朝代时间序,短列表)
- 拼音数据:纯数据资产 `pinyin_map.json`(词库字符集覆盖,缺字归 `#` 不丢条目),来源注明权威拼音表;多音字抽查校正留档
- 三端排序一致、可回滚(开关关闭即回旧列表)

## Acceptance Criteria

- [x] `db/songci.db` 含 `authors.pinyin_head/pinyin_full` 与 `rhythmic_index` 表,数据与 `ci.json/ciauthor.json` 一致(重建幂等,0.56s)
- [x] 词牌/作者列表按 0/A-X/# 分组,组内全拼序(实测:晏几道→Y、苏轼→S、⿰阳→#、水调歌头→S、七娘子→Q(归并)、「"僮至"念奴娇」→#)
- [x] 索引条含 0/A-X/#,点击跳转到对应分组首个条目(AlphabetIndexBar + LazyListState.scrollToItem);分组 header 显示
- [x] 目录索引/朝代列表不受影响(无索引条,heads 为空不分组;朝代仍时间序)
- [x] 词牌归并逻辑收拢数据层(build.py clean_rhythmic 生成 rhythmic_index);`cleanRhythmic` 保留为详情页/搜索展示归并(职责不同)
- [x] 运行时零新依赖;`data/pinyin_map.json`(62KB)+ `scripts/gen_pinyin_map.py`(一次性生成工具,pypinyin 仅开发期)为新增资产
- [x] desktopTest 新增 `pinyinGroupedAndSorted`(12 用例全绿);`assembleDebug` 通过,真机安装 Success

## 实施记录(2026-08-17)

- 分支 `feat/pinyin-index`;任务经用户批准后实施
- 数据层:`data/pinyin_map.json`(5,277 汉字全映射,多音字校正表 OVERRIDES 33 条);`db/build.py` 加列/建表;`prepare_db` 哈希判新自动替换三端缓存
- 查询层:`SongciDb.sq`(authors 加列 + rhythmic_index + allRhythmics JOIN + allAuthors 拼音排序);`SongciRepository.rhythmics()` → `List<RhythmicIndex>`;`Author` 加 `head`
- UI:`AlphabetIndexBar.kt` 新组件;`TextRowList`/`AuthorList` 分组 + 索引条(词牌/作者启用,目录/朝代关闭)

## Out of Scope

- 朝代列表排序(维持时间序)
- 列表内搜索/过滤
- 全拼输入搜索(FTS 已有 LIKE 搜索)

## Notes

- 预建库 `user_version=1` 不变,db 内容哈希判新自动替换三端缓存,无需迁移
- 多音字为数据准确性风险,校正表随映射文件留档(数据治理:只信权威源)
