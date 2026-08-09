# Implement — 小组件对齐设计稿

## 执行清单

1. **drawable 资源**:`widget_bg_round12/28.xml`(圆角+边框)、`ic_book/ic_refresh/ic_favorite/ic_bookmark.xml`(Material Symbols 描边 vector)
2. **重构 `WidgetContent`**:按 WidgetSpec 分支渲染四套布局(design.md 逐规格要点);色板 token 对齐设计稿
3. **竖排实现**:4x4 词牌/作者竖排;词句尝试竖排两列,超出高度则横排(记录)
4. **印章/装饰角线**:4x4 头部印章(border drawable + 宋字)、四角线
5. **构建 + 实机截图对比**:4 规格截图,与 design/widgets 对比
6. **回归**:desktopTest + assembleDebug
7. **提交**:detect_changes → commit

## 验证命令

```bash
cd app && ./gradlew :composeApp:assembleDebug
./gradlew :composeApp:desktopTest
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb exec-out screencap -p > /tmp/widget.png   # 实机截图(需手动添加 widget 后)
```

## 回滚点

- 步骤 5 前:纯资源+布局改动,git checkout 即回退
- 交互链路(clickable/action)不动,无回滚风险

## Review Gate

- 步骤 5 后:AC1 实机对比通过 → 继续
- 步骤 6 后:AC2/AC3 通过 → 提交;AC4 差距记录入 prd.md
