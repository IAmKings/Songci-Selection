# Design — Android 四规格独立小组件

## 总览

4 个独立 `AppWidgetProvider` 条目,共享一套内容渲染代码(规格参数化),数据通道不变(同进程直读 db)。

```
manifest → 4 个 receiver → 4 个 GlanceAppWidget 子类 → 共享 content(spec) → 同进程 db
```

## 组件分解

### 1. 共享内容函数(重构 `SongciWidget.kt`)

现有 `SongciWidget.provideGlance` 内按 `LocalSize` 分支 → 抽为共享函数,spec 参数驱动:

```kotlin
enum class WidgetSpec(val label: String, val wide: Boolean, val tall: Boolean) {
    Small(false, false),      // 2x2  词牌·作者 + 首句
    Banner(true, false),      // 4x1  横幅:词牌·作者 + 一句
    Medium(true, true),       // 4x2  多行词句 + ↻/♡
    Large(true, true),        // 4x4  全文 + ↻/♡ + 阅读全文
}
```

- `Large` 与 `Medium` 内容差异:行数(3 行 vs 全文 lines)、是否有"阅读全文 ›"
- 每规格独立的 Glance widget 子类,`SizeMode.Single`(固定规格,不再 Responsive):

```kotlin
class SongciWidgetSmall : GlanceAppWidget() { override val sizeMode = SizeMode.Single; override suspend fun provideGlance(...) = renderContent(context, WidgetSpec.Small) }
```

- 刷新/收藏 action 复用;`updateAll` 需逐类调用(4 类各更新自己的实例)——`RefreshAction` 改为遍历 4 类更新
- 整卡点击、深链、ColorProvider(Color()) 写法、AppContextHolder 兜底全部保留

### 2. 4 个 provider XML(`res/xml/`)

| XML | minWidth/minHeight | targetCell | label |
|---|---|---|---|
| songci_widget_small.xml | 110/110dp | 2×2 | 宋词选粹·小 |
| songci_widget_banner.xml | 250/60dp | 4×1 | 宋词选粹·横幅 |
| songci_widget_medium.xml | 250/170dp | 4×2 | 宋词选粹·中 |
| songci_widget_large.xml | 250/320dp | 4×4 | 宋词选粹·大 |

> minHeight 为**实测反推值**:添加面板规格 = `ceil(minHeight / cellHeight)`,本机(ColorOS,cellHeight≈87-104dp)直接写设计值 110/250/520 会得到 2/3/6 格。60/170/320 落在 (0,95]/(95,190]/(285,380] 区间内稳 → 实测显示 4x1/4x2/4x4。**不同设备 cellHeight 不同,可能 ±1 格**;如需通用精确,应改用 AndroidX 文档公式按 screenHeight/rows 计算。

- 均 `initialLayout=@layout/glance_default_loading_layout`、`resizeMode="none"`(固定规格,禁止拖拽改变语义)
- `widgetCategory="home_screen"`,`updatePeriodMillis=0`(沿用现状)

### 3. Receiver + manifest

- 4 个 receiver:复用 GlanceAppWidgetReceiver 模式,各绑一个 widget 实例
- 旧 `SongciWidgetReceiver` 改名/保留:2x2 条目沿用旧 receiver 名可省 manifest 变更,但新命名更清晰——**保留旧名**绑定 Small(避免已添加组件残留后失效),新增 3 个

## 兼容与风险

- 旧 2x2 组件(已添加的):receiver 路径不变 → 继续工作;卸载重装后走新条目
- Glance 1.1.1 已知坑(记录在 spec,不重复):ColorProvider 资源 ID、Application context
- `SizeMode.Single` 在 Glance 1.1.1 稳定(Responsive 也是其子集)
- 4x1 横幅内容放不下时截断策略:词句单行 + 省略

## 数据流

不变:widget 渲染时 `SongciDb(createDatabaseDriver())` 同进程直读;db 由主应用启动时从 assets 复制(版本标记判新)。
