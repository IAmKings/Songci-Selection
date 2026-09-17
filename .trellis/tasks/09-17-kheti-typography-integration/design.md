# Design:kheti 接入适配层与详情页排版替换

> 对应 prd.md(09-17-kheti-typography-integration)。范围锁定:仅词作详情页正文(横+竖),页头保留 Manuscript 设计。

## 1. 架构与边界

### 分层原则

- kheti 类型(`KhetiBlockSpec`/`KhetiTextStyle`/`KhetiColors` 等)**只在适配层文件内出现**,不泄漏到 screens 层;
  `DetailScreen` 只看到 `Modifier` + 业务参数(stanzas/title/author/fontStyle/fontScale)。
- 新增文件(建议位置 `ui/components/kheti/`):
  - `SongciKheti.kt` —— 唯一 import `com.kheti.*` 的文件:
    - `SongciKhetiTheme(fontStyle)`:把 `SongciColors`(ink=nearBlack/primary 视块、inkSecondary=stone)+
      `poemFamily(fontStyle)`(楷=文楷 / 宋=新致宋 / 明=源流明体→song 槽,kai 槽=文楷,hei=FontFamily.Default)注入 `KhetiTheme`;
    - `KhetiPoemBodyHorizontal(...)`:横排正文适配(见 §2);
    - `KhetiPoemBodyVertical(...)`:竖排适配(见 §3)。
  - `KhetiAvailability.kt`(expect/actual):iOS 回退开关(见 §4)。
- 删除/退役:`punctuatedLines()`(DetailScreen.kt L210-235)、`StanzaColumn()`/`PoemLines()` 的手写断行路径、
  `VerticalPoemBody()` 手写字符矩阵(L293-436,连同 `stableMaxChars`/`withFrameNanos` 两个 hack)、
  `data/Segmenter.kt` 的 `splitVerticalColumns()`/`VerticalColumn`(其唯一消费者就是旧竖排)。
- 保留:`Segmenter.segment(content, spec)`——格律段边界是 app 域知识,以 `List<List<String>>`(每阕一个 stanza,行内 `"\n"` 分句)
  喂给 kheti;`SegmenterTest.kt` 不动。

### 竖排装饰的保留方式

现有「词牌列 primary 色竖线」是页面品牌签名,适配层用 kheti-layout 的 per-block 能力保留:
词牌块 `color=SongciColors.primary`,竖线由适配层在块布局右侧自绘(块几何来自 `layoutKhetiVerticalBlocks` 返回值,不需 hack)。

## 2. 横排适配(`KhetiPoemBodyHorizontal`)

- 输入:`stanzas: List<List<String>>`、`fontStyle`、`fontScale`、`wide`。
- 构造 `KhetiBlockSpec`(每阕一块):
  - `KhetiTextStyle` 以 DESIGN.md 落地字号为基:`fontSize = (18/20 sp) * fontScale`,`lineHeight = fontSize * (2.05/2.1)`;
  - `alignment = KhetiAlignment.Start`(决策:保持左对齐);
  - `hang = KhetiHang.LineEnd`(决策:启用行末标点悬挂);
  - `spacing = true`(中西文间距 + 标点挤压);
  - `color = SongciColors.nearBlack`。
- 绘制入口:kheti-layout 公共引擎 `KhetiEngine(measurer, density)` → `layoutKhetiBlocks(...)` → `Canvas` + `drawKhetiText(...)`
  (songci 侧自写 ~30 行绘制壳,与 kheti-compose 内部 `KhetiBlocksView` 等价,但持有 songci 字号)。
- 宽屏双栏:沿用现有「两 Stanza 各占 weight(1f) + 1dp 竖分隔线」外层结构,只是每栏内容换成适配器;
  `maxLineLengthEm` 传宽松值(栏宽约束优先),避免 42em 上限与双栏冲突。
- ⚠️ 前置验证 V1(✅ 已完成 2026-09-17,方案 A 成立):kheti-layout/compose 公共 API 逐文件核实——
  `KhetiTextStyle`(任意 fontSize/lineHeight TextUnit)、`KhetiBlockSpec`/`layoutKhetiBlocks`(kheti-compose,public)、
  `KhetiVerticalBlockSpec`/`layoutKhetiVerticalBlocks`/`KhetiVerticalEngine`/`drawKhetiText`/`drawKhetiVerticalText`
  (kheti-layout,public)全部可直接依赖,无需 kheti 改动发版。唯一注意:`buildParagraphSpecs`/`KhetiBlocksView`
  为 kheti-compose internal(未用,songci 自写 ~30 行绘制壳等价)。
- ⚠️ 依赖解析实测:阿里云 central 镜像未同步 kheti(新发布构件,模块元数据命中镜像后不回退官方源)→
  settings.gradle.kts 加「按 group 定向」`repo1.maven.org`(仅 io.github.iamkings,不影响其余镜像策略)——**已实施并通过解析**。

