# 词作竖排排版 —— Design

## 背景与根因

Compose Multiplatform 1.11.1 的 `Text`/富文本**无原生竖排**(`writing-mode: vertical-rl` 类能力不存在,`TextDirection` 仅 Ltr/Rtl)。因此竖排需用 Compose 原生组件**手动拼接**:把「横排一句一个 `Text`」改为「竖排一句 = 一列,列内逐字纵向堆叠」。

## 架构与边界

改动集中在 **`app/.../ui/screens/DetailScreen.kt`**(详情页壳与 `DetailBody`);数据层 `Segmenter.segment()` **不改**(已产出句列表,竖排复用同一来源)。

```
DetailScreen
 └─ DetailBody(wide: Boolean, layout: 横/竖, ...)
      ├─ 宽屏横排: StanzaColumn(上/下阕)   [现状保留]
      ├─ 窄屏横排: PoemLines              [现状保留]
      └─ 竖排(新增): VerticalPoemBody     ← 宽度无关,横竖统一
           ├─ 每句 → 单字符列表 Column(逐字)
           ├─ 多句 → Row,列序反转(reversed)
           ├─ 容器 horizontalScroll(多列右展)
           └─ 外层 verticalScroll(整页)
```

## 数据流 / 契约

- 句子来源:`Segmenter.segment(poem.content, vm.matchedSpec(rhythmic, content))` → `List<List<String>>`
- 竖排渲染:每句 `List<String>` → `toCharArray().toList()`(保留标点)→ 每字一个 `Column`
- 阅读顺序:首列在**右**(第一句 = 最右列),`reversed()` 使后续列依次向左
- 字体/字号:沿用 `MaterialTheme.typography`(楷/宋/明体)+ `scale` 缩放,竖排横排共用
- 状态:`vm.verticalLayout: Boolean`(全局 single source),设置页与详情页图标都改它

## 持久化

- 新增偏好 `verticalLayout`(bool),复用现有 settings 存取 API(data 层同 `saveFontStyle`/`loadFontStyle` 模式)
- 详情页顶栏图标切换 → `vm.toggleVerticalLayout()` → 写偏好 → 重启沿用
- 设置页新增「默认竖排」开关,绑定同一 `vm.verticalLayout`

## 兼容与迁移

- 三端共 `commonMain`,无平台分支;`desktopTest` 只测 Segmenter(不改)
- 无 db/schema 变更,无迁移
- 现有横排完全保留,竖排仅新增一个渲染分支

## 关键权衡

- **方案 A vs Canvas**:A 用原生组件,读屏可读、测试简单、迭代成本低;Canvas 可做落款/印章/字距网格更精细但复杂且无障碍差。本任务选 A。
- **右起 vs rotate**:用 `reversed()` 控制列序(语义正确),**不用** `rotate`(破坏文本流/无障碍)。
- **双滚动**:竖排容器 `horizontalScroll` + 外层 `verticalScroll`;竖排组件宽度无关,窄/宽屏共用。

## 风险 / 回滚

- 风险:标点在单字 `Text` 里的悬挂/缩进(多端渲染差异)→ 兜底:每字 Text 用 `letterSpacing` 微调,不追求引擎级标点压缩
- 回滚:竖排为独立 UI 分支,关闭开关(删除组件)即回纯横排;无 schema 依赖,一条 revert
