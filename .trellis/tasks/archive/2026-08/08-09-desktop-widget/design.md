# 桌面小组件技术设计

## 架构总览

```
┌─────────────────────────────────────────────────────┐
│ 数据层(共用)                                          │
│  db/songci.db 21,340 首 + 应用 SQLDelight 查询       │
└──────────────┬──────────────────┬───────────────────┘
               │                  │
   Android 进程内直读          App Group 共享容器
   (Glance 同进程)              (iOS/macOS WidgetKit 扩展)
               │                  │
        ┌──────┴──────┐    ┌──────┴──────┐
        │ Glance Widget│    │ WidgetKit    │
        │ 4 规格       │    │ SwiftUI      │
        └─────────────┘    └─────────────┘
```

## 数据通道

| 平台 | 通道 | 说明 |
|---|---|---|
| Android | **进程内直读 db** | Glance composable 运行在应用进程,复用现有 `SongciDb`/Repository 查询随机词、收藏写回 favorites 表——零复制 |
| iOS/macOS | **App Group 共享 db 副本** | 应用首启/更新时复制 db → App Group 容器(`group.com.songci.selection/songci.db`),复用 `db_version.txt` 版本标记判新;WidgetKit 扩展以 SQLite 只读打开 |

- **随机词**:`ORDER BY RANDOM() LIMIT 1`(21k 行全表扫 ~20ms,小组件刷新低频可接受);Android 复用 `randomPoems`,Apple 扩展内直查
- **版本同步(iOS/macOS)**:应用侧启动时比较资源 db_version vs App Group 副本版本,不一致则重新复制(与现有 `~/.songci` 缓存同模式)

## 交互(渐进增强)

| 交互 | Android(无版本限制) | iOS/macOS 高版本(17+/14+) | iOS/macOS 低版本 |
|---|---|---|---|
| 刷新换词 | 按钮点击(Glance action) | Button(AppIntent timeline reload) | 整卡点击/定时 timeline |
| 收藏 | 直写 favorites 表 + UI 态更新 | AppIntent 写 App Group 收藏文件 | 降级:打开应用 |
| 阅读全文 | 点击 → deep link `songci://poem/{id}` | 同 deep link | 同 deep link |

- **deep link**:应用注册 `songci://poem/{id}` scheme(三端);小组件链接打开应用对应词作
- **收藏文件(iOS/macOS)**:App Group 内 `favorites.json`(poem_id 列表),应用启动时合并进 favorites 表(单向:小组件收藏 → 应用)

## 规格映射

| 设计稿 | Android | iOS/macOS |
|---|---|---|
| 2x2 | 2×2 单元格 | systemSmall |
| 4x1 | 4×1 单元格 | systemMedium |
| 4x2 | 4×2 单元格 | systemLarge(适配) |
| 4x4 | 4×4 单元格 | systemLarge 扩展(Extra Large, macOS 桌面) |

内容:词牌 + 作者 + 词句(2x2 极简仅标题+图标);配色沿用 SongciColors(纸张底/书生蓝/墨色)。

## 关键权衡

- **Android 直读 db** vs 共享文件:直读零同步成本(同进程),但小组件刷新时 db 可能被写(只读连接安全)
- **iOS/macOS 完整 db 副本**(17MB App Group) vs 精选集:用户选完整词库——存储成本 17MB 可接受,换取任意词随机
- **版本降级交互**:按系统能力渐进增强,低版本不损失核心(展示+打开)

## 兼容与风险

- Android Glance 要求 minSdk 23+(项目 minSdk 24 ✓)
- WidgetKit 交互式小组件(iOS 17+/macOS 14+):低版本自动走降级路径(编译期 availability 判断)
- App Group 需 Xcode capability 配置(iOS/macOS 同 group id)
- 风险:App Group db 同步时序(应用未启动时小组件读旧副本——版本标记兜底,显示旧数据可接受)
