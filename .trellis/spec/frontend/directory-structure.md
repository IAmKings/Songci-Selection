# Directory Structure

> UI 层(Compose Multiplatform)组织方式。

---

## Overview

- 全部 UI 在 `commonMain`(三端共用),平台差异走 expect/actual
- 分层:导航(SongciApp) → 屏幕(screens) → 复用组件(components) → 主题(theme)
- 数据访问经 AppViewModel(单 ViewModel 注入 Repository)

---

## Directory Layout

```
ui/
├── SongciApp.kt        # 导航宿主(10 路由) + 首启加载闸门 + 自适应(768dp 断点)
├── AppViewModel.kt     # 单一 ViewModel:StateFlow 列表 + mutableStateOf 交互态
├── PathUtil.kt         # 路由参数百分号编码(commonMain 无 java.net.Uri)
├── screens/
│   ├── IndexScreens.kt     # 目录索引/朝代/作者/词牌/词牌详情(含格律卡片)
│   ├── DetailScreen.kt     # 词作详情(窄屏单栏/宽屏双栏,上下阕分栏)
│   ├── HomeScreen.kt / SearchScreen.kt / FavoritesScreen.kt / SettingsScreen.kt
├── components/
│   ├── Common.kt           # SimpleListScreen / TextRowList / PoemList / PoemCard / EmptyState
│   └── StanzaColumn.kt     # (详情双栏阕列)
└── theme/
    └── Theme.kt            # SongciColors 设计 token(Classical Manuscript)
```

---

## 约定

- 新屏幕复用 `SimpleListScreen`(标题栏 + 内容槽),不自绘标题栏(详情页有特例:格律卡片需 LazyColumn 整页滚动)
- 组件放 components/,屏幕专属组件留在 screens/ 文件内(如 RhythmicCard 私有)
- 路由参数中文必须 `encodePath`(Navigation 自动解码)
