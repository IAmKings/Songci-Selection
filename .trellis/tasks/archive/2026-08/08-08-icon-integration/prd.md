# 三端应用图标接入

## Goal

把设计定稿图标 `design/app-icon/screen.png`(1024×1024,卷轴+毛笔,书生蓝+纸张配色)接入三端应用壳,替换占位/默认图标:Android(launcher 图标)、iOS(AppIcon)、桌面 macOS(mac 应用图标,连带 MSI/Deb 格式)。

## Background

- 应用为 Kotlin + Compose Multiplatform,三端共用设计资产(`design/README.md` 已登记 screen.png)
- `app/README.md` 关键决策记录「桌面/iOS 暂用占位;`design/app-icon/screen.png` 待接入(阶段 4 未完成项)」
- 现状:iOS `AppIcon.appiconset` 仅含空槽(单张 1024 通用格式);桌面 `nativeDistributions` 无 `icon()` 配置;Android manifest 无 `android:icon`(系统默认图标),minSdk 24(< 26,需 legacy mipmap + adaptive icon)

## Requirements

- **R1 单源可复现**:`screen.png` 为唯一图标源;新增生成脚本 `data/tools/gen_app_icons.py`(与 `prepare_db.py`/`dynasty.py` 同目录,本机有 Pillow),从单一源产出三端全部尺寸产物,不提交手工改过的中间图
- **R2 iOS**:`AppIcon.appiconset` 填入 1024×1024 图标(单尺寸通用槽,Contents.json 已就绪,仅补文件)
- **R3 Android**:manifest 加 `android:icon="@mipmap/ic_launcher"`;生成 legacy mipmap(全部密度)+ adaptive icon(API 26+,前景按 66% 安全区居中,背景取图标底色)
- **R4 桌面**:`compose.desktop.application.nativeDistributions` 加 `icon(...)`;macOS `.icns`、Windows `.ico`、Linux `.png` 三格式齐备(目标格式 Dmg/Msi/Deb 均已声明)

## Acceptance Criteria

- AC1:脚本执行后 `git status` 中三端图标产物齐全,再次执行结果幂等(不重复膨胀,可 `git diff` 无变化)
- AC2:iOS `Contents.json` 引用的图标文件存在且为 1024×1024
- AC3:Android `assembleDebug` 通过;`ic_launcher.xml`(adaptive)与 legacy png 均存在于正确 mipmap 目录
- AC4:桌面构建配置引用三个图标文件且路径存在(gradle 同步/编译通过)

## Out of Scope

- Android roundIcon、商店上架素材(Feature Graphic 等)
- 图标视觉再设计(以 screen.png 为终稿,不做评审)

## 关键决策

- 单一生成脚本取代手改:图标改动 → 重跑脚本,全端同步更新
- Android adaptive 前景安全区按 Google 规范 66%(108dp 设计稿中内容区 66dp)
- 桌面三格式全配:构建脚本已声明 Dmg/Msi/Deb,补 ico/png 成本约脚本 5 行,不做半个
