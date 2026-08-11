# Journal - kuluoluo (Part 1)

> AI development session journal
> Started: 2026-08-07

---



## Session 1: 宋词 SQLite 数据库设计与构建

**Date**: 2026-08-07
**Task**: 宋词 SQLite 数据库设计与构建
**Branch**: `main`

### Summary

基于 ciauthor.json 和 ci.json 设计并构建 SQLite 数据库。核心表（authors, poems, poems_fts）源自 JSON，应用表（favorites）独立管理，重建时用户数据不丢失。FTS5 全文搜索 <5ms。数据库 16.9MB。

### Git Commits

| Hash | Message |
|------|---------|
| `749acd6` | (see git log) |

### Status

[OK] **Completed**


## Session 2: 随机宋词 · kami 手机/平板排版样式

**Date**: 2026-08-07
**Task**: 随机宋词 · kami 手机/平板排版样式
**Branch**: `main`

### Summary

从 db/songci.db 随机抽取《贺新郎·戴复古》(poems id 13284,过滤残缺片段),用 kami 设计语言生成手机 375×812 与平板 768×1024 两个固定视口的静态 HTML 排版样式样例(design/specimens/),并附 README 记录出处、字体与设计决策。AC1 词文与库逐行一致、AC2 尺寸、AC3 kami token、AC4 说明全部通过;headless Chrome 实渲染截图确认可渲染(本会话无法目检图片,视觉状态已交由用户浏览器确认)。

### Git Commits

| Hash | Message |
|------|---------|
| `0491875` | (see git log) |

### Status

[OK] **Completed**


## Session 3: 宋词选粹 Compose Multiplatform 应用实现

**Date**: 2026-08-08
**Task**: 宋词选粹 Compose Multiplatform 应用实现
**Branch**: `main`

### Summary

实现 M1:Kotlin 2.4.10 + CMP 1.11.1 三端应用(app/),6 屏全做(首页推荐词流+目录索引/详情单双栏 768dp 断点/搜索 LIKE+朝代词牌筛选/收藏/设置字号)。SQLDelight 预建库(user_version=1 跳建表)+朝代推导脚本(覆盖率约 15%,数据缺口);搜索弃 FTS5 改 LIKE(实测 <20ms,unicode61 中文整段分词失效)。构建验证:桌面 jar+运行+6/6 基准测试、Android APK 41MB、iOS Xcode 构建成功(补 -lsqlite3)。trellis-check 修复 8 项(2 个 Android 首启崩溃/收藏竞态/词牌筛选/死代码)。待办:iOS 模拟器运行时未装、应用图标未接入、字号不持久化、朝代覆盖 15%。

### Git Commits

| Hash | Message |
|------|---------|
| `ba606c2` | (see git log) |

### Status

[OK] **Completed**


## Session 4: M1 体验打磨:字号持久化 + 首启异步 + 朝代覆盖

**Date**: 2026-08-08
**Task**: M1 体验打磨:字号持久化 + 首启异步 + 朝代覆盖
**Branch**: `main`

### Summary

三项打磨完成:①字号持久化(Settings expect/actual,SharedPreferences/NSUserDefaults/Properties 原生键值,零依赖)②首启异步(移除 runBlocking,Dispatchers.Default + 加载闸门,复制实测 7ms 定位为线程卫生)③朝代兜底宋(覆盖率 100%,年份规则 900/960 分界 + MIN_YEAR 700 + 策展表,修正王禹偁/吴淑姬/唐珏等回归;作者行显示年份证据 dynasty_evidence.json 159 位)。trellis-check 两轮 8 修 8。排查:字号不切换根因=测试污染设置文件(已隔离);⿰ 缺失字符(词牌 9 处+内容 3038 处)与金/元词人语料缺失(97 名录作者无词)记入 prd 数据缺口,后续必须处理。ponytail 精简 -100 行(正则合并曾致分布漂移已回滚)。8/8 测试 + 三端构建绿。提交 e5fb787。

### Git Commits

| Hash | Message |
|------|---------|
| `e5fb787` | (see git log) |

### Status

[OK] **Completed**


## Session 5: 三端应用图标接入 + GitNexus 索引

**Date**: 2026-08-08
**Task**: 三端应用图标接入 + GitNexus 索引
**Branch**: `main`

### Summary

