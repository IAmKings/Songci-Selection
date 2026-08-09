# 桌面小组件实施计划

## 前置(父任务)

1. **数据通道验证**(Android 直读 + Apple App Group 机制):
   - [ ] 验证 Glance 在应用进程内可复用 SongciRepository(原型:Glance 渲染随机词)
   - [ ] Xcode 创建 App Group capability(`group.com.songci.selection`),确认 WidgetKit 扩展可读
   - [ ] deep link scheme 三端注册验证(`songci://poem/{id}`)
2. **父任务 prd 定稿**(验收总纲) + `task.py start`

## 子任务 A:iOS/macOS WidgetKit(08-09-widget-apple)

- [ ] Xcode 新建 Widget Extension(SwiftUI),注册 App Group
- [ ] 共享 db 同步:应用侧复制 db → App Group(复用 db_version 判新)
- [ ] 随机词查询(SQLite 只读)+ timeline provider
- [ ] 规格适配:systemSmall/Medium/Large(设计稿 2x2/4x1/4x2/4x4 内容映射)
- [ ] 高版本交互:AppIntent(刷新/收藏)+ deep link 阅读全文
- [ ] 低版本降级:整卡链接 + timeline 定时
- [ ] 收藏文件合并:应用启动读 App Group favorites.json → favorites 表
- [ ] 验证:模拟器双版本(iOS 16/17)+ macOS 桌面添加小组件

## 子任务 B:Android Glance(08-09-widget-android)

- [ ] 引入 Glance 依赖 + AppWidgetProvider 注册
- [ ] 复用 SongciRepository 随机词/收藏(同进程直读)
- [ ] 规格适配:2x2/4x1/4x2/4x4 四布局(设计稿配色/内容)
- [ ] 交互:刷新按钮 + 收藏按钮 + 点击 deep link 打开应用
- [ ] 验证:API 24 模拟器 + API 33 实机(两版本档)

## 验证命令

- Android:`./gradlew :composeApp:assembleDebug` + 模拟器装 widget
- iOS/macOS:`xcodebuild -project iosApp/iosApp.xcodeproj` 模拟器运行 + 桌面添加小组件
- 三端:`./gradlew :composeApp:desktopTest`(数据层回归不受影响)

## 风险与回滚

- 风险:App Group db 同步时序(应用未启动时小组件读旧副本)——版本标记兜底,可接受
- 风险:Glance 版本与 CMP 1.11 兼容性——先原型验证再全量
- 回滚:小组件为独立扩展/组件,移除不影响主应用;db 副本删除即回退
