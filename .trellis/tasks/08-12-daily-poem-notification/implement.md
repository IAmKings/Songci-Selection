# 执行计划:daily-poem-notification

## 顺序

1. **impact 分析**(每步编辑前 MUST):Settings expect/actual、MainActivity、SettingsScreen、iOSApp.swift
2. **common 层**:
   - `NotificationSettings.kt`:设置项存取(expect/actual 扩展)+ `timeStep` 纯函数
   - `DailyNotificationCoordinator`(common):`suspend syncDailyNotifications()`——授权状态入参(Swift 查)、`randomPoems(1)` 选词、产出待排 poemId 清单与日期窗口
   - 测试:timeStep 边界 + 设置存取 roundtrip
3. **时间选择器 UI**:`TimePickerDialog.kt`(步进式,古典 token)+ SettingsScreen「每日一词」区块(开关/时间行/入口)
4. **Android**:
   - `DailyPoemWorker`(触发时:授权检查 → 选词 → 通知,点击 ACTION_VIEW songci://)
   - `DailyPoemScheduler`(enqueue/cancel,initialDelay 到设定时刻)
   - `MainActivity.onNewIntent` 补深链热启动(与 onCreate 同解析,驱动 deepLinkToken)
   - Manifest:POST_NOTIFICATIONS 权限 + 渠道
5. **iOS**:
   - Swift:`NotificationSync.swift`(滚动窗口排期,调 Kotlin 拿词)+ iOSApp.swift delegate(点击/冷启动 → songci:// URL)
6. **macOS**:
   - 引入 jna 依赖(desktop target,libs.versions.toml)
   - `MacNotification.kt`:JNA 绑定 + 滚动窗口 sync(复用 common 协调器)+ delegate 回调 → deepLinkChannel
   - main.kt 接线(初始化中心/delegate/启动 sync)
7. **验证**:
   - desktopTest 全绿(含新测试)
   - Android:装 PJZ110 → 设定时间 → 验证通知触发 + 点击深链(冷/热启动)
   - iOS:模拟器编译 + 排期逻辑走查(真机验证可选)
   - macOS:桌面端设定 → 通知触发 → 点击深链直达(热启动)
8. **commit**:feat(notification): 每日一词定时通知… + journal + gitnexus sync + archive

## 验收对照

- AC1(时间选择器视觉/无依赖)→ 步骤 3 + 人工
- AC2/AC3(Android/iOS 通知+点击深链)→ 步骤 6 设备验证
- AC4(未授权不调度)→ Worker/sync 授权门 + 设备验证
- AC5(关闭开关)→ Scheduler.cancel + 设置 roundtrip 测试
- AC6(布局不越界)→ 桌面端跑一遍设置页
- AC7(Android 触发时新词)→ Worker 逻辑走查
- AC8(既有测试全绿)→ desktopTest