三端图标接入完成:单一源 design/app-icon/screen.png + gen_app_icons.py 生成全部产物(幂等可重跑)。Android adaptive icon(66% 安全区)+ legacy 全密度 + manifest 引用;iOS AppIcon 单尺寸 1024;桌面 CMP 1.11 按平台 iconFile DSL(icns/ico/png)。macOS/iOS 图标按 Apple 网格烘焙圆角卡面(1024/824/185 + 透明边距),人工验收通过。修复 packageVersion 0.1.0→1.0.0(jpackage 要求首数字非零)。trellis-check 全 PASS,修正脚本位置到 app/data/tools/。另按用户要求初始化 GitNexus 索引(611 nodes),约束文件(CLAUDE.md/AGENTS.md/skills)入库。提交 e9c0692 + aaae66d。

### Git Commits

| Hash | Message |
|------|---------|
| `e9c0692` | (see git log) |
| `aaae66d` | (see git log) |

### Status

[OK] **Completed**


## Session 6: iOS 图标 alpha 修复(App Store 合规)

**Date**: 2026-08-08
**Task**: iOS 图标 alpha 修复(App Store 合规)
**Branch**: `main`

### Summary

修复 trellis-check 遗留项:iOS AppIcon.png 去除 alpha 通道。iOS 改用源图全出血 RGB(1024×1024),圆角由 iOS 系统 squircle 蒙版切,与 macOS 预烘焙圆角卡面区分;桌面/Android 产物字节不变。验证:RGB 无 alpha ✓、xcrun actool 编译 exit 0 ✓、脚本幂等 ✓。adaptive 前景单密度确认为 Android 官方允许做法,不做改动。提交 4fab352。

### Git Commits

| Hash | Message |
|------|---------|
| `4fab352` | (see git log) |

### Status

[OK] **Completed**


## Session 7: 桌面端词库全空白修复(java.sql 模块 + 资源缓存 + 图标缓存)

**Date**: 2026-08-08
**Task**: 桌面端词库全空白修复(java.sql 模块 + 资源缓存 + 图标缓存)
**Branch**: `main`

### Summary

打包版桌面应用词库全空白,双重根因:①jlink 精简运行时缺 java.sql 模块,SQLDelight JDBC 查询全抛 NoClassDefFoundError(主因,自首个 DMG 起存在,开发模式验证未覆盖打包运行时)——nativeDistributions 加 modules("java.sql") 修复(CMP 1.11 DSL,位于块内);②gradle 增量缓存损坏致 desktopJar 缺 composeResources(--rerun-tasks 强制重打修复)。图标显示异常定位为 macOS LaunchServices/Dock 缓存(lsregister -f + killall Dock),非打包问题。验证:jimage 确认 runtime 含 java.sql、bundle jar 含 songci.db、icns MD5 一致、二进制启动无异常。Android/iOS 驱动(AndroidSqliteDriver/NativeSqliteDriver)不依赖 java.sql,无此风险,APK 资源完整。trellis-check 全 PASS,ponytail-review 无冗余。提交 e711970。

### Git Commits

| Hash | Message |
|------|---------|
| `e711970` | (see git log) |

### Status

[OK] **Completed**


## Session 8: 词牌名清洗与格律映射(数据治理第一步)

**Date**: 2026-08-08
**Task**: 词牌名清洗与格律映射(数据治理第一步)
**Branch**: `main`

### Summary

词牌名清洗+钦定词谱格律映射管线完成:rhythmic_map.py(清洗:引号/·拆分/⿰剥离/全角 + desc别名索引565条 + A·B反向回退),映射1195/1423词牌(84.0%),词作覆盖95.3%;unmapped_rhythmics.json活档案228条三分类(placeholder 1交还原专项/alias-mismatch 24策展/missing-in-source 203)。数据源chinese_word_rhyme(818调)不入repo。trellis-check 0阻塞,修复A·B反向回退(84.0%)。抽查5词牌与钦定词谱一致。提交a7cb123。

### Git Commits

| Hash | Message |
|------|---------|
| `a7cb123` | (see git log) |

### Status

[OK] **Completed**


## Session 9: 格律 UI 接入(词牌详情平仄谱卡片 + 段边界)

**Date**: 2026-08-08
**Task**: 格律 UI 接入(词牌详情平仄谱卡片 + 段边界)
**Branch**: `main`

### Summary

