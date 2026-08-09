# iOS/macOS 小组件点击直达词详情

## Goal

iOS 与 macOS 桌面小组件支持点击直达对应词详情(与 Android 的 OpenPoemAction 深链行为一致),补齐 widget 层(点击)与主应用层(scheme 注册 + 深链处理)全链路。

## 现状(已查证)

- widget 层:iOS/macOS 的 SongciWidget.swift 均无任何点击动作(纯展示),且 `SongciEntry` 无 poemId 字段
- 主应用层:common `App(initialPoemId: Long? = null)` 已支持深链导航(Android 已验证);iOS 壳 `MainViewController() → App()` 不透传;macOS `main() → App()` 不透传,无 scheme 注册

## 要求

| 端 | widget | 主应用 |
|---|---|---|
| iOS | `.widgetURL(songci://poem/{id})` | Info.plist 注册 songci scheme;`onOpenURL` 解析 → MainViewController(initialPoemId) 透传 |
| macOS | `.widgetURL(songci://poem/{id})` | jpackage 产物 Info.plist 注入 CFBundleURLTypes;`Desktop.setOpenURIHandler` 解析 → App(initialPoemId) 重组合 |

## Acceptance Criteria

- AC1:iOS 小组件点击打开主应用并直达对应词详情(真机验证)
- AC2:macOS 桌面小组件点击打开主应用并直达对应词详情(本机验证)
- AC3:无深链启动(正常打开 app)行为不变,词详情导航不回归(desktopTest)
- AC4:构建产物(iosApp 构建 + compose 打包)正常

## 非目标

- 不动 Android(已实现)
- 不改 widget 视觉/布局(本任务只加点击)
- 不处理 app 已运行时的窗口焦点/单实例策略(首次激活场景,后续需要再说)
