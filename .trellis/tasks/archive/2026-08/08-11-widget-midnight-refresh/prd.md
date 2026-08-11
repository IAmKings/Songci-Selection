# 凌晨小组件自动更新

## Goal

三端小组件（Android Glance / iOS WidgetKit / macOS WidgetKit）每日凌晨自动刷新为新的随机诗，无需用户启动应用。

## Requirements

- R1: 每日凌晨自动刷新小组件内容（随机抽取一首词），与每日推荐池**无关**
- R2: 刷新不依赖用户打开应用——系统自主调度执行
- R3: 三端覆盖：Android / iOS / macOS
- R4: 保留现有手动刷新能力（Android 刷新按钮、深链、收藏交互不受影响）

## Acceptance Criteria

- [ ] AC1: 凌晨到达后（无需打开应用），三端 widget 自动显示新随机诗
- [ ] AC2: Android 端凌晨刷新对齐系统时间，跨天、跨重启（设备重启后凌晨闹钟仍生效）
- [ ] AC3: iOS/macOS 端 Timeline 策略指向凌晨时刻，widget 到期自动刷新
- [ ] AC4: 现有手动刷新、深链点击、收藏交互回归通过
- [ ] AC5: 既有测试全绿（Android unit/desktop 测试）

## Out of Scope

- 与每日推荐池联动（明确无关）
- widget 内容多样性策略（保持随机）
- Android 精确闹钟权限的 UI 引导（系统默认行为即可）

## Notes

- 当前现状：Android 无任何定时机制（仅手动刷新）；iOS/macOS 每小时 timeline 刷新（随机诗），改为凌晨触发