格律数据接入 UI:rhythmic_map.json 扩展 6 字段(段边界 segs 由钦定词谱 shift 按段数推断,含三叠/四叠;叶/叠/换韵统一 Y 标记)。Rhythmic.kt 加载 + tuneLines() 纯函数(按句切行/段尾标记,可单测)。词牌详情页格律卡片:句式摘要+逐字平仄谱(平蓝/仄红/中灰,按句分行,阕间空行,韵脚下划线),整页统一滚动(卡片为 LazyColumn 首 item);词牌列表行尾字数标签;目录「格律」入口合并入「词牌」。trellis-check 12 问题全修复(L2:三段四段段边界 13 名调、README 同步;L3:flushLine 重构纯函数、叶叠换韵标记、KDoc)。测试 3 新增全绿,desktopTest/assembleDebug/packageDmg 通过。词作内容分段(切上下阕)按用户决策排在数据治理后实施。提交 82ee4d8。

### Git Commits

| Hash | Message |
|------|---------|
| `82ee4d8` | (see git log) |

### Status

[OK] **Completed**


## Session 10: 数据治理专项:⿰ 还原工具 + 金元词人补全

**Date**: 2026-08-08
**Task**: 数据治理专项:⿰ 还原工具 + 金元词人补全
**Branch**: `main`

### Summary

阶段3(⿰还原):restore_manifest.py 清单(3038处/254作者全覆盖)+restore.py 回填管道(CSV/校验/备份/幂等),3首知名词验证闭环(辛弃疾煮纤鳞/苏轼燕望极平田/范仲淹芦花,LIKE命中);辛弃疾「⿰⿰⿰」词牌名修正为「出塞」(=谒金门别名,联网确认,11615误改已恢复),placeholder未映射清零;6个贺铸特殊遗缺词牌按用户决策保留(清洗已映射格律)。阶段4(金元补全):jinyuan_audit.py 97人分类(金元19/花间18/残缺35/其他25)+异体候选27对;数据源snowtraces/poetry-source(MIT,金919+元5004首);jinyuan_merge.py 仅补名录内无词作作者:15位金元词人290首(蔡松年64/元好问57/李俊民49/段克己32/段成己28等),词库21050→21340,名录无词作97→82;title词牌解析(·取主+最长前缀匹配),内容级去重,外键关联验证。提交2d6774a+5b81651+2b86528。

### Git Commits

| Hash | Message |
|------|---------|
| `2d6774a` | (see git log) |
| `5b81651` | (see git log) |
| `2b86528` | (see git log) |

### Status

[OK] **Completed**


## Session 11: Bootstrap Guidelines 填充(项目真实约定入 spec)

**Date**: 2026-08-08
**Task**: Bootstrap Guidelines 填充(项目真实约定入 spec)
**Branch**: `master`

### Summary

填充 .trellis/spec 全部占位文件:backend 5 个(目录结构/错误处理/质量/日志——数据管线+防御解析+幂等+零依赖原则),frontend 6 个(目录/组件/hook Compose 对应/状态管理/质量/类型安全——单 ViewModel+复用组件+主题 token+768dp 自适应)。全部为本会话沉淀的真实约定(非通用模板),guides thinking guides 保留已有内容。索引更新为已填充。提交 e67e5b6。

### Git Commits

| Hash | Message |
|------|---------|
| `e67e5b6` | (see git log) |

### Status

[OK] **Completed**


## Session 12: 词作内容分段(格律段边界切上下阕)

**Date**: 2026-08-08
**Task**: 词作内容分段(格律段边界切上下阕)
**Branch**: `master`

### Summary

Segmenter.kt(data 层纯函数):词作去标点字数与格律首体一致→按 segEnds 段边界逐句切分(57.6% 精确),变体/未映射→行数对半兜底(≥4行),单调/短词单段。详情页宽屏双栏按真实阕界(2段并置/其他段数 Column 分隔),窄屏段间 gap 由段数驱动;移除 splitStanzas 近似对半。测试 5 个全绿(精确双调/单调单段/变体兜底/无格律/去标点),用户验收水调歌头/浣溪沙/如梦令正确。覆盖报告 57.6% 记入任务 notes,多体匹配提升覆盖率记 README 未来选项。提交 5d1696e。

### Git Commits

| Hash | Message |
|------|---------|
| `5d1696e` | (see git log) |

### Status

[OK] **Completed**


## Session 13: 格律多体切换(多体展示 + 分段精确率 57.6%→74.9%)

**Date**: 2026-08-08
**Task**: 格律多体切换(多体展示 + 分段精确率 57.6%→74.9%)
**Branch**: `master`

