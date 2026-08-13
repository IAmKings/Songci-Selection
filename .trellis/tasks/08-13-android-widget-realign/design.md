# Design — 四规格视觉重对齐

在 `SongciWidget.kt` 现有结构(WidgetSpec + WidgetContent + 各规格 Composable)上做渲染层修正,不动 receiver/manifest/交互。所有改动可 git 回退。

## 字号表(设计稿 px → Glance sp)

设计稿为 CSS px,Glance sp 在 widget 渲染中近似等价(mdpi 1:1)。统一取设计稿值,超限场景由截断兜底。

| 元素 | 设计稿 | 当前 | 目标 |
|---|---|---|---|
| 2x2 词牌 | 24px | 16sp | 24sp |
| 2x2 首句 | 14px | 12sp | 14sp |
| 4x1 词牌 | 14px | 13sp | 14sp |
| 4x1 作者 | 14px | 10sp | 12sp* |
| 4x1 词句 | 14px | 12sp | 14sp |
| 4x2 品牌名 | 14px | 11sp | 14sp |
| 4x2 词牌 | 14px | 14sp | 14sp(补 tracking-widest) |
| 4x2 词句 | 14px | 11sp | 14sp |
| 4x4 头部 | 24px | 18sp | 24sp |
| 4x4 词牌(竖排) | 28px | 24sp | 28sp |
| 4x4 词句(竖排) | —(poem-body-mobile 14px 基线) | 12sp | 14sp |
| 4x4 作者(竖排) | —(author 14px 基线) | 12sp | 14sp |

\* 4x1 作者 12sp:设计稿 14px 但 4x1 高度 60dp,与词牌 14sp 同列两行会溢出,取 12sp 并记录。

## 逐规格改动

### 2x2(SmallContent)
- `verticalAlignment = Alignment.Top` → `Alignment.CenterVertically`(S1)
- 图标:新增/改 `ic_book` 为实心版本(fillColor #1B365D,参考设计稿 FILL 1),尺寸 22→24dp(S2)
- 词牌 16sp→24sp、首句 12sp→14sp;首句颜色按 80% 透明度近似:直接使用 ARGB 80% 的 #1B365D(#CC1B365D) 或保留原色并记录(S3/S4)
- 间距:icon 与词牌 mb-2→padding(bottom 8.dp),词牌与首句 mt-1→padding(top 4.dp)

### 4x1(BannerContent)
- 词牌 13sp→14sp、作者 10sp→12sp、词句 12sp→14sp(B1/B2/B3)
- 左列 76dp→80dp 对齐 min-w-[80px](B4)

### 4x2(MediumContent)
- 刷新/收藏图标色:设计稿 text-stone #6B6A64 → 新增 `ic_refresh_stone.xml`(fill #6B6A64)与 `ic_favorite_stone.xml`;或直接改现有 vector fillColor(ic_refresh 现为 #1B365D,ic_favorite 已 stone 无需改)(M2)
- 品牌区:ic_book 26dp + "宋词选粹" 11sp→14sp,间距微调(M1)
- 词牌行:加 tracking-widest(Glance 无 letterSpacing?——TextStyle 有 letterSpacing,确认可用则加 0.05em 近似);作者独立成行 secondary(M3)
- 词句 11sp→14sp,行数 3→2(设计稿两行)(M4)
- 容器 padding 12→16dp(M5)

### 4x4(LargeContent)
- 头部:「宋词选粹」18sp→24sp;印章保持(L1,L7 微调:ic_seal 圆角 4→6dp 近似设计稿 rounded)
- 词牌竖排 24sp→28sp,移除 take(8) 截断改由高度自适应(Column 溢出由外层裁剪;保留 take 上限防异常)(L2)
- **词句竖排两栏**(L3):设计稿 vertical-text 竖排(每字一行、句间换列)。实现方案:
  - `Column { 每字一个 Text }`,与现有词牌竖排同构
  - 按上阕/下阕切两栏(设计稿:栏1「明月几时有,把酒问青天。/ 不知天上宫阙,今夕是何年。」栏2「我欲乘风归去,又恐琼楼玉宇,/ 高处不胜寒。」),栏间 1dp 竖分隔线(warm-sand)
  - 字高 14sp×行高 1.4≈20dp;4x4 高约 320dp,可用高度约 220dp → 每栏 ≤10 字,超限截断(字符级 take + maxLines 双保险)
  - 失败兜底:保持横排两列,记录差距
- **底部操作栏**(L4):`Row(GlanceModifier.fillMaxWidth().background(surface 80% ≈ #CCFBF9F2).padding(v 8.dp))` + 顶部 1dp warm-sand 分隔线;左侧 ↻ 圆形按钮(24dp Box + 背景 surface-variant 圆角 12dp + ic_refresh)、♡ 圆形按钮(ic_bookmark);右侧「阅读全文」胶囊按钮(Box padding h 12dp + border 1dp warm-sand 圆角 12dp + 文本 12sp + open_in_new 图标可选)
- **四角装饰线**(L5):4 个 `Box(size 14.dp)` 2dp 边框,warm-sand #E8E6DC(Glance 无半透明边框,用固定色),绝对定位四角(top/start/end/bottom 偏移 8dp)
- **羊皮纸纹理**(L6/P2):程序化近似尝试——drawable 层叠:基色 #F5F4ED + 若干低透明度暖色斑点(shape oval #E8E6DC @ ~5%)。Glance background 只支持单层 → 若不可行则跳过记录

## 资源改动清单(res/drawable/)

| 文件 | 改动 |
|---|---|
| `ic_book.xml` | 描边 → 实心(fill #1B365D) |
| `ic_refresh.xml` | fill #1B365D → #6B6A64(4x2 stone 色;4x1/4x4 仍用 #1B365D → 需两个资源) |
| `ic_refresh_primary.xml`(新) | 原 #1B365D 版,供 4x1/4x4 使用 |
| `ic_seal.xml` | 圆角 4dp → 6dp |
| `widget_bg_parchment.xml`(新,可选) | 羊皮纸纹理近似,失败则不做 |

## 交互链路(不变)

`randomPoem` → `WidgetContent` → 各规格 Composable;整卡 `openPoem`、↻ `RefreshAction`、♡ `FavoriteAction` 原样保留。图标换资源不涉及 clickable 变更。

## 验证

1. `assembleDebug` + `desktopTest` 编译回归(AC3)
2. 实机四规格截图 vs `design/widgets/*/screen.png` 逐项对照偏离表(AC1,需设备)
3. 交互冒烟:点击卡片进详情、↻ 换词、♡ 收藏入库(AC2)
