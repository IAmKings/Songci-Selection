# PRD:GitHub Actions 自动构建 Android APK 并发布 Release

- 任务目录:`.trellis/tasks/08-19-android-ci-release`
- 状态:规划中

## Goal

为 Android 客户端生成 GitHub workflow:自动构建 APK 并发布到 GitHub Release,方便用户直接下载安装。

## Background / 已确认事实(代码勘查)

- 项目**无现有 CI**(无 `.github/workflows/`)。
- 项目根目录即 Gradle 项目(`app/` 为模块),wrapper 在 `app/gradlew`(`gradle-wrapper.properties` 指向腾讯镜像 gradle-9.7.0)。
- 技术栈:AGP 9.3.1、Kotlin 2.4.10、Compose Multiplatform 1.11.1、compileSdk 36、minSdk 24、targetSdk 36;JDK 17。
- `buildTypes.release` 仅 `isMinifyEnabled = false`,**无签名配置**。
- **签名决策(用户确认)**:CI 用 **debug keystore 签 release APK**(零配置、直接可装),正式上架前再换正式签名。
- 现有 debug APK 输出:`app/composeApp/build/outputs/apk/debug/composeApp-debug.apk`(~40MB)。

## Requirements

- R1:新增 `.github/workflows/android-release.yml`,支持**手动触发(workflow_dispatch)** 与 **tag 触发(push tags v\*)**。
- R2:CI 环境:JDK 17、Android SDK(compileSdk 36)、Gradle 缓存加速。
- R3:构建 `assembleRelease`,用 debug keystore 签名,产出可安装 APK。
- R4:发布到 GitHub Release:tag 触发时创建/更新对应 tag 的 Release;手动触发时创建草稿 Release(或由 tag 决定)。
- R5:Release 资产含 APK + 简短说明(版本、构建时间)。
- R6:工作流可复现(依赖 gradle/actions/setup-gradle 缓存)。

## Out of Scope(不含)

- 正式 release 签名(keystore secrets 注入)——后续如需上架 Play 再另行任务。
- iOS/Desktop 自动发布。
- 自动版本号 bump、changelog 生成。
- APK 加固/混淆(release 目前 minify off)。

## Acceptance Criteria

- AC1:workflow 文件存在且 YAML 合法;触发配置(workflow_dispatch + tags v*)正确。
- AC2:本地模拟验证:workflow 中的构建命令(`./gradlew :composeApp:assembleRelease`)在 CI 等价环境可产出 APK。
- AC3:APK 用 debug 签名,`adb install` 可安装。
- AC4:Release 资产命名含版本号,便于区分。

## Open Questions

- 无(签名方案已确认)。
