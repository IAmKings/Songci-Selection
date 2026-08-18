# 词作竖排排版(方案 A:字符矩阵 + Column)

## Goal

词作详情页新增**竖排阅读视图**(从右到左、句 = 一列,每句内逐字从上到下堆叠),与现代主流横向逐句排版可**切换**。让用户能以传统古籍「右起竖排、字距整齐」的方式阅读诗词,贴合 Classical Manuscript 设计系统,三端(Android/iOS/Desktop)行为一致。

用户价值:诗词本身就是古籍文体,竖排提供一个契合「书卷气/刻本」的阅读视角;且本项目已接入源流明体、未来可配合竖向排版增强古典氛围。

## 背景与现状(2026-08-17 勘查确认)

- 详情页排版集中在 `app/.../ui/screens/DetailScreen.kt`:
  - **宽屏(≥768dp)** `DetailBody` → 双栏 `StanzaColumn`(上/下阕并置,竖列展示);窄屏 → 单栏横向 `PoemLines`(逐句 `Text`)
  - 数据层 `Segmenter.segment()` 已把词切成「句」(`List<List<String>>`,上/下阕);`RhythmicSpec` 提供格律
  - 字号缩放 `scale`;结果都走 `MaterialTheme.typography`(楷体/宋体/明体可切换)
- **技术事实**:Compose Multiplatform 1.11.1 的 `Text`/富文本**无原生竖排**(`TextDirection` 仅 Ltr/Rtl,无 `writing-mode: vertical-rl`)。已查证 `ui-text` 无 `writing-mode / verticalText` API。
- 外部方案(赫蹏 heti 等)为 Web CSS 方案,Compose 无法直接用——需在 Compose 内**语义复刻**。

## 方案 A(本任务)技术原理

将「横向逐句 `Text`」改为「**逐字纵向 Column**」:

1. `Segmenter.segment()`(已有)产出句列表 `List<List<String>>`
2. 每句拆成单字符(`toList()`),标点/韵脚符号按原顺序保留,作为一「列」
3. 每字一个 `Text` 放进 `Column`(竖向堆叠)——一列即一句诗
4. 多句多列放进 `Row`,`Row` 用 `horizontalArrangement`/方向反转实现「**右起**」(第一列在右,逐列向左)
5. 列间距、字间距沿用现有 `pair`/排版常量,DESIGN 统一
6. 字号缩放 `scale`、字体风格跟随现有 `MaterialTheme.typography`,竖排与横排共用同一字体

无需引擎级支持;全部由 Compose 原生 `Column`/`Row`/`Text` 组合完成,三端一致、零新依赖。

## Requirements

- 详情页(全屏详情与双栏右侧)提供竖排视图,可与现有横向视图切换
- 竖排 = 从右到左、每句一列、列内从上到下逐字
- 上/下阕结构保留(分段;**宽屏**沿用双栏等宽布局;**窄屏**可用单列竖排或分段分列)
- 竖排下字号仍支持缩放(复用 `fontScale`)
- 竖排下沿用字体风格(楷体/宋体/明体)
- 右起的阅读顺序正确(首个字在右上方)

## Acceptance Criteria(MVP)

- [ ] 词作详情页顶栏图标可切换横向 ↔ 竖排(右起竖排),切换顺畅
- [ ] 竖排正确呈现:示例(如「花间一壶酒/独酌无相亲」→ 右起两列,每列逐字向下)
- [ ] 宽屏上/下阕分段保留,双栏并置
- [ ] 竖排 vs 横排可平滑切换,状态持久化(详情切换写回设置,重启沿用)
- [ ] 设置页「默认横/竖排」开关生效(全局默认;详情切换同源写回)
- [ ] 三端(Android/iOS/Desktop):`assembleDebug` + `desktopTest` 通过;真机竖排效果目视确认
- [ ] 零新依赖;`git diff` 仅新增/改 UI 层 + 少量可选数据层

## Out of Scope(本 MVP 不做)

- 竖排**注音(ruby)**:Compose 跨平台无原生 ruby,需自绘,成本高 — 列入后扩展
- 印章/落款/回行装饰、Canvas 刻本渲染(方案 B)
- 竖排的文本选择/复制顺序优化(罕见场景,现横向 Text 也不保证词库复选顺序)
- 其它平台(Web/WASM)

## Key Decisions

1. **方案 A(字符矩阵 + Column)** 而非 Canvas:Compose 原生组件组合,三端一致、可访问性(读屏可读)、迭代成本低;Canvas 可控性高但复杂度大、无障碍差。
2. **右起方向**:用 `Row` + 列序反转(Scene 内直接控制),而非 transform 旋转——旋转破坏文本语义与无障碍。
3. **切换入口 = 词作详情页顶栏图标**(`‹ 词作详情` 右侧加横竖切换图标;2026-08-17 用户确认);**选择持久化到本地设置**:设置页新增「默认横/竖排」开关,详情切换写回该偏好,App 重启沿用(全局生效)。
4. **窄屏竖排 = 多列右展 + 水平滚动**(2026-08-17 用户确认):窄屏竖排也把全部句作为多列向右展开,像真的古籍卷轴;超出屏宽用水平滚动阅读更多列,而非仅单列。

## Risks / 开放问题(规划期需收敛)

全部决策已收敛(2026-08-17)。剩余为已知边界,非阻塞:

- **边界**:偶句/奇句并存、超长字句的列溢出 → 列随字号/屏宽 wrap;竖排容器用 `horizontalScroll`(多列右展)+ 外层 `verticalScroll`(整页)双滚动。阅读顺序:首列在右,逐列向左。


## Artifact Status

- [x] `prd.md` — 需求/验收/决策已收敛(本文件)
- [x] `design.md` — 技术设计与权衡已建
- [x] `implement.md` — 实施清单/验证/回滚已建
