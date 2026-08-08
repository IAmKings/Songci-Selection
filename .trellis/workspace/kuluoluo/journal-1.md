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
