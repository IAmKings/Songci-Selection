# 词作竖排排版 —— Implement

## 实施清单(按序)

1. **`data/Settings.kt` + 三平台 actual**(android/ios/desktop):新增 `saveVerticalLayout(flag: String)` / `loadVerticalLayout(): String?`,复用 `saveFontStyle` 的 SharedPreferences/NSUserDefaults/Properties 机制(key = `vertical_layout`,`"1"`/`"0"`)
2. **`AppViewModel.kt`**:`verticalLayout: Boolean`(mutableStateOf,初值 `loadVerticalLayout()=="1"`);`toggleVerticalLayout()` 写回 `saveVerticalLayout`
3. **`DetailScreen.kt`**:
   - 新增 `VerticalPoemBody(...)`:对 `segments` 每句 `toCharArray` → 逐字 `Column`;多句成 `Row(reversed)`;容器 `horizontalScroll` + 外层 `verticalScroll`
   - `DetailBody` 加 `vertical: Boolean` 参数:true 走竖排分支,false 走现有横排
   - 详情页壳顶栏(‹ 词作详情 右侧)加横/竖切换图标,`onClick = vm.toggleVerticalLayout()`
4. **`SettingsScreen.kt`**:新增「默认竖排」开关 Row,绑定 `vm.verticalLayout`
5. **测试 + 构建**:
   - `cd app && gradle :composeApp:desktopTest`(回归 Segmenter 不改)
   - `gradle :composeApp:assembleDebug` + `adb install -r`
   - 真机:进词详情切竖排,目视宽/窄屏右起多列

## 验证命令

```bash
cd app && ./gradlew :composeApp:desktopTest          # 数据层回归
cd app && ./gradlew :composeApp:assembleDebug         # Android 构建
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## 风险文件

- `DetailScreen.kt`(竖排组件 + DetailBody 分支)——改动大,分段实现
- `AppViewModel.kt`(verticalLayout 状态 / 持久化)
- `SettingsScreen.kt`(开关)
- data 层 settings 存取 API(load/save VerticalLayout)

## 回滚

- UI:移除竖排分支 + `vertical` 参数;设置页移除开关
- 状态:移除 `verticalLayout` 偏好读写(无迁移)
- 一条 branch revert 到 `feat/vertical-poem` 起点即可

## 完成后检查

- [ ] 详情页顶栏图标切换横/竖,切换顺畅无空档
- [ ] 竖排右起正确:首个字在右上,句=列逐字向下,多列向左
- [ ] 窄屏竖排多列 + 水平滚动;宽屏双栏保留
- [ ] 设置开关与详情图标同源持久化,重启沿用
- [ ] desktopTest / assembleDebug / 真机目视全通过

## 追加修复(2026-08-17 真机反馈,仍属本任务)

1. **分阕空开一列**:`VerticalPoemBody` 不再 `flatMap` 打平;保留 `Segmenter.segment` 的上下阕结构,跨阕交界处插入更大的空隙 `Spacer`(同阕用 28dp,跨阕用更大如 72dp)
2. **竖排初始从最右开始**:`horizontalScroll` 默认 scrollTo(0) 落在最左(最后一句);应为右起——`LaunchedEffect` 中 `scrollState.scrollTo(scrollState.maxValue)` 滚到最右(首句)
3. **滑动提示(UX,待用户决策形式)**:竖排无提示用户不知可横滑阅读更多列;需加可见提示(形式见 prd Open)

完成追加修复后复核:右起首列在最右可见;上/下阕交界明显空列;用户知晓可横向滑动。
