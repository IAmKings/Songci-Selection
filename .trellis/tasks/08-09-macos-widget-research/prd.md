# macOS 桌面小组件方案研究

## Goal

确定 Compose Multiplatform desktop 主程序(macOS)接入系统小组件(WidgetKit)的可行方案,给出实施规划。

## 研究结论(2026-08-09)

### 技术事实

- macOS WidgetKit 扩展 = `.appex`,嵌入 host app 的 `Contents/Extensions/`;扩展 bundle id 须为 host id 前缀(com.songci.app.SongciWidgetExtension ✓ 已符合);扩展与 host 同签名链
- Compose desktop 主程序:jpackage 打包 .app(当前 **ad-hoc 签名**,无开发者证书 0 identities),bundle 无 Extensions 目录
- jpackage **不支持**嵌入 .appex;App Group 数据共享需双方 entitlements(codesign --entitlements 支持 ad-hoc 签名带 entitlements)
- CMP desktop 无官方 SwiftUI 嵌入(desktop 为 AWT 栈)——「改造主程序为 SwiftUI 壳」不可行

### 方案对比

| 方案 | 可行性 | 成本 | 风险 |
|---|---|---|---|
| **A. jpackage 后处理嵌入 .appex** | 高(本地 ad-hoc)/中(分发需 Developer ID) | Xcode 独立 macOS 扩展工程 + 打包后处理脚本 + 签名链 | ad-hoc 扩展能否被 WidgetKit 加载——**需实测验证**;分发前需申请证书 |
| B. SwiftUI host 壳 app | 中 | 新 app 工程 + 双 app 分发 | 体验割裂(小组件宿主与主应用分离) |
| C. 主程序改 SwiftUI 壳 | 不可行 | — | CMP desktop 无 NSView 嵌入 |

### 推荐:方案 A

- 本机开发链路:独立 Xcode 工程(macOS Widget 扩展 target,复用 iOS Swift 代码适配 macOS 规格)→ xcodebuild 构建 .appex → 打包脚本嵌入 Compose .app 的 Contents/Extensions + 双端 ad-hoc 重签(含 App Group entitlements)→ packageDmg
- 数据通道:复用已有 App Group(group.com.songci.selection)——Compose 主程序 iOS 驱动已有同步逻辑,desktop 侧需补 App Group 复制(desktop 无 App Group 容器?——**macOS app 的 App Group 容器路径由 entitlement 决定**(~/Library/Group Containers/group.com.songci.selection)——desktop 侧同样可写,需验证)

## 规划

1. ✅ **验证阶段**(本任务完成,见下方实测结论)
2. **实现阶段**(后续任务):扩展正式化(规格适配)+ 打包脚本入管线 + 签名链文档
3. **分发阶段**(外部依赖):Developer ID 证书申请

## 验证结果(2026-08-09 实测)

- AC1 ✅ macOS 桌面小组件添加成功并展示随机词(小/中/大/超大 4 尺寸)
- AC2 ✅ App Group 容器 db 从 Compose desktop 同步(21340 首词,SQLite 直查)
- AC3 待 desktopTest(后续实现阶段跑)

### 关键实测结论(修正研究假设)

- **非 ad-hoc**:本机有 Apple Development 证书(w496830083@qq.com / GZLGVHRJ46 / Team 74M6SQ4PBQ),扩展必须与 host 同证书链签名;"0 identities" 假设已过时
- **扩展必须带 `com.apple.security.app-sandbox=true`**(macOS 扩展沙盒化硬性要求),缺则 WidgetKit 拒绝加载——之前"无法加载"的两大根因之一
- **另一根因**:只部署到 build 目录的 .app 不注册扩展,必须部署到 `/Applications` 后 pluginkit 才注册(运行中的旧拷贝无 Extensions 目录)
- **macOS 26 起** widget 背景必须用 `.containerBackground(for: .widget)`,否则显示 "Please adopt containerBackground API" 占位
- compose 打包默认 ad-hoc 签名,须手动重签 host(带 application-groups entitlement),嵌套签名先签子

### 交付物

- `scripts/macos-widget-deploy.sh`:嵌入 .appex → 重签 host → 部署 /Applications 一步到位(xcodebuild 产物复用)

## 开放问题(已闭环)

- ad-hoc 签名扩展的 WidgetKit 加载行为(验证阶段实测,不预先假设)
