# PRD:应用内检查更新(GitHub Release,自研零依赖)

- 任务目录:`.trellis/tasks/08-19-inapp-update-checker`
- 状态:规划中

## Goal

应用内提供「检查更新」功能:查询 GitHub Release 最新版本,与本地版本比较,有新版时提示用户(跳转浏览器下载 或 下载安装)。**自研,零新增依赖**。

## Background / 已确认事实(代码勘查)

- GitHub Release 已上线:workflow 构建 `composeApp-release.apk` → 发布 `SongciSelection-{tag}-android.apk`(`.github/workflows/android-release.yml:71`)。
- 版本信息:`versionCode = 1` / `versionName = "0.1.0"`(`app/composeApp/build.gradle.kts:85-86`);Android 端 `BuildConfig.VERSION_NAME` 可用。
- 项目**零网络代码**(无 Ktor/OkHttp/HttpURLConnection);`HttpURLConnection` 是 JRE 内置,不引入第三方依赖。
- 三端已有 `expect/actual` 桥接先例(Settings/PoemSpeaker)。
- 设置页有「关于」区(`SettingsScreen.kt`),是检查更新入口的自然落点。
- **方案决策(用户确认)**:自研零依赖,不用开源库。

## Requirements

- R1(检查):设置页「关于」区加「检查更新」入口,点击查询 GitHub Releases API(`/repos/IAmKings/Songci-Selection/releases/latest`)。
- R2(比较):解析 `tag_name`(如 `v0.1.0`)与本地 `BuildConfig.VERSION_NAME`(如 `0.1.0`)semver 比较;strip `v` 前缀、数值比较 major/minor/patch。
- R3(提示):有新版 → 弹提示(版本号 + release notes 摘要),提供「去下载」(跳浏览器 GitHub Release 页面 `html_url`);无新版 → 提示已是最新。
- R4(零依赖):网络用 JRE 内置 `HttpURLConnection`,不新增第三方依赖。
- R5(平台隔离):commonMain 定义 `expect` 检查接口,androidMain actual 实现(`BuildConfig` 版本 + 网络);desktop/iOS 占位(后续)。
- R6(限流):检查结果缓存/冷却(如 24h 内不重复自动检查;手动检查不受限)。
- R7(网络容错):离线/API 失败 → 提示「检查失败,请稍后重试」,不崩溃。

## Out of Scope(不含)

- **应用内直接下载安装 APK**(Android 安装权限/FileProvider/PackageInstaller 复杂度)——先做「跳转浏览器下载」,验证交互后再评估。
- iOS/Desktop 更新检查(后续多端)。
- 自动检查(启动时静默轮询)——先手动触发。
- 强制更新/更新进度条。

## Acceptance Criteria

- AC1:设置页「关于」区出现「检查更新」入口,显示当前版本号。
- AC2:点击后查询 GitHub latest release;本地低于远程 → 提示有新版(版本号 + 摘要 + 「去下载」按钮跳浏览器)。
- AC3:本地已是新版 → 提示「已是最新版本」。
- AC4:离线/API 失败 → 提示检查失败,不崩溃。
- AC5:不新增任何第三方依赖(`build.gradle.kts` 依赖块无变化)。
- AC6:commonMain expect 接口 + androidMain actual;desktopTest 编译通过;assembleDebug 通过;装机验证。

## Open Questions

- (已决)「去下载」跳转:GitHub Release 页面 `html_url`(用户可看版本说明,2026-08-19 确认)。
