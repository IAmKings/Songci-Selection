# 桌面小组件开发(基于 widgets 设计稿)

## Goal

根据 design/widgets/ 设计稿开发桌面小组件:桌面展示宋词精选(词牌+作者+词句),支持刷新/收藏/阅读全文。

## 已确认事实(仓库证据)

- 设计稿:design/widgets/{2x2,4x1,4x2,4x4}/ —— 4 种规格,HTML 预览 + screen.png(706×1600)
  - 2x2:标题 + 书本图标(极简)
  - 4x1:词牌+作者+词句(横条)
  - 4x2:词牌+作者+词句 + 刷新/收藏按钮(「桌面小组件」标注)
  - 4x4:宋词精选 + 词牌+作者+词句 + 刷新/收藏/阅读全文
  - 内容模型:随机/精选一首词,刷新换一首,收藏,阅读全文(打开应用)
- 尺寸规格 2x2/4x1/4x2/4x4 为 **WidgetKit 标准规格**(Apple 生态)
- 应用栈:Compose Multiplatform(Android/iOS/macOS),db 21,340 首(SQLite),SQLDelight
- **技术约束**:Compose Multiplatform 无系统小组件 API;macOS 系统小组件需 **WidgetKit 扩展(SwiftUI)**,与 Compose 应用经 **App Group** 共享数据;Windows 小组件为不同机制(WebView2/Adaptive Cards,生态不成熟)

## 需求(已收敛)

- **平台**:iOS + macOS(WidgetKit/SwiftUI 扩展)+ Android(App Widget)三平台;Windows 后续处理
- **数据来源(完整词库共享)**:Android 小组件运行在应用进程**直读应用 db**(21,340 首);iOS/macOS 经 **App Group 共享容器**同步完整 db 副本(17MB,复用现有 db_version 版本标记机制同步)
- **交互(按系统版本渐进增强)**:
  - 高版本(iOS 17+/macOS 14+):完整交互——刷新换词 + 收藏(写回应用)+ 点击阅读全文(deep link 打开应用)
  - 低版本:刷新(定时/点击整卡)+ 打开应用;收藏降级为打开应用内收藏
  - Android:Glance 交互无系统版本限制(Android 12+ 小组件均支持按钮)
- **规格**:设计稿 4 规格全实现(2x2/4x1/4x2/4x4),iOS/macOS 映射 systemSmall/Medium/Large
- **内容模型**:随机一首词(词牌+作者+词句),刷新换一首

## Acceptance Criteria

- AC1:三平台小组件可添加并显示随机词(词牌+作者+词句),4 规格/映射规格齐全
- AC2:Android 小组件直读应用 db(21,340 首随机);iOS/macOS 经 App Group 共享 db(版本标记同步)
- AC3:高版本(iOS 17+/macOS 14+)完整交互(刷新/收藏/阅读全文);低版本降级(刷新/打开)
- AC4:小组件收藏写回应用(Android 直写 favorites;Apple 经 favorites.json 合并)
- AC5:主应用功能不受影响(desktopTest 全绿,三端构建通过)

## Out of Scope

- Windows 小组件(后续)
- 小组件主题定制/多风格
- 小组件展示历史/统计

## 任务结构

- 父任务(本任务):数据通道/交互规范/验收总纲/集成验证
- 子任务 08-09-widget-apple:iOS/macOS WidgetKit(SwiftUI 扩展)
- 子任务 08-09-widget-android:Android Glance(App Widget)
- 依赖:两子任务均依赖父任务数据通道前置验证(App Group capability/deep link scheme)
