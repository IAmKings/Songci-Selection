# Implement:kheti 接入执行计划

> 前置:prd.md 决策已收敛(范围/对齐/iOS/依赖方式),design.md 已定架构。用户明确:本任务经最终规划摘要显式批准后才可 `task.py start`。

## 有序清单

1. **V1 spike:依赖解析 + API 可见性验证**(半天内定案)
   - version catalog + `composeApp/build.gradle.kts` commonMain 加 `io.github.iamkings:kheti-compose:0.1.0`;
   - 验证腾讯镜像可拉取(repo1 构件);
   - desktopTest 源集里写临时探针:能否 new `KhetiEngine`、调 `layoutKhetiBlocks`/`layoutKhetiVerticalBlocks`、
     构造 `KhetiBlockSpec`/`KhetiVerticalBlockSpec`(visibility spike,design.md §2);
   - 失败分支:立即切方案 B(kheti 侧公开引擎入口/自定义字号 API),通知并同步 PRD/design。
2. **spec/许可前置**:「零新依赖」例外回写(`.trellis/spec/frontend/quality-guidelines.md` + backend 对应条目);
   `licenses/` 增 kheti NOTICE 文件。
3. **主题桥**:`SongciKheti.kt` 实现 `SongciKhetiTheme`(colors/fonts 映射),desktopTest 校验映射纯函数。
4. **横排替换**:适配器 + `DetailScreen` 窄/宽两路接入;删 `punctuatedLines`/旧 `PoemLines`/`StanzaColumn` 断行路径;
   桌面端目检:悬挂、中西文间距、上下阕段间距、宽屏双栏 + 分隔线。
5. **竖排替换**:适配器替换 `VerticalPoemBody`;删手写矩阵 + 两个 hack + `splitVerticalColumns`;
   目检:列序右→左、悬挂列尾、跨阕 gap、词牌 primary 块 + 竖线、初始右起、返回动画无重排闪烁
   (复验 spec「chrome 压缩 400ms」案例场景)。
6. **iOS 回退缝**:`KhetiAvailability` expect/actual;iOS-only 旧路径副本;Xcode 模拟器(尽力真机)烟雾验证后置 true。
7. **测试与守门**:
   - 新增 desktopTest:适配器块规格纯函数(颜色/字号/行高/hang 断言)、字体三档映射断言;
   - 保留 `SegmenterTest` 全绿;确认诗词正文路径无平台 `letterSpacing` 退化(对齐 kheti ADR-001 精神);
   - `./gradlew :composeApp:desktopTest` 全绿。
8. **收尾**:trellis-check 质量闭环;README/PRD.md 能力条目更新(词作详情:赫蹏排版);
   `gitnexus_detect_changes` 核对影响面(DetailScreen.kt/Segmenter.kt/Theme.kt 预期内)。

## 验证命令

```bash
cd app && ./gradlew :composeApp:desktopTest          # 单测全绿
cd app && ./gradlew :composeApp:packageDmg           # 桌面打包冒烟
# iOS:xcode 跑 ComposeApp framework 的宿主工程,烟雾验证详情页横/竖排
# Android:真机详情页横/竖排目检 + 返回动画无闪烁
```

## 风险文件与回滚点

- 高风险:`DetailScreen.kt`(唯一大改面;quality-guidelines 有两个固化案例与其相关——退栈 chrome 压缩、竖排视口高度)。
- 中风险:`gradle/libs.versions.toml`、`Theme.kt`(仅新增桥,不动现有 typography)。
- 回滚点:每步独立提交;整体回滚 = revert 依赖行 + revert DetailScreen 提交(设计上无散落改动)。

## task.py start 前检查

- [x] 用户已显式批准最终规划摘要(2026-09-17「开始执行」)
- [x] implement.jsonl / check.jsonl 已含真实 spec/research 条目
- [x] V1 spike 结果已回填 design.md §2(方案 A 成立,无需 kheti 改动)

## 执行状态(2026-09-17)

### 真机验收修正(2026-09-17 第二轮,用户反馈两项)

1. **竖排跨阕空隙不可见**:根因 = gapPx 单位 bug——kheti 引擎按 px 工作,适配层把 dp 语义值(24/8)当 px 直传,
   真机 density≈3 下间距缩为 1/3(24px≈8dp)。修复:`khetiVerticalBlockSpecs` 参数化 gap 并要求调用方
   `with(density){ dp.toPx() }` 换算;跨阕 gap 与横排阕距对齐为 **34dp**。
2. **竖排列距过挤**:根因 = 竖排样式 lineHeight 沿用旧字符矩阵「容量口径」1.25em(kheti 列宽默认=行高),
   列间空白仅 ≈0.25em。修复:列距提至 **1.7em**(用户选定;介于 heti 上游 1.5em 与 DESIGN.md 横排 2.05em 之间),
   列间空白 ≈0.7em;代价为同屏列数略少。
