# iOS/macOS 小组件(WidgetKit)

## Goal

iOS WidgetKit 小组件(SwiftUI 扩展)实现设计稿规格:随机宋词展示,按系统版本渐进增强交互(高版本刷新/收藏/阅读全文,低版本刷新/打开)。

> **范围决策(2026-08-09)**:macOS 小组件因 Compose desktop 主程序非 Xcode SwiftUI host app,WidgetKit 扩展非常规宿主——**本轮仅 iOS**,macOS 记录为后续项(方案:jpackage 后处理嵌入或 SwiftUI host 壳)。

## Background(父任务 design.md 设计)

- 数据通道:App Group 共享容器同步完整 db 副本(17MB,复用 db_version 版本标记判新);WidgetKit 扩展 SQLite 只读打开
- 交互:高版本(iOS 17+/macOS 14+)AppIntent 按钮交互;低版本整卡链接 + timeline 定时
- 收藏:App Group 内 favorites.json(poem_id 列表),应用启动合并进 favorites 表
- 规格映射:2x2→systemSmall / 4x1→systemMedium / 4x2→systemLarge / 4x4→systemLarge(桌面 Extra Large)

## Requirements

- **R1 Xcode 工程**:iosApp 壳加 Widget Extension target(SwiftUI)+ App Group capability(group.com.songci.selection)双端(iOS/macOS)
- **R2 db 同步**:应用侧启动时比较资源 db_version vs App Group 副本,不一致重新复制(复用桌面 ~/.songci 同模式);小组件 SQLite 只读随机词查询
- **R3 规格与内容**:四规格映射,词牌+作者+词句,配色沿用 SongciColors
- **R4 版本降级交互**:高版本(iOS 17+/macOS 14+)AppIntent(刷新 timeline reload/收藏写 favorites.json/deep link 打开);低版本整卡链接 + 定时 timeline
- **R5 收藏合并**:应用启动读 App Group favorites.json → favorites 表(单向合并)

## Acceptance Criteria

- AC1:iOS/macOS 模拟器/桌面可添加小组件并随机展示词
- AC2:App Group db 同步生效(版本标记,应用更新后小组件数据更新)
- AC3:高版本系统完整交互(刷新/收藏/打开);低版本(iOS 16/macOS 13)降级路径正常
- AC4:小组件收藏在应用内可见(合并)
- AC5:主应用功能不受影响

## Out of Scope

- Windows/Android 小组件(父任务其他子任务)
- 小组件主题定制

## 依赖

- 依赖父任务前置验证(deep link 基础已就绪,App Group 为本子任务实施项);需 Xcode 工程操作(pbxproj 编辑)
