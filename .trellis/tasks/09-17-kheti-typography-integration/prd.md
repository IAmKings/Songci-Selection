# kheti 中文排版库接入与详情页排版优化

> 状态:规划完成,待用户对最终规划摘要显式批准后才可 `task.py start`。用户明确要求:先出计划,不改代码。

## Goal

将用户自研的 kheti 库(heti 中文排版规则的 Kotlin/Compose Multiplatform 移植,
https://github.com/IAmKings/kheti)接入「宋词选粹」,替换词作详情页正文的手写排版实现,
获得赫蹏级中文排版(行末标点悬挂、中西文间距、标点挤压、竖排縦中横/悬挂列尾),
并消除现有自绘实现的已知 hack 与闪烁问题。

## Background(已核实证据)

### kheti 库(用户本人维护,基于 README + master 源码 KhetiTheme/KhetiPoem/KhetiText/KhetiVerticalPoem)

- Maven Central 已发布:`io.github.iamkings:kheti-compose:0.1.0`(Android AAR / Desktop JAR / iOS klib,GPG 签名);
  `kheti-compose` 以 `api` 透传 `kheti-core`(纯规则层)/`kheti-layout`(度量+定位+自绘引擎,TextMeasurer+Canvas)+ compose ui/runtime。
- 质量:84 项桌面测试 + 4 项真机(Android 16)全绿;与 Chrome 中真实 heti@0.9.6 逐字几何比对,纯 CJK 最大偏差 0.03px。
- 能力:中西文间 ¼ 字宽、标点 ½/¼ 挤压、0.02em 字距、行末标点悬挂(`KhetiHang.LineEnd`)、引号策略;
  竖排(Compose 全平台零官方支持,库自实现):列序右→左、一句一列、标点悬挂列尾、縦中横、区域标点、ruby 右侧;
  文章组件(Heading/Blockquote/Hr/List/Table/Figure/Footnotes)。
- 配置面:`KhetiTheme{}` 注入 `KhetiColors(ink/inkSecondary)` 与 `KhetiFontFamilies(song/kai/hei)`;
  块级 `KhetiBlockSpec`(text/style/alignment/hang/color/...)与竖排 `KhetiVerticalBlockSpec`(text/style/color/gapPx)。
- 缺口:公共 compose 高层组件字号只有 `KhetiMetrics.Size` 枚举档位(任意 sp 缩放需直连 kheti-layout 引擎层);
  `kheti-compose` 内部 `buildParagraphSpecs`/`KhetiBlocksView` 为 `internal`(适配层须用 kheti-layout 公共 API,见 design.md §2 V1 spike);
  iOS target 已配置但未实测(README 自述唯一剩余里程碑)。
- 许可:上游 heti 为 MIT(Copyright © 2020 Sivan),kheti NOTICE 已随库声明。

### 宋词选粹现状(手写排版,证据锚点)

- `app/composeApp/src/commonMain/kotlin/com/songci/app/ui/screens/DetailScreen.kt`:
  - 横排正文:`punctuatedLines()`(L210-235)手写贪心断行 + `PoemLines()`/`StanzaColumn()`;
    无悬挂/无中西文间距/无标点挤压,依赖 `Text` 平台 letterSpacing(kheti Phase 0 结论:Skia 与 Minikin 几何不同)。
  - 竖排正文:`VerticalPoemBody()`(L293-436)字符矩阵逐字 Text;自述已知 hack:
    `stableMaxChars` remember 压闪烁(L314-319)、`withFrameNanos` 自旋滚位(L337)、跨阕空列 ownerStanza 约定;
    标点占一格、无縦中横。词牌列 primary 竖线装饰(L373-395)。
- `data/Segmenter.kt`:`Segmenter.segment(content, spec)` 格律段边界(上下阕)是 app 域知识,保留;
  `splitVerticalColumns()`/`VerticalColumn` 仅服务旧竖排,随替换退役。
- `theme/Theme.kt`:字体三选一持久化(楷=霞鹜文楷/宋=霞鹜新致宋/明=源流明体,`poemFamily()` L82-88),
  设置项 fontScale/fontStyle/verticalLayout(AppViewModel.kt L97-148,详情页临时切换不持久化 L63-64)。
- `DESIGN.md`:纸面 #FBF9F2、墨字 nearBlack #141413、正文 18/20sp、行高 2.05–2.1、kicker+左对齐标题+分隔线页头。
- 项目 spec:「零新依赖」原则(`.trellis/spec/backend/quality-guidelines.md` L23-25、frontend 同名文件)——本次打破,须例外回写。
- 构建:gradle wrapper 指向腾讯镜像(官方源本机不可达),Maven Central 构件经镜像拉取需验证。

## Requirements

- **R1 依赖接入**:version catalog 单行引入 `kheti-compose:0.1.0`;「零新依赖」spec 例外回写;`licenses/` 增 kheti NOTICE。
- **R2 横排替换**(详情页窄屏+宽屏双栏):正文渲染交 kheti 引擎(悬挂/间距/挤压),保留 Segmenter 格律上下阕分段;
  页头(kicker/词牌大标题/作者/分隔线)保持 Classical Manuscript 设计;对齐保持左对齐;删除 `punctuatedLines` 路径。
- **R3 竖排替换**:`KhetiVerticalPoem` 等价能力的适配层替换手写字符矩阵;列序右→左、悬挂列尾、跨阕块间 gap、
  词牌 primary 块+竖线装饰保留;消除 stableMaxChars/withFrameNanos hack;退役 `splitVerticalColumns`;滑动提示样式不变。
- **R4 设置与设计兼容**:ink=nearBlack/inkSecondary=stone 映射;三档字体经 KhetiFontFamilies 注入生效;
  fontScale 任意缩放经 kheti-layout 直连适配层生效(kheti 不为 songci 改公共 API);行高 2.05–2.1 保留。
- **R5 回退缝**:expect/actual iOS 开关;iOS 模拟器(尽力真机)烟雾验证通过后启用,失败临时回退 iOS 原生路径副本。
- **R6 质量闭环**:desktopTest 全绿(SegmenterTest 保留);诗词正文路径无平台 letterSpacing 退化;
  quality-guidelines 两个固化案例场景(退栈 chrome 压缩/竖排视口高度)复验不回归;README/PRD.md 能力条目更新。

## Out of Scope

- 首页推荐卡、搜索结果、收藏列表等列表型界面(小片段原生 Text,性能与收益不成比例)。
- 格律卡片(平仄谱/韵脚下划线/2,306 体多体切换)——kheti 无平仄概念,拆独立后续任务。
- 暗色模式(app 无暗色是独立 M1 决策)、亮度/账号设置、赏析页(app 未实现)。
- 居中版式改版(用户已选保持左对齐;居中属后续视觉改版任务)。
- kheti 库本身的代码改动(仅当 design.md §2 V1 spike 失败时,才走方案 B 请 kheti 发版,届时重新过规划)。

## Key Decisions(用户已确认)

1. **范围**:MVP = 词作详情页正文(横排+竖排),页头保留 Manuscript 设计;列表与格律卡不动。
2. **依赖方式**:Maven Central `io.github.iamkings:kheti-compose:0.1.0`(用户自发布的推荐消费路径),版本化可一键回滚。
3. **横排对齐**:保持左对齐(与 Classical Manuscript 框架及现有宣传素材一致)。
4. **iOS 策略**:三端同接 + 任务内 iOS 验证 + expect/actual 回退开关(失败则 iOS 临时回原生)。

## Acceptance Criteria

- [ ] 词作详情横排(窄屏+宽屏):行末标点悬挂生效;中西文混排间距符合 heti 规则;上下阕分段保持;页头视觉不变。
- [ ] 词作详情竖排:列序右→左;标点悬挂列尾;跨阕间距;词牌 primary 块+竖线;切换词作/返回无重排闪烁。
- [ ] 设置页三档字体(楷/宋/明)与字号缩放在 kheti 渲染下全部生效;详情页横竖排临时切换语义不变。
- [ ] iOS 通过模拟器(尽力真机)烟雾验证或处于回退开关保护下;Android/Desktop 无回归。
- [ ] `./gradlew :composeApp:desktopTest` 全绿;正文路径无平台 letterSpacing;两个 spec 固化案例场景复验通过。
- [ ] 「零新依赖」例外已回写 spec;`licenses/` 含 kheti NOTICE;依赖单行可 revert。

> **实现进度(2026-09-17)**:代码全部落地;desktopTest 全绿;iOS/Android/Desktop 三 target 编译全绿;
> 适配层纯函数规格测试新增(KhetiPoemBodyTest 6 项)。**待人工目检**:桌面/Android 真机横竖排观感、
> 返回动画无闪烁、iOS 真机烟雾(回退开关 `KhetiAvailability.ios.kt` 一行翻转)。

## Risks & Deferred

- 中|kheti-layout 引擎 API 可见性未验证 → 实现首日 V1 spike 定案(design.md §2),失败走方案 B(kheti 发版)。
- 中|iOS 未实测 → 回退开关兜底(R5)。
- 低|腾讯镜像拉取 Maven Central 失败 → 首日依赖解析验证,失败补直连 repo。
- 中|退栈 chrome 压缩案例回归 → 按固化案例埋点复验(implement.md 步骤 5)。

## Notes

- 用户同时是 songci 与 kheti 两库作者;kheti 侧演进(方案 B)是备选路径而非默认。
- 证据来源:kheti README 与 master 源码、DetailScreen.kt、Theme.kt、AppViewModel.kt、DESIGN.md、.trellis/spec 依赖条目。
