# 凌晨小组件自动更新 — 实施计划

## 前置

- 确认最新 androidx.work 稳定版（目标 2.10.x，若网络有变以 catalog 解析为准）

## 实施清单（有序）

1. **依赖**: `app/gradle/libs.versions.toml` 加 `androidx-work = { module = "androidx.work:work-runtime-ktx", version = "<稳定版>" }`；`build.gradle.kts` androidMain 加 `implementation(libs.androidx.work)`
2. **Android: 新增 MidnightRefreshWorker + 调度函数**
   - 新文件 `app/composeApp/src/androidMain/kotlin/com/songci/app/widget/MidnightRefresh.kt`
   - `scheduleNextMidnight(context)`：kotlinx-datetime 算本地时区下一 0 点 → `enqueueUniqueWork("midnight-refresh", REPLACE, OneTimeWorkRequest.setInitialDelay(...))`
   - `MidnightRefreshWorker.doWork()`：四规格 `updateAll()` + 尾部重排
3. **Android: 挂载点** — `SongciApp.onCreate()` 调 `scheduleNextMidnight`
4. **iOS: `iosApp/WidgetExtension/SongciWidget.swift`** — policy 改 `.after(nextMidnight())`，新增 `nextMidnight()`
5. **macOS: `MacWidgetExtension/SongciWidget.swift`** — 同步改

## 验证命令

```bash
# Android 编译 + 单测
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest

# desktop 测试（nextMidnightMillis 跨平台纯函数，可加断言）
./gradlew :composeApp:desktopTest

# iOS/macOS 类型检查（本机缺 iOS runtime，swiftc 单文件 typecheck）
swiftc -typecheck -parse-as-library app/iosApp/iosApp/WidgetExtension/SongciWidget.swift
```

## 风险文件

- `SongciWidget.kt` 无改动（worker 只调现有 updateAll）；新增 `MidnightRefresh.kt`
- `SongciApp.kt`（加一行调度调用）
- `libs.versions.toml` / `build.gradle.kts`（依赖两行）
- 两个 Swift 文件（仅 policy 行 + 辅助函数）

## 完成检查

- [ ] `gitnexus_detect_changes()` 确认改动范围仅为 widget 相关
- [ ] 四规格 widget 现有 AC（深链/收藏/手动刷新）回归
- [ ] 编译 + 测试全绿