### Summary

rhythmic_bodies.json 全部体数据(1202 词牌,0 非法体);Rhythmic.bodiesOf/matchBody 按字数匹配体(首体优先);格律卡片体1..N 点击切换显示对应体;词作分段多体匹配(满江红 96 字变体精确切段),精确率 57.6%→74.9%(+3691 首);未映射词牌走兜底(已映射内 87.9%)。测试新增 bodiesAndMatchBody 全绿,用户验收。提交 1ba3684。

### Git Commits

| Hash | Message |
|------|---------|
| `1ba3684` | (see git log) |

### Status

[OK] **Completed**


## Session 14: alias 策展消解(律体校验双条件)

**Date**: 2026-08-08
**Task**: alias 策展消解(律体校验双条件)
**Branch**: `master`

### Summary

25 条 alias-mismatch 全部闭环:律体校验(词作字数 vs 目标调 bodies)作策展门槛——5 条消解(水调歌→水调歌头 4/5、木兰花→玉楼春 3/3(防误配修正目标)、雨中花→雨中花慢 1/1,共 16 首),20 条确认独立调重归类 missing-in-source(status=reviewed-independent,含人娇 67/68 vs 殢人娇 0 命中、簇水近句长 9 行 vs 簇水 15 句等证据)。alias-mismatch 清零,映射率 83.9%→84.2%,未映射 226 全为数据源缺失。提交 c45a4cb。

### Git Commits

| Hash | Message |
|------|---------|
| `c45a4cb` | (see git log) |

### Status

[OK] **Completed**


## Session 15: 技术债清理与质量闭环(多体体名/预索引/版本标记/三端验证)

**Date**: 2026-08-08
**Task**: 技术债清理与质量闭环(多体体名/预索引/版本标记/三端验证)
**Branch**: `master`

### Summary

格律体作者标注(bodies 6 字段,卡片显示毛滂体/苏轼体);技术债清理:expand 预索引(specToKeys/aliasToSpec 查表 O(1),新旧逻辑 1701 查询全量等价)+ 驱动缓存版本标记(db_version.txt 内容哈希,三端版本文件比对,根治旧库残留);trellis-check 发现阻断级 putIfAbsent(Kotlin/Native 无此 API, iOS 编译挂——JVM 解析 java.util 默认方法)——getOrPut 修复并固化三端构建验证约定(spec+README, 必修 desktopTest+assembleDebug+iOS compile);README 重构「有意决策(非技术债)」章节(核心库分离/DataStore/数据源扩充,触发条件驱动);数据源扩充检验归档(钦定 CSV 无韵脚/段边界,不值得);alias 策展补 2 条(一翦梅→一剪梅 49 首/水调→水调歌头 15 首,律体校验);根 README 仓库总览。提交 1f9a78f+c7b001f+cdf669d+a4deef0。

### Git Commits

| Hash | Message |
|------|---------|
| `1f9a78f` | (see git log) |
| `c7b001f` | (see git log) |
| `cdf669d` | (see git log) |
| `a4deef0` | (see git log) |

### Status

[OK] **Completed**


## Session 16: 搜索体系增强 + 字体替换(LXGW 霞鹜文楷)

**Date**: 2026-08-09
**Task**: 搜索体系增强 + 字体替换(LXGW 霞鹜文楷)
**Branch**: `master`

### Summary

作者页面显示简介(db long_desc 100% 覆盖接入,此前数据层丢弃);搜索增强:作者号/别称匹配(long_desc LIKE,友古居士→蔡伸,+4ms 实测)+词牌模糊筛选(水→水调歌头,组合搜索不再 0 结果,0.6ms);字体:仓耳今楷因商业许可未授权弃用(字体内嵌声明),替代评估后全量替换 Noto Serif SC→LXGW WenKai 霞鹜文楷(OFL-1.1,楷体贴合手稿设计,覆盖严格优于 Noto:5,379 字符缺 1 vs 缺 3,替换零损失;子集化分析不值得:搜索框任意输入需全字符集);PRD 全面对齐(状态标注/平台修正/页面清单);README 决策链完整。三端构建全绿(含 iOS compile 约定)。提交 7 个。

### Git Commits

| Hash | Message |
|------|---------|
| `08bd24a` | (see git log) |
| `0e57092` | (see git log) |
| `5ad96c0` | (see git log) |
| `d7bafe4` | (see git log) |
| `262803a` | (see git log) |
| `a413b78` | (see git log) |
| `47abc95` | (see git log) |

