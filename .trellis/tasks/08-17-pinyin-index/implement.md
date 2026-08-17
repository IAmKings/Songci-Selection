# 实施:词牌作者拼音排序与字母快捷导航

## 阶段 1 · 数据层(pinyin_map.json + db 变更)

- [ ] 1.1 提取词库唯一汉字集(ci.json + ciauthor.json 的 rhythmic/name/content),生成 `data/pinyin_map.json`(汉字→全拼),来源注明权威拼音表;缺字留空(后续归 `#`)
- [ ] 1.2 多音字人工抽查校正:词牌/作者首字多音(乐/长/重/调/行/还/朝/曲/单/区/曾…),校正后更新映射并留档
- [ ] 1.3 扩展 `db/build.py`:
  - authors 加列 `pinyin_head`/`pinyin_full`(生成规则:数字→0,汉字→首字母,其他→#)
  - 新建 `rhythmic_index` 表(归并词牌 + head + full,排序 (head, full, 码点))
- [ ] 1.4 重建 `db/songci.db`,跑 `prepare_db.py`(哈希判新自动生效)

## 阶段 2 · 查询层(SongciDb.sq + Repository)

- [ ] 2.1 `.sq` 同步新表/列:`allAuthors` 返回 pinyin_head/pinyin_full;新增 `allRhythmicsIndexed`(JOIN rhythmic_index)或改造 `allRhythmics`
- [ ] 2.2 `SongciRepository`:新增数据结构 `RhythmicIndex`/`AuthorIndex`(含 head);`rhythmics()`/`authors()` 返回带 head 已排序列表;删除 Kotlin 侧 `.sorted()`/`cleanRhythmic` 重复逻辑(收拢数据层)

## 阶段 3 · UI(IndexScreens.kt + 新组件)

- [ ] 3.1 新组件 `AlphabetIndexBar.kt`:竖排 0/A-X/#,点击回调 head
- [ ] 3.2 `TextRowList` 分组改造:`indexed` 开关、分组 header item、LazyColumn state 注入
- [ ] 3.3 词牌/作者屏接分组数据 + 索引条(仅长列表显示);目录索引/朝代列表维持原样(开关关闭)

## 阶段 4 · 测试与验证

- [ ] 4.1 desktopTest 新增:分组正确性(0/A/#/X 边界)、组内全拼排序、词牌归并后 head、空/全异常/全数字列表边界
- [ ] 4.2 `assembleDebug` + 真机安装:词牌/作者列表肉眼验收(跳转、header、0/# 组)

## 验证命令

```bash
cd db && python3 build.py && python3 ../app/data/tools/prepare_db.py   # 重建+打包
cd app && ./gradlew :composeApp:desktopTest                             # 数据层测试
# 真机:assembleDebug + adb install -r
```

## 风险文件

- `db/build.py`(schema + 数据):重建可逆,先备份 songci.db
- `app/composeApp/src/commonMain/sqldelight/.../SongciDb.sq`(表结构):与预建库同步改
- `IndexScreens.kt`(分组改造):保持 TextRowList 向后兼容(开关默认关)

## 回滚

- 数据层:git revert 脚本 + 重建 db
- UI:关 `indexed` 开关即回旧列表

## 完成后检查

- [ ] 三端排序一致(db 列驱动,无平台分支)
- [ ] 索引条点击跳转分组正确(含 0/# 组)
- [ ] 零新依赖(仅新增数据资产 pinyin_map.json,无代码依赖)
