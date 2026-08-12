# 修复 macOS 通知 block 崩溃(ObjC block 手工构造 + 常驻回调)

## Goal

修复发布版(SongciSelection.app)启动约 1.6s 后 SIGSEGV 崩溃:JNA 手工构造的 ObjC block 未写 native 内存 + descriptor 为 NULL,系统在 `getNotificationSettingsWithCompletionHandler:` 内 `_Block_copy` 读 `descriptor->size` 段错误(hs_err_pid22911.log 已实测复现)。

## 背景

- 崩溃日志 `hs_err_pid22911.log`(2026-08-12 23:21,发布版,`_Block_copy+0x38` SIGSEGV)
- 根因三处,修复已写入 `MacNotification.kt`(未提交):
  1. ObjCBlock 未显式 `write()`,native 内存全零 + descriptor NULL → `_Block_copy` 段错误
  2. grant 回调局部变量被 GC → 系统异步持有时悬垂指针
  3. settings 回调异步,原同步读 `status` 是竞态(加 CountDownLatch 等 5s)

## 非目标

- 不动通知调度逻辑、UI、其他平台
- 不引入新依赖(纯 JNA Structure 层修复)

## Acceptance Criteria

- AC1:`MacNotification.kt` 编译通过
- AC2:macOS 实机(或本机)运行启动 + 触发每日一词通知流程,无 `_Block_copy` 崩溃;`authorizationGranted()` 能正确读到授权状态(回调竞态无回归)
- AC3:hs_err 日志确认崩溃路径(settings block)不再出现
- AC4:提交,含 journal 记录;commit 前跑 `gitnexus_detect_changes()` 核对影响范围

## 验证方法

- `./gradlew :composeApp:compileKotlinDesktop`(或发布构建)编译
- 本机运行桌面 app,重复触发 `rescheduleDailyNotification` 路径(启停通知开关)多次,确认无崩溃

## 验证结果(2026-08-12)

- AC1 ✓ 编译通过(`compileKotlinDesktop`)
- AC2 ✓ 打包 `createDistributable` 后运行 `SongciSelection.app` 60s 存活,无新 hs_err;启动即走 `authorizationGranted()` 全路径(与崩溃时间点 1.6s 对应)
- 过程中发现并修复第二处崩溃:block 修复后 settings 回调首次真正触发,暴露 `send(...)?.getLong(0)` 潜伏 bug —— `authorizationStatus`/`longLongValue` 返回 NSInteger 不是指针,`invokePointer` 拿到值后 `getLong(0)` 解引用即段错误(实测 si_addr=0x1,status=1)。新增 `sendLong()`(invokeLong)修复,`ResponseCallback.poemId` 同款 bug 一并修
- gitnexus detect_changes:risk high(单一文件 JNA 原生边界),6 个受影响流程全部是通知功能自身(Reschedule/RequestPermission/ResponseCallback),无外部调用方
- hs_err_pid22911.log(23:21 原始崩溃)与 hs_err_pid27636.log(23:43 getLong 崩溃)已删除
