# implement.md · 宋词选粹 CMP 应用

## 实施顺序(每步可独立验证)

**阶段 0 · 环境与骨架**
1. 核验本机工具链:`java -version`(JDK 17+)、`xcodebuild -version`、Android SDK/ANDROID_HOME、`gradle` 或 wrapper
2. 用官方 KMP wizard 生成 Compose Multiplatform 模板(Android+iOS+Desktop)到 `app/`;锁定 Kotlin/CMP/AGP 版本
3. 骨架三平台构建通过:`:composeApp:assembleDebug`(Android)、`:composeApp:build`(桌面 jar/pkg)、`iosApp` 经 Xcode 构建

**阶段 1 · 数据层(先于 UI,基准可测)**
4. 打包 `db/songci.db` 为各平台资源;实现首启复制到可写目录 + SQLDelight 幂等打开(验证:表存在即跳过 schema)
5. `data/tools/dynasty.py` 推导 `dynasty_map.json`;抽样断言(苏轼→北宋、李清照→南宋、范仲淹→北宋、无信息→未知)
6. 7 条核心查询落地;FTS5 MATCH 编译期首验(失败则切原生驱动降级路径)
7. **验证**:JVM 单元测试对照 `sqlite3` 基准 SQL,断言搜索结果/朝代过滤/收藏增删一致

**阶段 2 · 主题与设计系统**
8. DESIGN.md token → `Theme.kt`(ColorScheme/Shapes/Typography/Kicker 组件)
9. 下载打包 Noto Serif SC + Inter(Google Fonts,标注来源与 License)
10. **验证**:token 抽样断言(颜色值/行高/字号与 DESIGN.md 一致)

**阶段 3 · 页面与导航**
11. 导航骨架(BottomNav 首页/收藏/设置 + 详情 + 搜索 + 目录索引弹层)
12. 首页(推荐词流卡片 + 目录索引 + 自适应导航)
13. 详情(单栏/双栏自适应 + 字号设置生效 + 作者/词牌上下文跳转)
14. 搜索(输入 + 朝代 chip + 词牌 + 结果列表)
15. 收藏(列表 + 空态 + 移除)与设置(字号 + 关于 + 占位项)
16. **验证**:三平台构建 + 桌面可运行;核心路径手测

**阶段 4 · 验收与打磨**
17. AC 逐条核对(AC1 三端构建/AC2 token/AC3 双栏断点/AC4 搜索与朝代基准/AC5 收藏持久化/AC6 字号生效)
18. 与定稿截图逐屏比对(截图经本机渲染,修正偏差);应用图标接入各平台

## 验证命令

```bash
cd app
./gradlew :composeApp:assembleDebug                 # Android
./gradlew :composeApp:build                         # 桌面 JVM 构建
./gradlew :composeApp:testDebugUnitTest / test      # 数据层基准测试
open iosApp/…xcworkspace                            # iOS 经 Xcode 构建运行
```

## 风险文件 / 回滚点

- `app/settings.gradle.kts`、`composeApp/build.gradle.kts` —— 版本/依赖,回滚可整体重生成
- 数据层 `SongciDatabase.kt` 与 .sq 文件 —— FTS5 降级切换点
- `Theme.kt` —— token 唯一来源,改 token 需同步 DESIGN.md
- 回滚:删除 `app/`(纯新增目录),仓库其余部分不受影响

## 开工前检查

- [ ] 用户已批准最终规划总结(本表所属 prd/design/implement 评审)
- [ ] 工具链核验(阶段 0 第 1 步)通过或已明确缺项
- [ ] 本机可下载依赖(Google Fonts、Maven Central、KMP 模板)
