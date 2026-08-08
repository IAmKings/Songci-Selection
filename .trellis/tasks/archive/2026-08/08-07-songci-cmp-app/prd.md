# 宋词选粹 Compose Multiplatform 应用

## Goal

用 **Kotlin + Compose Multiplatform** 实现「宋词选粹」跨平台应用:把 `db/songci.db` 的 21,050 首词转化为宁静、学术的沉浸阅读体验(PRD 定位),手机单栏、平板/桌面双栏。首期覆盖 Android + iOS + 桌面(macOS),6 屏全做。

## Background / 已确认事实

**输入资源:**

| 资源 | 路径 | 说明 |
|---|---|---|
| 产品需求 | `PRD.md` | 探索/搜索、沉浸阅读、上下文跳转、收藏、设置 |
| 设计系统 | `DESIGN.md` | 羊皮纸 surface 体系、书生蓝 `#1B365D`、notoSerif 文学 + Inter 界面、直角零阴影、kicker |
| 屏幕定稿 | `design/screens/*/code.html` | 6 屏 Tailwind 原型 + 截图 |
| 排版样张 | `design/specimens/` | 标题 36/46px、正文 18/20px、行高 2.05/2.1 |
| 应用图标 | `design/app-icon/screen.png` | 1024×1024 |
| 数据库 | `db/songci.db`(16.9MB) | poems 21,050 / authors 1,564 / FTS5 / favorites |

**数据事实:** `poems(id, author_id, rhythmic, content)`、`authors(id, name, long_desc)`、`poems_fts`(unicode61 逐字分词)、`favorites(poem_id, created_at)`。content 换行分隔韵句,无阕标记;核心表只读,收藏为应用表。

**定稿屏幕结构(已核验):**
- 首页:顶栏(menu/标题/search)、目录索引(朝代/作者/词牌/格律)、推荐词作卡片流(词牌+作者+名句+阅读全文)、底部导航(首页/收藏/设置)
- 搜索:输入框「搜索作者、词牌、诗句...」+ 朝代筛选(全部/北宋/南宋)+ 词牌筛选 + 结果列表
- 设置:账号设置/阅读设置/通知设置/关于宋词选粹/退出登录(列表式)
- 详情(手机单栏 / 平板双栏,见 specimens 排版)

**已验证的可行性:** 朝代推导 —— 1564 作者均有 `long_desc`,含朝代关键词(北宋/南宋等)或生卒年,可推导(如范仲淹 989–1052 → 北宋)。

**已识别差异/缺口:**
- DB 无朝代字段(需推导)与格律字段(目录索引「格律」以词牌聚合实现)
- 首页「推荐词作」无推荐数据源(无浏览行为数据)
- 设置页含账号/通知/退出登录(均为 M1 切出项)
- 亮度调节:DESIGN.md 无 dark 主题 token

## Requirements

- **R1 工程**:Compose Multiplatform 标准结构(composeApp + iosApp),置于仓库根 `app/`(用户已预留空目录);Android/iOS/macOS(JVM)三平台 target。
- **R2 数据层**:内置 `songci.db`(资源打包,启动一次性复制到可写位置);SQLDelight 生成类型安全查询:随机取词、按作者/词牌/朝代过滤、FTS5 全文搜索、收藏增删查。
- **R3 朝代数据**:一次性脚本从 `authors.long_desc` 推导朝代(关键词 + 生卒年区间回退),产物随应用打包;不修改 `db/` 核心模块。
- **R4 主题**:DESIGN.md 全部 token → Compose Material3 主题(颜色/字体/间距/形状);notoSerif 文学 + Inter 界面字体打包进应用。
- **R5 页面**:按定稿实现 6 屏 —— 首页(推荐词流 + 目录索引)、词作详情、搜索结果、收藏、设置、平板/桌面自适应详情。
- **R6 自适应**:宽度 ≥768dp 详情双栏并置(上下阕),<768dp 单栏;底部导航仅窄屏显示,宽屏改侧边/顶栏。
- **R7 设置**:阅读设置(字号)+ 关于生效;账号/通知/退出登录按定稿展示为占位(提示后续版本)。
- **R8 首页推荐**:随机词流(词牌+作者+首句摘录),「阅读全文」进详情。

## Acceptance Criteria

- AC1:Android + iOS + macOS 三端均可构建运行,核心路径 首页→详情→搜索→收藏 走通
- AC2:主题颜色/字体/间距与 DESIGN.md 抽样一致(自动化比对或截图比对)
- AC3:详情页 ≥768dp 双栏、<768dp 单栏
- AC4:FTS 搜索结果与 SQL 基准查询一致;朝代过滤与推导规则基准一致
- AC5:收藏增删重启后保留(本地持久化)
- AC6:设置字号生效

## Out of Scope(M1)

- 赏析、账号系统、通知、在线同步/云服务
- 亮度调节(dark 主题无 token;如后续需要,另行设计 dark 变体)
- Web 平台(PRD 字面提及,后续扩展)
- 首页个性化推荐算法(数据不足)

## 关键决策(已确认)

| 决策 | 结论 | 依据 |
|---|---|---|
| 平台范围 | Android + iOS + 桌面(macOS) | 用户 2026-08-07 确认 |
| M1 范围 | 6 屏全做,切出赏析+账号 | 用户 2026-08-07 确认 |
| 工程位置 | 仓库根 `app/` | 用户已预留空目录 |
| 数据访问 | SQLDelight(预建库 + 幂等 schema) | 见 design.md 权衡 |
| 朝代数据 | long_desc 推导脚本(关键词+年份回退) | 抽样验证 1564/1564 可行 |
| 首页推荐 | 随机词流 | 无行为数据,PRD 未指定 |

## 风险与待办

- FTS5:Android 系统 SQLite(API 24+)含 FTS5;iOS/macOS 系统 SQLite3 含;MATCH 语法在 SQLDelight 编译期支持需实现期首验(降级路径:原生 SQL 直查)
- 字体文件需从 Google Fonts 下载打包(Noto Serif SC / Inter);TsangerJinKai02 为可选(版权/商用注意)
- 16.9MB DB 资源打包与首启复制耗时(一次性,可接受)
- 本机工具链(Xcode / JDK17+ / Android SDK)需实现首步核验
- 「格律」目录无独立数据,按词牌聚合实现(降级:隐藏该分类)
