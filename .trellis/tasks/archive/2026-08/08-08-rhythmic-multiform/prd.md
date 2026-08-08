# 格律多体切换(多体展示 + 分段精确率跃升)

## Goal

一石二鸟:①格律卡片支持**多体切换**(当前只展示首体,钦定词谱 826 调共 2,306 体);②词作分段按**多体匹配**(词作字数匹配任一体 → 用该体段边界),精确分段率 57.6% → 预计 90%+。

## Background

- 数据源 Ci_Tunes.json 每调含 formats 数组(全部体),当前 `rhythmic_map.py` 只提取首体(8 字段扁平 JSON)
- 分段器 `Segmenter.segment(content, spec)` 仅按首体字数匹配(57.6%),变体词(满江红 96 字 vs 首体 93 字)走兜底
- 体数已展示(「95字 · 11体」)但不可切换

## Requirements

- **R1 多体数据**:`rhythmic_map.py` 新增输出 `rhythmic_bodies.json`——`{词牌名: [ {sketch, chars, tune, rhythm, segs}, ... ]}`(全部体,复用现有提取逻辑 RHYTHM_CODE/segment_ends);主 map 不动(向后兼容)
- **R2 加载**:`Rhythmic` 加载 bodies 文件(懒加载或与主 map 同批);`bodiesOf(rhythmic)` 返回全部体
- **R3 分段多体匹配**:`Segmenter` 支持按字数选体——词作去标点字数 == 任一体 chars → 用该体 segEnds 切分(精确);无匹配 → 现有兜底
- **R4 卡片多体切换**:格律卡片「11 体」可点选切换(体选择行:序号按钮),切换显示该体 sketch/字数/平仄谱;默认首体
- **R5 覆盖报告**:分段精确率重测(57.6% → ?%),记入任务 notes

## Acceptance Criteria

- AC1:desktopTest 新增多体解析/匹配/切换状态测试,既有全绿(含 Segmenter 变体词用第二体精确切分)
- AC2:桌面运行:满江红 96 字变体词详情分段正确(非对半);卡片切换体 2..N 显示对应格律
- AC3:数据文件校验:bodies 全量可解析,chars/segEnds 一致性(脚本验证)
- AC4:覆盖报告:精确分段率提升幅度记录

## Out of Scope

- 体名/体序的权威标注(体按数据源顺序编号)
- 韵部/韵脚高亮

## 关键决策

- 独立 `rhythmic_bodies.json`(主 map 8 字段不动,消费方按需查)——避免主 map 膨胀与格式破坏
- 分段匹配优先级:首体 chars 相等 → 其余体 chars 相等 → 兜底(保持确定性)
- 卡片切换为局部 UI 状态(remember),不改导航/数据流