### Status

[OK] **Completed**


## Session 17: 桌面小组件开发(Android Glance 完整 + iOS WidgetKit 基础)

**Date**: 2026-08-09
**Task**: 桌面小组件开发(Android Glance 完整 + iOS WidgetKit 基础)
**Branch**: `master`

### Summary

基于 design/widgets 设计稿的三平台小组件:Android(Glance)完整交付——deep link(songci://poem/{id})三端注册机制 + 四规格(2x2/4x1/4x2/4x4 Responsive/LocalSize)+ 刷新/收藏(insertFavorite 写回)/阅读全文交互,同进程直读 db 零复制;iOS(WidgetKit)基础版——pbxproj 手编 Widget Extension target(含 Embed/依赖/App Group entitlements 双端, 中文引号坑)+ Swift TimelineProvider(SQLite3 直查 App Group db)+ App Group db 同步(Kotlin 驱动版本标记);macOS 因 Compose desktop 非 SwiftUI host 记录为后续项;构建验证: pbxproj xcodebuild -list + swiftc typecheck + 三端 Kotlin 全绿(本机缺 iOS runtime 完整构建受限)。提交 bd324ff+0c82352+f51fbc2。

### Git Commits

| Hash | Message |
|------|---------|
| `bd324ff` | (see git log) |
| `0c82352` | (see git log) |
| `f51fbc2` | (see git log) |

### Status

[OK] **Completed**


## Session 18: macOS WidgetKit 扩展接入验证通过

**Date**: 2026-08-09
**Task**: macOS WidgetKit 扩展接入验证通过
**Branch**: `master`

### Summary

桌面小组件无法加载的两大根因:仅部署 build 目录不注册 pluginkit(须 /Applications)+ 扩展缺 app-sandbox entitlement。补 containerBackground(macOS 26 硬性)。scripts/macos-widget-deploy.sh 固化嵌入/重签/部署。App Group db 同步 + 21 测试全绿。AC1-3 达成

### Git Commits

| Hash | Message |
|------|---------|
| `6ea4581` | (see git log) |
| `53a3995` | (see git log) |

### Status

[OK] **Completed**


## Session 19: Android 四规格小组件 + 全链路排障

**Date**: 2026-08-09
**Task**: Android 四规格小组件 + 全链路排障
**Branch**: `master`

### Summary

Android widget 排障:ColorProvider 资源ID陷阱、AppContextHolder 冷启动 NPE、actionStartActivity 深链失效(改 ACTION_VIEW ActionCallback)。四规格独立 provider 落地:添加面板 4 条目(2x2/4x1/4x2/4x4),minHeight 按 cellHeight 反推(60/170/320),共享 WidgetContent 按 spec 渲染。AC1-5 全过,21 测试绿

### Git Commits

| Hash | Message |
|------|---------|
| `10f30c6` | (see git log) |
| `b60136d` | (see git log) |

### Status

[OK] **Completed**


## Session 20: Widget 设计稿对齐 + 字体能力调研

**Date**: 2026-08-09
**Task**: Widget 设计稿对齐 + 字体能力调研
**Branch**: `master`

### Summary

Android 四规格 widget 对齐 design/widgets:布局(居中/三段式/品牌区/竖排)/图标 drawable/圆角边框/色板 token;截断机制(词牌 15 字/首句 77 字实测, maxLines+take 双保险);FontFamily.Serif+Italic 补上(1.1.1 实际支持)。字体结论:Glance 渲染在 launcher 进程,自定义 TTF 无法跨进程,1.1.1→1.3.0-alpha02 全系不支持,升级路线终止(1.2.0-rc01 编译零破坏但无 API;1.3.0-alpha02 需 compileSdk 37)

### Git Commits

| Hash | Message |
|------|---------|
| `baf134a` | (see git log) |

### Status

[OK] **Completed**


## Session 21: iOS/macOS 小组件点击直达深链

**Date**: 2026-08-09
**Task**: iOS/macOS 小组件点击直达深链
**Branch**: `master`

### Summary

widget 加 poemId+widgetURL(iOS/macOS);iOS Info.plist scheme+onOpenURL+MainViewController 透传;macOS Desktop.setOpenURIHandler+部署脚本注入 CFBundleURLTypes;common 导航改 LaunchedEffect(initialPoemId) 响应运行中点击。macOS 实机跳转验证通过;TCC 弹窗=开发重签所致,正式签名仅首次;App Group 同步延迟方案回退(阻塞数据加载)。iOS 代码完成待编译验证

### Git Commits

| Hash | Message |
|------|---------|
| `35525ed` | (see git log) |

### Status

[OK] **Completed**


## Session 22: 安全加固 + 小组件经验沉淀

**Date**: 2026-08-09
**Task**: 安全加固 + 小组件经验沉淀
**Branch**: `master`

### Summary

部署脚本签名证书改环境变量读取(~/.songci-signing.env,权限 600),仓库公开+历史含邮箱已评估接受(方案B)。trellis-check 全绿(desktopTest/assembleDebug/lintDebug),widget 五条硬教训沉淀进 spec quality-guidelines(Glance 字体平台限制、ColorProvider 陷阱、深链 trampoline、TCC 弹窗、证书不入库)

### Git Commits

| Hash | Message |
|------|---------|
| `49684bd` | (see git log) |
| `c294ae1` | (see git log) |

### Status

[OK] **Completed**


## Session 23: 导航分层重构(grill 驱动)+ 索引 tab + rail

**Date**: 2026-08-10
**Task**: 导航分层重构(grill 驱动)+ 索引 tab + rail
**Branch**: `master`

### Summary

grill-me 收敛导航模型:频道层/内容层分离。详情全屏盖 tab、openPoem 同层唯一(消除无限循环)、切 tab 内容层清+频道层内部保留(索引挖宝翻阅感)。索引进第 4 tab(自绘填充书本图标),宽屏 NavigationRail,收藏改书签(书页挑选→书签保留)。词牌变体名 FlowRow、窗口最小 360px、长名截断。桌面实机验收通过,21 测试绿

### Git Commits

| Hash | Message |
|------|---------|
| `f693003` | (see git log) |

### Status

[OK] **Completed**


## Session 24: 宽屏 master-detail 双栏 + 深链事件通道重构

**Date**: 2026-08-10
**Task**: 宽屏 master-detail 双栏 + 深链事件通道重构
**Branch**: `master`

### Summary

词牌/作者页宽屏双栏(左列表+右详情,选词内部切换);DetailBody 抽取复用;路由 poemId 参数(从详情进入右侧无缝衔接)。深链大修:macOS 同词重复失效根因=状态参数层快照脱节(证据链:handler 正常/写值正确/参数恒读旧值),改 Channel 事件通道组合内挂起迭代直接导航;ConcurrentLinkedQueue→Channel 修 iOS 跨平台隐患;openPoem inclusive 修复防详情叠加。合并 master,21 测试绿

### Git Commits

| Hash | Message |
|------|---------|
| `b2043ef` | (see git log) |

### Status

[OK] **Completed**


## Session 25: GitNexus 索引刷新 + Swift 解析纳入

**Date**: 2026-08-10
**Task**: GitNexus 索引刷新 + Swift 解析纳入
**Branch**: `master`

### Summary

npx gitnexus analyze --force 全量重建索引:1052 symbols/1951 edges/85 flows;Swift 解析器 rebuild 后纳入 6 个 swift 文件(无跳过);CLAUDE.md/AGENTS.md 上下文同步

### Git Commits

| Hash | Message |
|------|---------|
| `b2fafb6` | (see git log) |

### Status

[OK] **Completed**


## Session 26: 字体风格选择 + 词库子集化

**Date**: 2026-08-10
**Task**: 字体风格选择 + 词库子集化
**Branch**: `master`

### Summary

设置页新增楷体/宋体选择(霞鹜新致宋屏幕版),三端即时生效+持久化+widget 标注;scripts/subset-fonts.sh 词库 5275 字子集化(WenKai 25MB→2.6MB,新致宋 11.9MB→2.2MB,覆盖零损失);APK 72.6→31.3MB;IPA 授权入库。grill 收敛:动机为宋体审美非繁简转换

### Git Commits

| Hash | Message |
|------|---------|
| `9e8d05b` | (see git log) |

### Status

[OK] **Completed**


## Session 27: 首页每日推荐池

**Date**: 2026-08-11
**Task**: 首页每日推荐池
**Branch**: `master`

### Summary

每日 20 首推荐池:预建库加 recommended_date+recommendation_pool;build.py user_version=1 同步 Schema(修 Android table already exists);异常过滤(⿰ 词牌/内容、超长、单行、缺字);当天快照固定、90% 重置循环;运行中精确 0 点刷新(纯算术午夜);4 池测试。踩坑:桌面 db 在 ~/.songci(非 App Group)、datetime 0.7 API 反转、池快照不自动净化

### Git Commits

| Hash | Message |
|------|---------|
| `ccc8c48` | (see git log) |

### Status

[OK] **Completed**

## Session 28: 凌晨小组件自动更新

**Date**: 2026-08-11
**Task**: 凌晨小组件自动更新
**Branch**: `master`

### Summary

三端 widget 每日凌晨自动刷新为随机诗(与推荐池无关)。Android:MidnightRefreshWorker(WorkManager 一次性延迟+尾部重排,enqueueUniqueWork REPLACE 幂等防堆积,复用 msUntilNextMidnight 纯算术延迟,内置重启恢复零权限);iOS/macOS:Timeline policy .after(nextMidnight)(Calendar.nextDate 本地 0 点),系统到点刷新无需 app 运行。选型:用户对比后选 WorkManager(弃 AlarmManager 手动 BOOT_COMPLETED/updatePeriodMillis 30min 下限)。零新增权限、无 manifest 变更。assembleDebug/testDebugUnitTest/desktopTest/lintDebug/swiftc 全绿,detect_changes LOW

### Git Commits

| Hash | Message |
|------|---------|
| `12414b9` | (see git log) |

### Status

[OK] **Completed**

## Session 29: 凌晨刷新实机验收 + 手动刷新黑盒排查

**Date**: 2026-08-11
**Task**: 凌晨小组件自动更新(验收)
**Branch**: `master`

### Summary

一加13 实机验收:凌晨任务入队指向 0 点(jobId 动态,强制触发 cmd jobscheduler run -f -n androidx.work.systemjobscheduler)、MidnightRefreshWorker 四规格更新+尾部重排全通过。排障"连续点击无响应":加诊断日志确认全链路每次成功(广播→onAction→SessionWorker→渲染→updateViews→launcher callbacks),根因=ColorOS launcher 更新节流黑盒(窗口内丢弃重绘,数分钟恢复),无法从 app 侧修复。顺带修复真 bug:randomPoem/FavoriteAction 不关 driver → SQLite 连接池泄漏(上限4)耗尽致刷新静默失效(adaa1c3)。决策:手动快速刷新不可控不作为需求,只保证凌晨刷新;iOS/macOS 无此场景,手动刷新需 iOS17+/macOS14+ AppIntent(未实施)。spec 沉淀 3 条(widget 节流/db 泄漏/凌晨模式)

### Git Commits

| Hash | Message |
|------|---------|
| `adaa1c3` | fix(widget): 关闭 db driver 修复连接池泄漏致重复刷新失效 |

### Status

[OK] **Completed**

## Session 30: widget 随机诗异常字符过滤

**Date**: 2026-08-11
**Task**: widget 随机诗异常字符过滤
**Branch**: `master`

### Summary

widget 随机诗与首页推荐池统一过滤:⿰ 词牌/内容、𠴇/𫍙 缺字、词牌>12字、单行词全部排除。过滤下沉 randomPoems 查询层(与 dailyCandidates 同源),widget 与首页随机入口统一受益;iOS/macOS Swift SQL 字面量同步。真实数据验证 21340→20024 首(排除 1316 异常词),候选充足随机不受影响。assembleDebug/testDebugUnitTest/desktopTest/swiftc 全绿

### Git Commits

| Hash | Message |
|------|---------|
| `c39da31` | (see git log) |

### Status

[OK] **Completed**


## Session 28: widget 收尾:过滤回归测试 + 全量质量检查

**Date**: 2026-08-11
**Task**: widget 收尾:过滤回归测试 + 全量质量检查
**Branch**: `master`

### Summary

trellis-check 全绿:补 randomPoems 异常字符过滤回归测试(5轮x20首断言 ⿰/缺字/超长词牌/单行),lintDebug/desktopTest/testDebugUnitTest/swiftc 通过;清理验证时误建空文件 app/data/songci.db;GitNexus 索引刷新 1113 nodes。本会话全部工作:凌晨自动刷新(12414b9)+连接池泄漏修复(adaa1c3)+随机诗过滤(c39da31)+spec 沉淀(9496c5f)

### Git Commits

| Hash | Message |
|------|---------|
| `8bfc534` | (see git log) |

### Status

[OK] **Completed**
