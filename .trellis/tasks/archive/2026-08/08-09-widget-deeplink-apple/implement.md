# Implement — iOS/macOS 小组件点击直达

## 执行清单

1. **common**:`SongciApp.kt` LaunchedEffect(Unit) → LaunchedEffect(initialPoemId),navigate 加 launchSingleTop(响应运行中点击,三端兼容)
2. **widget(iOS/macOS 两处 SongciWidget.swift)**:
   - `SharedDb.randomPoem()` 返回补 id;`SongciEntry` 加 `poemId: Int64`
   - 视图挂 `.widgetURL(songci://poem/{id})`(id 有效时)
3. **iOS 主应用**:
   - `app/iosApp/iosApp/Info.plist` 加 CFBundleURLTypes(songci)
   - `iOSApp.swift` onOpenURL 解析 id → 状态 → ComposeView 透传
   - `MainViewController.kt` 签名加 `initialPoemId: Long? = null` → `App(initialPoemId)`
4. **macOS 主应用**:
   - `main.kt`:`Desktop.setOpenURIHandler` 解析 → `poemId` state → `App(poemId)`
   - scheme 注册:打包后 Info.plist 注入 CFBundleURLTypes(扩展 scripts/macos-widget-deploy.sh)
5. **验证**:
   - iOS:真机 xcodebuild 安装,小组件点击 → 直达词详情
   - macOS:打包 + 部署脚本 → 点击小组件 → 直达词详情;无深链启动正常
   - desktopTest 全绿
6. **提交**:detect_changes → commit

## 验证命令

```bash
# macOS 打包验证
cd app && ./gradlew :composeApp:packageDistributionForCurrentOS
./scripts/macos-widget-deploy.sh   # 含 Info.plist 注入扩展后
# iOS
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS' build
```

## 回滚点

- 步骤 3/4 前:widget 改动独立可回退
- common 改动(步骤 1)影响 Android,iOS 验证失败时 git checkout 该文件即可

## Review Gate

- 步骤 5:iOS/macOS 实机点击验证通过 → 提交
- macOS Desktop.setOpenURIHandler 若 jpackage 下无效 → 回退方案:启动参数(argv)解析,prd 记录
