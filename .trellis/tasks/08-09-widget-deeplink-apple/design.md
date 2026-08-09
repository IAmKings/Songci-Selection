# Design — iOS/macOS 小组件点击直达

## 总览

```
widget(.widgetURL) → 系统 open songci://poem/{id}
→ 主应用(URL scheme 注册)接收 → 解析 id → App(initialPoemId) → 词详情
```

## 1. Widget 层(iOS + macOS,两处 SongciWidget.swift)

- `SongciEntry` 增加 `poemId: Int64`;`SharedDb.randomPoem()` 返回值补 id
- `SongciWidgetView` 根视图挂 `.widgetURL(URL(string: "songci://poem/\(entry.poemId)"))`(macOS 12+/iOS 14+)
- fallback:poemId 无效(0)时不挂 widgetURL(避免打开后无定位)

## 2. iOS 主应用

- **scheme 注册**:`app/iosApp/iosApp/Info.plist` 加 CFBundleURLTypes(songci,CFBundleURLName com.songci.app)
- **接收**:`iOSApp.swift` `onOpenURL { url in ... }` → 解析 `songci://poem/{id}` → 状态存 `poemId: Int64?` → 传给 ContentView/ComposeView
- **透传**:`MainViewController.kt` 签名改 `fun MainViewController(initialPoemId: Long? = null)` → `App(initialPoemId)`;Swift 侧 `MainViewControllerKt.MainViewController(poemId: ...)` 调用
- 注意:widget 点击可能冷启动 app,onOpenURL 在 App 层触发;ComposeView 需在 make 时拿 initialPoemId

## 3. macOS 主应用

- **接收**:`main.kt` 中 `Desktop.getDesktop().setOpenURIHandler { e -> e.uri 解析 id }`(Java 9+,AWT 栈,CMP 可用)
- **传递**:application 内 `var poemId by mutableStateOf<Long?>(null)`;handler 更新 poemId;`App(poemId)` 组合时传入——**App 组件需对 initialPoemId 变化响应**(导航到词详情;确认 common App 的 initialPoemId 用法,若仅首次生效则需调整)
- **scheme 注册**:jpackage 产物后处理——Info.plist 注入 CFBundleURLTypes(扩展 scripts/macos-widget-deploy.sh 或在打包流程加一步;与现有签名重签流程同位置)

## 4. common App(initialPoemId) 复核

- 确认 App.kt 对 initialPoemId 的处理:仅首次 or 可响应变化;macOS 运行中点击需可响应 → 必要时改为 LaunchedEffect(initialPoemId) 导航

## 风险

- Desktop.setOpenURIHandler 在 jpackage app 的行为(需实测;失败回退:启动参数解析 `-Dcom.apple...`/argv)
- iOS onOpenURL 冷启动时序(需真机实测)
- macOS Info.plist 注入后必须重签(现有脚本流程已覆盖)
