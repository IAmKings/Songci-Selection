# Implement — Android 四规格独立小组件

## 执行清单

1. **重构 `SongciWidget.kt`**
   - `WidgetSpec` 枚举(4 规格:label/wide/tall/行数)
   - 共享 `renderContent(context, spec)`(从现有 provideGlance 抽取,按 spec 分支)
   - 4 个 `GlanceAppWidget` 子类(`SizeMode.Single`)+ 4 个 receiver(Small 沿用 `SongciWidgetReceiver` 名,新增 Banner/Medium/Large)
   - `RefreshAction` 改为逐类 `updateAll`(4 类)
2. **新增 4 个 provider XML**:`res/xml/songci_widget_{small,banner,medium,large}.xml`(删旧 `songci_widget_info.xml`)
3. **manifest**:4 个 receiver 注册,meta-data 指向新 XML
4. **构建验证**:`assembleDebug` 通过
5. **实机验证**(连接设备):
   - 添加面板出现 4 条目,各自预览/尺寸
   - 各规格渲染内容正确
   - 整卡点击直达词详情;↻/♡ 生效(中/大)
6. **回归**:`desktopTest` 全绿
7. **提交**:detect_changes → commit

## 验证命令

```bash
cd app && ./gradlew :composeApp:assembleDebug
./gradlew :composeApp:desktopTest
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## 回滚点

- 步骤 3 前:改 XML/receiver 均可随时回退(manifest 未动)
- 步骤 7 前:全部改动未提交,`git checkout` 即回退

## Review Gate

- 步骤 4 后:构建通过 + 无 Glance 1.1.1 已知坑回退(ColorProvider/Application)
- 步骤 6 后:AC1-AC5 全过 → 提交
