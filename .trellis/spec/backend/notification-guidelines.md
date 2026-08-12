# 三端通知调度约定(Android / iOS / macOS)

> 每日一词定时通知的跨端约定与踩坑记录(2026-08-13 真机实测沉淀)。

## 权限模型(三端行为不同)

| 端 | 授权行为 | 拒绝后 |
|----|---------|--------|
| Android 13+ | 每次可弹 `POST_NOTIFICATIONS` 运行时授权框 | 可再次请求;`areNotificationsEnabled()` 为唯一判定 |
| iOS / macOS | **系统只弹一次**(NotDetermined 时) | 永不弹框 → 深链引导系统设置(桌面:`open x-apple.systempreferences:com.apple.preference.notifications`) |

- 设置页进入即查 `notificationPermissionGranted()`;未授权显示可点击引导行
- 开启开关未授权 → 先请求,授权返回(ON_RESUME)自动补开+排期;拒绝则开关保持关闭
- 注意:`updateNotificationPrefs` 内部已触发 reschedule,授权返回路径不要重复调用(曾致每次聚焦重排)

## 排期模型:幂等滚动窗口

- 每次 `rescheduleDailyNotification` 都 `removeAll` + 重排未来 7 天(**不按 lastScheduledDay 推进**)
- 起点:今天时刻未过则从今天,已过则明天 → **改时间当天生效**
- `lastScheduledDay` 仅记录进度,不参与推进
- **教训**:推进式 + `lastScheduledDay=0`(首次)→ `day=1` → `dateByAddingUnit` 加负天 → 1970 年 → 全部立即触发 → 无限通知循环(2026-08-13 实测)

## macOS JNA 绑定坑(desktopMain/MacNotification.kt)

- `send()`(invokePointer)只用于**返回 id/指针**;NSInteger 返回(如 `authorizationStatus`、`longLongValue`)必须走 `sendLong()`(invokeLong),否则 `getLong(0)` 解引用整数即段错误
- ObjC block 手工构造:必须显式 `write()` + 非空 descriptor(BlockDescriptor.reserved+size),否则 `_Block_copy` 读 NULL 段错误
- block/delegate/回调对象必须**顶层常驻**(JNA 对 Callback 弱引用,局部实例 GC 后 IMP 悬垂)
- `UNUserNotificationCenter.delegate` 是 weak:实例必须持有,否则点击无回调
- **`UNNotificationResponse` 无 `request` 属性**(iOS API 混淆源)!正确链:`response.notification.request.content.userInfo`
- `dateByAddingUnit:value:toDate:options:` 的 unit 参数是 NS_OPTIONS 位:`NSCalendarUnitDay = 16`,传 1 是非法 unit → 返回 nil → components nil → `UNCalendarNotificationTrigger` 抛 NSException

## 测试边界

- JNA 依赖路径(block/回调/ObjC 调用)无法单测:测试进程 JNA 触发 NSException 终止(已知),以真机验证为准
- `gradlew :composeApp:run` 无 app bundle,`UNUserNotificationCenter` 抛 NSException → 验证必须打包 `.app` 后运行
- 纯算术逻辑(`millisUntilNext` 等)可单测