## 3. 竖排适配(`KhetiPoemBodyVertical`)

- 替换 `VerticalPoemBody` 整体;块序右→左、一句一列、标点悬挂列尾、縦中横、跨阕块间 gap 全部由
  `KhetiVerticalEngine` + `layoutKhetiVerticalBlocks(engine, specs, limit, region, hang)` 承担。
- 块规格:
  - 词牌块:楷体/加粗、`color=primary`、`gapPx`=块间距;
  - 作者块:`titleMedium` 语义(stone 色)、稍小字号;
  - 正文块:每阕一块,`color=nearBlack`,跨阕自然由块间 gap 呈现(替代旧 ownerStanza=-1/空列机制)。
- 视口:`BoxWithConstraints` 提供 bounded height → `limit = maxHeightPx`(与 kheti `KhetiVerticalPoem` 同法);
  返回动画中 maxHeight 波动问题由「块布局 remember 键不含波动量」结构消除,不再需要 stableMaxChars。
- 初始滚动:保留「首列右起」语义,但用一次性 `scrollTo(maxValue)`(保留现 guard 逻辑语义,`withFrameNanos` 自旋改由
  `rememberScrollState` + `LaunchedEffect(Unit)` 常规等待,或视实现保留——以回归测试为准)。
- 底部「◀ 左右滑动翻阅」提示:保留,样式不变(MVP 不动)。

## 4. 兼容与回退

- **iOS 回退**(决策:三端同接+验证回退):`expect fun khetiEnabled(): Boolean`(actual:desktop/android=true;ios=编译期常量,
  验证通过后置 true)。适配器入口处 `if (!khetiEnabled()) 旧路径` ——旧路径仅 iOS 保留一小段期(或直接 git revert 依赖,见下)。
  为减少分叉:回退实现采用「最小旧路径副本」(iOS-only source set 内保留旧 `VerticalPoemBody`/`punctuatedLines` 副本),
  验证通过后删除。
- **设置项**:fontStyle 三档 / fontScale / verticalLayout(含详情页临时切换语义)全部不变,适配层只读。
- **颜色**:kheti 仅需 ink/inkSecondary;primary 竖线与 stone 元数据由 per-block color 达成,不新增 token。
- **行高**:DESIGN.md 2.05–2.1 保留为块 lineHeight;kheti 网格/间距规则在自定义行高下仍按其度量推导(其行盒顶边模型与行高解耦)。
- **回滚**:依赖版本化在 version catalog 单行;整体回滚 = revert 本任务提交(代码无散落)。
  spec 例外记录指明该回滚方式。

## 5. 重要权衡记录

| 决策点 | 选择 | 备选 | 理由 |
|---|---|---|---|
| 消费方式 | Maven Central `kheti-compose:0.1.0` | 源码/复合构建 | 版本化、一键回滚;用户自己发布的推荐路径 |
| 字号任意缩放 | kheti-layout 直连适配层 | 请 kheti 加 API | 不动 kheti 公共 API/发版节奏;适配层 ~150 行 |
| 横排组件 | 自写绘制壳 + 引擎层 | `KhetiPoem`(高层) | KhetiPoem 自带居中标题版式,与 Manuscript 页头冲突 |
| 对齐 | 左对齐 | 居中(heti 默认) | 用户已选;居中留待后续视觉改版任务 |
| 词牌列竖线 | per-block color + 自绘保留 | 移除装饰 | 页面品牌签名,成本 ~20 行 |

## 6. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| kheti-layout 引擎 API 可见性未验证(§2 V1) | 中 | 实现首日 spike 验证;失败走方案 B(kheti 发版) |
| iOS 未实测 | 中 | 真机/模拟器烟雾验证 + expect/actual 回退开关 |
| 腾讯镜像拉取 Maven Central 构件失败 | 低 | 首日依赖解析验证;失败则 repos 补 central 直连或方案 B |
| 退栈动画压缩竖排区(quality-guidelines 固化案例)回归 | 中 | 实现后按 spec 案例「竖排区 BoxWithConstraints 约束高」复验 400ms chrome 延迟逻辑不动 |
| Canvas 自绘无障碍语义 | 低 | 沿用 kheti `semantics { text }` 模式,验收含 TalkBack/VoiceOver 抽查 |

## 7. 运维/回滚

- 单一 feature 分支、单一提交序列;version catalog 独立一行依赖声明。
- `licenses/` 增加 kheti NOTICE(上游 heti MIT © 2020 Sivan + kheti 移植声明)。
- spec 回写:「零新依赖」条目增例外记录(自研三端库 + 版本化 + 回滚方式)。
