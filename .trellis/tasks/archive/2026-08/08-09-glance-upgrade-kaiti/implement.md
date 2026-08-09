# Implement — Glance 升级接入楷体

## 执行清单

1. **升版本**:libs.versions.toml `androidx-glance = 1.2.0-rc01` → `assembleDebug` 编译
2. **验证 API**:javap 本地缓存 AAR 的 FontFamily/ColorProvider/SizeMode;编译错误清单整理
3. **接入楷体**:按 design.md A/B/C 方式,把 lxgw_wenkai 应用到词牌/词句/作者全部词文 Text
4. **回归修正**:API 变化导致的编译错误逐一适配(不改变已踩坑语义)
5. **构建 + 实机验证**:4 规格楷体渲染 + 交互 + 无 ColorProvider 错误
6. **回归**:desktopTest + assembleDebug
7. **提交**:detect_changes → commit(若 1.2.0-rc01 失败则试 1.3.0-alpha02,再失败回退 1.1.1 记录差距)

## 验证命令

```bash
cd app && ./gradlew :composeApp:assembleDebug
./gradlew :composeApp:desktopTest
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## Review Gate

- 步骤 5 后:AC1 楷体实机可见 + AC2 交互正常 → 继续
- 步骤 6 后:AC4 全绿 → 提交
- 兜底:字体 API 缺失 → prd.md 记差距,回退 1.1.1,任务按"不达成"归档
