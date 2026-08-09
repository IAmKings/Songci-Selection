# Design — 小组件对齐设计稿

## 总览

在现有四规格结构(WidgetSpec + WidgetContent)上替换渲染内容,不触碰 manifest/receiver/交互链路。

```
WidgetContent(poem, spec) → 按 spec 分支渲染设计稿布局
```

## 资源新增(res/drawable/)

| 资源 | 用途 |
|---|---|
| `widget_bg_round12.xml` | 12px 圆角 + 1dp warm-sand 边框 shape 背景(2x2/4x2) |
| `widget_bg_round28.xml` | 28px 圆角 + 1dp 边框(4x4) |
| `widget_kicker.xml`? | 顶部分色条直接用 Glance Box + background(primary),无需 drawable |
| `ic_book.xml` | 2x2 书本图标(描边 vector,primary-container) |
| `ic_refresh.xml` | 刷新图标(vector,可色) |
| `ic_favorite.xml` | 收藏心形(vector) |
| `ic_bookmark.xml` | 4x4 收藏书签(vector) |
| `ic_seal.xml`? | 4x4 印章:圆角矩形 + 红边框 drawable,内嵌"宋"Text |

图标来源:Material Symbols 描边风格手写 vector path(24dp viewport),参考 design/widgets 中使用的符号(menu_book/refresh/favorite/bookmark_border)。

## Glance 1.1.1 能力映射

| 设计元素 | Glance 手段 |
|---|---|
| 圆角+边框背景 | `GlanceModifier.background(ImageProvider(R.drawable.widget_bg_round12))`,内容 padding 撑开避免压边框 |
| 图标 | `androidx.glance.Image(provider = ImageProvider(R.drawable.ic_refresh))`,尺寸 `GlanceModifier.size(20.dp)` |
| 顶部色条 | 容器内顶部 `Box(GlanceModifier.fillMaxWidth().height(4.dp).background(primary))` |
| 竖排 | `Column { 每字一个 Text }`,词牌 28sp、作者 14sp;分隔线 `Box(width 1.dp, height 32.dp, bg warm-sand)` |
| 印章 | 4x4 头部右侧:`Box(border drawable)` + 居中"宋"字 Text(10sp,error 红) |
| 边框分隔(4x1/4x2) | 相邻区之间 `Box(1dp x fillMaxHeight, bg warm-sand)` |

## 布局要点(逐规格)

- **2x2**:Column 居中(Alignment.CenterHorizontally):icon(24dp)+ 词牌(16sp bold primary-container)+ 首句(12sp,primary-container 80% 近似 color copy)。整卡 clickable 保留。背景 round12 + padding 14dp。
- **4x1**:Row:左列(词牌 13sp bold #1B365D + 作者 10sp secondary)| 1dp 分隔 | 词句(12sp,斜体无原生支持→记录差距,可用字重/颜色近似)| 右侧 refresh 图标(20dp)。顶部 4px 色条。无圆角。
- **4x2**:Row:左 1/4(图标 24dp + "宋词选粹" 12sp primary)| 1dp 分隔 | 右侧:右上角 ↻/♯ 图标(stone 色,20dp)+ 词牌/作者行 + 词句 3 行。背景 round12,顶部色条。
- **4x4**:Column:头部(品牌 20sp #1B365D + 印章)｜ 主区 Row:词牌竖排列(每字一 Text 28sp #1B365D)+ 1dp 竖分隔线 + 词句两列(Row 内两 Column,每字一行)｜ 底部操作栏(Row:refresh + bookmark + "阅读全文 ›"文字按钮)。背景 round28,顶部色条,四角装饰线(4 个 Box 1.5dp 长, warm-sand 半透明——Glance 无半透明边框,用固定色 #E8E6DC)。

## 差异记录(不实现)

- 衬线字体:Glance 1.1.1 TextStyle 无 fontFamily → 系统默认(需 Glance 1.2+)
- 词句斜体:TextStyle 无 italic → 近似不做
- 羊皮纸纹理:需位图,不做
- 竖排词句:若 Column 逐字渲染在 4x4 高度内可行则做,否则横排两列

## 数据流/交互

不变:randomPoem → WidgetContent;整卡 clickable(OpenPoemAction)、↻(RefreshAction)、♡(FavoriteAction)。
