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
