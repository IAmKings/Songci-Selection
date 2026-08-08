# design.md · 宋词选粹 CMP 应用

## 架构与边界

```
songci/
└── app/                          ← KMP 工程根(独立 gradle 项目)
    ├── composeApp/
    │   ├── commonMain/           ← 共享:UI/导航/数据层/主题
    │   │   ├── ui/               ← 6 屏 Compose + 导航 + 自适应布局
    │   │   ├── data/             ← SQLDelight 查询 + 朝代推导产物
    │   │   ├── theme/            ← DESIGN.md token → Material3
    │   │   └── resources/        ← 字体、songci.db、dynasty_map
    │   ├── androidMain/          ← Android 壳(assets 打包、复制策略)
    │   ├── iosMain/              ← iOS 壳(NSBundle 资源、复制策略)
    │   └── jvmMain/              ← 桌面壳(macOS)
    ├── iosApp/                   ← Xcode 工程(iOS 壳)
    └── gradle/…
data/ db/ design/                 ← 仓库其余部分,app 不反向依赖
```

单 `composeApp` module(官方模板结构),包内分层(`ui`/`data`/`theme`);不做多 gradle module —— 首期无第二个消费者,多 module 是 YAGNI。

## 数据流

- **静态核心数据**(poems/authors/fts):只读。预建 `songci.db` 作为资源打包 → 首启复制到应用私有目录 → SQLDelight 以文件路径打开,`createSchema` 幂等(表已存在则跳过)。
- **朝代**:`data/tools/dynasty.py`(一次性,输出 `dynasty_map.json`:author_id → 北宋/南宋/唐/五代/金/元/清/未知)→ 打包为资源;数据层加载进内存 Map,与 SQL 过滤组合。
- **收藏**(favorites):SQLDelight 增删查,纯本地。

**朝代推导规则**(关键词优先,年份回退):`long_desc` 含 北宋/南宋 → 对应;含 唐/五代/金/元/清 → 对应;否则取生卒年(正则 `(\d{3,4})`)按区间:960–1126 北宋、1127–1279 南宋、960 前按其他朝代关键词;无法判定 → 未知(UI 归入「全部」)。

## 关键查询(SQLDelight .sq)

```sql
randomPoems:    SELECT p.id, p.rhythmic, a.name, p.content FROM poems p
                JOIN authors a ON a.id = p.author_id ORDER BY RANDOM() LIMIT ?;
byAuthor:       SELECT … WHERE a.name LIKE '%'||?||'%';
byRhythmic:     SELECT … WHERE p.rhythmic = ?;
byDynasty:      SELECT … JOIN (朝代内存过滤:author_id IN dynasty_map) WHERE …;
search:         SELECT … WHERE p.content LIKE '%'||?||'%' OR a.name LIKE … OR p.rhythmic LIKE … LIMIT ?;
favorites:      SELECT … FROM favorites f JOIN poems p … ORDER BY f.created_at DESC;
insert/deleteFavorite: INSERT OR REPLACE INTO favorites(poem_id) VALUES(?);
```

> **实现期修正(2026-08-08)**:全文搜索改用 **LIKE** 而非 FTS5 —— 实测 21k 行全表扫描 <20ms;FTS5 unicode61 对中文按整段分词(非逐字),`MATCH '明月'` 仅命中 1 行,原 FTS 索引对中文短语搜索基本失效。LIKE 同时消除 SQLDelight 对 FTS5 MATCH 的编译期兼容风险。`poems_fts` 表保留于库中但不再被查询。

## 主题映射(DESIGN.md → Compose)

| DESIGN.md | Compose Material3 |
|---|---|
| primary `#002046` / primary-container `#1B365D` | `ColorScheme.primary` / `primaryContainer` |
| surface 分层 `#fbf9f2…#e3e3dc` | `background`/`surface`/`surfaceVariant` 等 |
| on-surface `#1b1c18` / on-surface-variant `#44474e` | `onSurface` / `onSurfaceVariant` |
| near-black / stone / warm-sand / line | 语义色映射到 Material 角色 |
| 无 shadows、直角(0dp 圆角) | `Shapes` 全 0dp;禁 elevation 阴影 |
| kicker(4/5px 高品牌块) | 自定义组件 `Kicker` |
| notoSerif(文学)/ Inter(界面) | `Typography` 两族;字号/行高/字距取 DESIGN.md(标题 46/36、正文 20/18、行高 2.05–2.1) |

无 dark 主题(M1 切出亮度调节)。

## 屏幕 → Compose 组件

| 定稿 | 实现要点 |
|---|---|
| 首页 | TopBar(menu/标题/search) + 推荐词流卡片(词牌+作者+名句摘录+阅读全文) + 目录索引(朝代/作者/词牌/格律,底部弹层) + BottomNav(首页/收藏/设置) |
| 详情 | 单栏(窄)/ 双栏(≥768dp)自适应;词牌标题、作者、韵句分行、上下阕留白;设置字号生效;上下文跳转(作者→该作者列表、词牌→同词牌列表) |
| 搜索 | 输入框 + 朝代 chip(全部/北宋/南宋)+ 词牌 + 结果列表 → 点击进详情 |
| 收藏 | favorites 列表 + 空态,移除收藏 |
| 设置 | 阅读设置(字号)→ 详情页生效;关于;账号/通知/退出登录占位 |
| 目录索引 | 朝代/作者/词牌三类真实数据;「格律」按词牌聚合(无独立字段) |

导航:Compose Navigation(CMP 官方 `org.jetbrains.androidx.navigation`);底部导航窄屏显示,宽屏(≥768dp)改顶栏/侧栏(与定稿平板布局对齐)。

## 自适应策略

`WindowSizeClass`:width < 768dp → 单栏 + BottomNav;≥ 768dp → 双栏详情(上下阕并置,中缝 1px line)+ 顶栏导航。与 DESIGN.md「Fixed Grid」「tablet 双栏」一致。

## 兼容与迁移

- Android minSdk 24(FTS5 系统支持);iOS 15+;桌面 macOS 13+(CMP JVM target)
- 预建库 schema 变更:重新生成 db(改 `db/build.py` 属范围外;如需增列走应用表)
- 收藏数据独立于核心表,升级/重建不丢失(沿用 db 模块原则)

## 权衡与风险

1. **SQLDelight vs Room KMP vs 原生驱动**:选 SQLDelight —— 三平台统一类型安全查询;风险在 FTS5 MATCH 编译期支持(有降级路径,查询面收敛)。Room KMP 的 FTS 走注解生成,预建库适配成本更高;原生驱动三端三写,重复最多。
2. **预建库 + 幂等 schema**:避免 SQLDelight 建库流程与既有 16.9MB 库冲突;schema 回调仅检查 `poems` 存在。
3. **朝代在内存 Map 而非 DB 列**:不动核心库/不依赖 db 模块重建;2 万行内 IN 过滤性能无虞。**覆盖率现实**:关键词+年号+生卒年推导后仅约 15% 作者可分类(北宋 96/南宋 112/五代 18/金 2/元 1),其余归「未知」—— 数据源本身缺乏朝代信息,朝代筛选仅覆盖已知部分。
4. **字体**:Google Fonts 下载 Noto Serif SC + Inter 打包;离线可用。TsangerJinKai02 版权风险高,默认不用,按需替换。
5. **首页推荐 = 随机**:无行为数据;后续可加「每日一词」(seed 固定)增强。

## 回滚

- 纯新增工程(`app/`),不触碰 data/db/design;回滚 = 删除 `app/` + 不动其他目录
- 每步验证点见 implement.md;数据层查询先于 UI 验证(基准 SQL 对比)