3. 验证:desktopTest 绿(KhetiPoemBodyTest 同步断言 1.7 口径 + gap 显著性);Android 真机截图确认跨阕空隙与疏朗列距;
   iOS 编译复验通过(见下)。

### 真机验收修正(2026-09-17 第三轮:阕距挂错块,第二轮的 34dp 实际未生效)

1. 用户复验:列距 1.7em 可验收,但**分阕间隔仍不可见**。根因不在数值而在**挂载点**:kheti `gapPx` 语义 =
   「该块与其**左侧**相邻块」的距离,第一/二版把阕距挂在**下阕**(列表更后的块)上 → 落点在内容左缘之外,
   上阕块的 gapPx=0 → 两阕之间实际 0 间距(此前截图判读有误)。
2. 修复:阕距改挂**前(右)一阕**块(末阕 gap=0 防画布幽灵留白);语义升级为「**空开一列**」——
   gapStanzaPx = 一个列宽(bodyStyle.lineHeightPx,随 fontScale 自动缩放),阕界总空白 ≈2.4em vs 普通列间 ≈0.7em。
3. 验证:desktopTest 绿(测试断言改为「gap 必须挂上阕块 + 末阕 0」);Android 真机截图:生查子 8 列同屏,
   阕界空列清晰可辨。

### 功能增补(2026-09-17 第四轮:赫蹏基线网格开关)

### 功能增补(2026-09-17 第五轮:网格收窄为「竖排网格」)

1. 用户反馈:横排行盒线破坏稿纸版面(不美观),竖排列盒线似传统竖排书信纸(美观)。
2. 修改:横排 `KhetiPoemStanza` 移除网格绘制(签名还原);网格仅竖排保留;设置文案
   「排版网格(调试)」→「**竖排网格**」(去"调试"二字),说明文字改为竖排专用;VM/Settings 注释同步。
3. 验证:desktopTest 绿;真机:开关开启后横排无任何参考线(版面干净)、竖排列盒线呈书信纸效果;
   验证后恢复默认关闭。教训:设置页开关坐标以 uiautomator dump 为准(目测 y 会点错行)。

1. 用户需求:把 kheti 的排版网格(调试覆盖层)加进配置,默认关闭。
2. 实现:kheti `KhetiTheme(showGrid)` 的等价能力进适配层——`KhetiPoemStanza`/`KhetiVerticalPoemBody` 加
   `showGrid` 参数,Canvas 内自绘(横排=行盒顶边横线;竖排=列盒边界竖线+块右缘闭合线;stone 35% 垫底,
   几何与 kheti 自家组件一致);不走 CompositionLocal(适配层直连引擎,参数更显式)。
3. 管线:Settings expect/actual(kheti_grid 三端键值)→ AppViewModel.khetiGrid/toggleKhetiGrid →
   SettingsScreen「排版网格(调试)」开关行(带说明文字)→ DetailScreen 两处调用点传入。
4. 验证:desktopTest 绿;iOS 编译绿;真机设置页开关切换+重启持久化 ✓;开启后横排行盒线/竖排列盒线截图确认 ✓;
   验证后已恢复默认关闭(uiautomator 精确定位,注意设置页开关行 y 坐标比目测低:网格开关 center≈(202,1493))。

1. ✅ V1 spike:API 可见性全 public(方案 A);依赖解析需 settings.gradle.kts 加 repo1 按 group 定向(阿里云未同步 kheti)
2. ✅ spec 例外回写(backend+frontend)+ `licenses/kheti-NOTICE.md`
3. ✅ 主题桥 `ui/components/KhetiPoemBody.kt`(colors/fonts/字号映射)+ `Theme.kt` poemFamily internal 化
4. ✅ 横排替换(DetailScreen 窄/宽两路接入;kheti 类型隔离于适配层)
5. ✅ 竖排替换(KhetiVerticalPoemBody;旧实现整体搬入 `DetailLegacy.kt` 作 iOS 回退副本)
6. ✅ iOS 回退缝 expect/actual(三端 actual=true);**iOS/Android/Desktop 三 target 编译全绿**
   - 顺带修复存量 iOS 编译阻断:IndexScreens.kt 两处 JVM-only `toSortedMap` → 跨平台等价(影响面 LOW,gitnexus impact 已核)
7. ✅ desktopTest 全绿(SegmenterTest 保留分段断言;竖排分列测试随实现退役并注明);新增 KhetiPoemBodyTest
8. 🔶 收尾:README/app/README 已更新;gitnexus MCP detect-changes 本会话无 MCP 工具(以 impact 分析+三端编译替代);
   **待用户目检**:桌面/Android 真机横竖排观感、返回动画、iOS 真机烟雾;提交待用户确认(工作区含用户 TTS WIP,不宜自动 commit)
