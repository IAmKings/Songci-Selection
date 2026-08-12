# 设计:每日一词通知

## 数据流

```
设置页(时间选择器) → 设置项持久化(expect/actual Settings)
  → 调度器(平台) → 系统定时 → 触发(选词:repo.randomPoems(1))
  → 通知(词牌·作者 + 首句) → 点击 → songci://poem/{id} 深链(三端通道现成)
```

## 设置项持久化(扩展 Settings expect/actual)

沿用现有 fontScale/fontStyle 模式(Android SharedPreferences / iOS NSUserDefaults / desktop properties):

- `notification_enabled: Boolean`(默认 false)
- `notification_hour: Int`(0-23)
- `notification_minute: Int`(0-59)
- `notification_last_scheduled_day: Long`(iOS 滚动窗口标记,epoch day;Android 不需要)

## 选词(两端统一)

`repo.randomPoems(1)`——SQL 已滤异常字符(⿰/缺字/ⱬ长词牌/单行),与首页/小组件同源规范。Android 触发时调用;iOS 排期时逐条调用。

## Android

- **调度**:`WorkManager.enqueueUniquePeriodicWork("daily-poem", KEEP)`:
  - `PeriodicWorkRequest` 24h 周期,`initialDelay` = 距下次设定时刻的时长(设置变更时 `cancelUniqueWork` + 重排)
  - 注意:Doze 下触发可能延迟数分钟~十几分钟(内容型推送标准代价,已与用户确认接受)
- **Worker**(CoroutineWorker):先查 `NotificationManagerCompat.areNotificationsEnabled()` → 未授权直接 return(不通知);授权则随机选词 → 构建 `NotificationCompat`(渠道 "daily_poem",点击 `PendingIntent.getActivity(Intent(ACTION_VIEW, songci://poem/{id}))`)
- **权限**:Android 13+ 需 `POST_NOTIFICATIONS` 运行时权限(首次设置时请求一次,拒绝即不调度并显示提示);`areNotificationsEnabled()` 兜底(13 以下恒 true)
- **深链补缺**:`MainActivity` 补 `onNewIntent(intent)`:解析 intent.data 与 onCreate 同逻辑,并更新 Compose 层 initialPoemId(经 App 的 deepLinkToken 机制——具体:MainActivity 持有可更新的状态,通知点击热启动时走到 openPoem)。注意与 widget 深链共用此修复。

## iOS

- **滚动窗口**:app 启动/设置变更时(前台任意入口调用一个 `syncDailyNotifications()`):
  1. 查授权(`getNotificationSettings` → authorized,否则清除 pending 并返回)
  2. 从 `notification_last_scheduled_day` 起,补排到 today+7(每天一条):
     - `UNCalendarNotificationTrigger(dateMatching: hour/minute, repeats: false)`(未来日期)
     - 内容:`rhythmic · author` + 首句;`userInfo["poemId"]`
     - 每天一条,各随机选词
  3. 更新 last_scheduled_day
- **点击**:`iOSApp.swift` 设 `UNUserNotificationCenter.delegate` → `didReceive response` → 取 userInfo poemId → `UIApplication.shared.open(songci://poem/{id})`(复用 onOpenURL 路径);**冷启动**:`launchOptions[.notification]` 同样转 URL(在 App init/ContentView 建立前处理,或经 initialPoemId 通道)
- **系统删除通知开关**(设置里关通知):下次启动 sync 时授权检查为 denied → 清 pending ✓

## 时间选择器(自研,Classical Manuscript 风格)

- 入口:SettingsScreen 新增「每日一词」区块:开关 + 当前时间("每日 21:00")+ 点击时间行打开选择器
- 组件:自研 `TimePickerDialog`(不走 Material3 TimePicker——现代圆盘风格与古典不搭):
  - **步进式**:时/分两组「− 时间 +」按钮 + 大字显示(00-59 循环、00-23 循环),像翻页钟
  - 风格 token:`SongciColors.background` 底、楷体(主题 fontStyle)、`stone` 辅助、`primary` 强调、边框用 `line`(与 PoemCard 一致的 1dp 边框)
  - 确定/取消;确定 → 保存设置 → 触发重调度
- 步进边界逻辑抽纯函数(可测):`timeStep(hour, minute, deltaMin)` 处理跨小时进位与 23:59→00:00 循环

## 平台接线(新文件/改动)

| 平台 | 文件 |
|------|------|
| common | `data/NotificationSettings.kt`(设置项存取 + 纯函数 timeStep)+ `ui/screens/TimePickerDialog.kt` + SettingsScreen 区块 |
| Android | `notification/DailyPoemWorker.kt` + `notification/DailyPoemScheduler.kt` + MainActivity.onNewIntent + AndroidManifest(渠道常量在 Worker) |
| iOS | `iOSApp.swift`(delegate + 冷启动)+ `NotificationSync.swift`(滚动窗口,调 Compose 层选词?) |

**iOS 选词跨层问题**:选词在 Compose(Kotlin)侧(`repo.randomPoems`),而 UNUserNotificationCenter 在 Swift 侧——K/N 导出 MainViewController 已有跨层桥;方案:Swift 调 `MainViewControllerKt` 暴露的 suspend 选词接口(Compose 层提供 `suspend fun randomPoemForNotification(): Poem?`),或由 Compose 层在 sync 时把 7 条词一次性传给 Swift(经桥)。**倾向后者**:Compose 侧 `DailyNotificationCoordinator` 提供 `suspend fun syncDailyNotifications()`(内部:查授权状态由 Swift 返回、取词、把 [poemId 清单] 交给 Swift 排期)——减少 Swift 侧 SQL 逻辑,选词规范保持在 Kotlin 一处。

## macOS(JNA 绑定 UNUserNotificationCenter)

纯 JVM 侧调 AppKit,`net.java.dev.jna:jna`(desktop 依赖):

- **绑定面**(`notification/MacNotification.kt`,desktopMain):
  - `UNUserNotificationCenter.current()`
  - `requestAuthorizationWithOptions` / `getNotificationSettingsWithCompletionHandler`(查 authorized——异步回调转 suspend)
  - `addNotificationRequest`(UNMutableNotificationContent + UNCalendarNotificationTrigger 每日触发,非重复)
  - `removePendingNotificationRequests`
  - **delegate 回调**:`UNUserNotificationCenterDelegate.didReceiveNotificationResponse` → JNA Callback 桥 → `deepLinkChannel.trySend(poemId)`(**main.kt 现有通道,零改动接上**)
- **滚动窗口**:与 iOS 同构——启动/设置变更时 `syncDailyNotifications()`:查授权 → 补排未来 7 天(每天一条,各随机选词)→ 更新 last_scheduled_day
- **点击冷启动**:app 未运行时点击通知,macOS 启动 app 后系统会重新投递响应;delegate 注册晚于启动可能漏接——桌面 app 常驻,先做热启动,冷启动标记为已知近似(ponytail 注释)
- **授权请求时机**:首次设置时由设置页触发(与 Android/iOS 一致的交互)

## 测试

- desktopTest:`timeStep` 边界(00:00-1min→00:01?跨天、23:59+1→00:00);NotificationSettings 存取 roundtrip(桌面 properties);`randomPoems` 异常过滤已有回归
- 平台侧(不可自动化):Android 设备通知触发/点击深链;iOS 模拟器排期/点击;macOS 通知触发/点击深链(热启动)
- JNA 绑定代码不写测试(系统 API 桥,走设备验证)
