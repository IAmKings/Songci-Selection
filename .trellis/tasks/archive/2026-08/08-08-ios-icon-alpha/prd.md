# iOS 图标去除 alpha 通道(App Store 合规)

## Goal

消除 trellis-check 遗留项:iOS AppIcon.png 目前为含透明通道的圆角卡面(RGBA),App Store 提交校验会拒绝含 alpha 的图标。改为全出血不透明图。

## Background

- 圆角卡面(1024/824/185 + 透明边距)是为 macOS Dock 设计;iOS 系统自带 squircle 蒙版切圆角,预烘焙圆角对 iOS 是冗余且引入 alpha
- 源图 `design/app-icon/screen.png` 为 RGB 无 alpha,直接可用
- 桌面端 icns/ico/png 继续用圆角卡面,不动

## Requirements

- **R1**:iOS `AppIcon.png` 使用源图全出血 RGB(无 alpha 通道),与 macOS/Windows 产物的圆角卡面区分
- **R2**:`gen_app_icons.py` 中 iOS 分支改为输出 RGB 全出血图;脚本其余逻辑不变
- **R3**:重新生成后 iOS 图标经 `sips` 验证无 alpha;脚本仍幂等

## Acceptance Criteria

- AC1:重新生成后 `AppIcon.png` 为 RGB(无 alpha 通道),1024×1024
- AC2:`xcrun actool` 编译 Assets.xcassets 通过
- AC3:脚本重跑幂等(git diff 无变化);桌面/Android 产物字节不变

## Out of Scope

- adaptive 前景单密度(Android 官方允许,系统缩放,无异常风险——仅记录不做改动)
- App Store 上架全流程
