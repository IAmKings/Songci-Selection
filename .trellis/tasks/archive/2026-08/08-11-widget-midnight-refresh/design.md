# 凌晨小组件自动更新 — 技术设计

## 现状

| 平台 | 内容源 | 刷新机制 |
|------|--------|----------|
| Android | `randomPoem()` 同进程直读 db | 仅手动按钮(RefreshAction → updateAll) |
| iOS/macOS | `SharedDb.randomPoem()` 直查 App Group db | Timeline policy `.after(+3600s)` 每小时 |

与每日推荐池无任何关联（R1）。db 是预建库，随机诗读取不依赖运行时生成，**无同步时序问题**。

## 方案

### Android: WorkManager（用户决策）

- **为什么不是 updatePeriodMillis**：最小 30 分钟，无法对齐凌晨。
- **为什么不是 AlarmManager**：需手动 BOOT_COMPLETED 重挂 + RECEIVE_BOOT_COMPLETED 权限；WorkManager 内置重启恢复。
- **为什么不需要精确闹钟**：用户明确"0 点附近，不要求精确"，WorkManager 延迟任务无权限墙。

```
[新增] MidnightRefreshWorker : CoroutineWorker
  └─ doWork():
       ├─ 四规格 GlanceAppWidget.updateAll(applicationContext)  // 重随机
       └─ scheduleNextMidnight(applicationContext)              // 尾部重排下一个凌晨

[新增] scheduleNextMidnight(context)
  └─ WorkManager.enqueueUniqueWork(
       "midnight-refresh", ExistingWorkPolicy.REPLACE,
       OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
         .setInitialDelay(nextMidnightMillis() - now, MILLISECONDS))

nextMidnightMillis(): 本地时区下一个 0 点 epoch millis（kotlinx-datetime 已依赖，跨平台纯函数）
```

- `enqueueUniqueWork + REPLACE`：防重复入队堆积（app 多次启动不叠加任务）
- 重启恢复：WorkManager 内置（BOOT_COMPLETED 由 work 库处理），**无需 RECEIVE_BOOT_COMPLETED 权限、无需自定义 receiver**（对比 AlarmManager 少一个组件）
- 权限：**零新增权限**，无任何弹窗场景

挂载点（一处 + worker 自续）：
1. `SongciApp.onCreate()` — 首次入队（REPLACE 幂等，重复启动安全）
2. worker 内尾部重排 — 每日循环

manifest 变更：**无**（WorkManager 自动初始化 + 自动注册）

### iOS/macOS: Timeline policy 指向凌晨（各一行）

`getTimeline` 中 policy 从 `.after(Date().addingTimeInterval(3600))` 改为 `.after(nextMidnight())`：

```swift
private func nextMidnight() -> Date {
    Calendar.current.nextDate(
        after: Date(),
        matching: DateComponents(hour: 0, minute: 0),
        matchingPolicy: .nextTime
    ) ?? Date().addingTimeInterval(86400)
}
```

系统到点自动调 `getTimeline` 换随机诗——**天然无需 app 运行**（R2 原生满足）。
两个文件同步改：`iosApp/WidgetExtension/SongciWidget.swift` + `MacWidgetExtension/SongciWidget.swift`。

## 边界与权衡

- WorkManager 延迟任务在 Doze 下可能延迟到凌晨数小时——用户接受"0 点附近"，内容仍是"新一天的随机诗"。
- 新依赖 androidx.work（~200KB）——用户对比后选择，换来重启自动恢复 + 现代 API + 无自定义组件。
- 设备重启后首次触发：work 库自动恢复待执行任务，凌晨已过则执行后从新时刻重排（不补当天）。
- 时区切换：`nextMidnightMillis()` 每次执行时用当前时区计算，重排即纠正。

## 回滚

删除 worker + 调度调用 + catalog/build 依赖两行即回滚；iOS/macOS 还原 policy 一行。
