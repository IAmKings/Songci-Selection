# 执行计划:db-upgrade-migration

## 顺序

1. **impact 分析**:`createDatabaseDriver`(android/ios/desktop 三变体)——MUST,向用户报告 blast radius
2. **prepare_db.py**:复制后清空用户表 + 哈希改对 DST;重跑,验证产物三表 0 行
3. **commonMain**:`expect fun mergeUserData` + `USER_TABLES` + `quoteLiteral`(放 `data/` 下新文件 `UserDataMigration.kt`)
4. **三端 actual**:
   - `DatabaseDriver.desktop.kt`:JDBC 实现 + 驱动内"临时副本→merge→替换"重构
   - `DatabaseDriver.android.kt`:SQLiteDatabase 实现 + 同样重构
   - `DatabaseDriver.ios.kt`:NativeSqliteDriver 实现 + 同样重构(主库与 App Group 副本两处)
5. **测试**(desktopTest,`SongciRepositoryTest.kt` 或新 `DbUpgradeTest.kt`):
   - 旧库含三表数据 → 新库合并后齐全、无重复
   - 旧库缺 recent_views → 不崩,favorites 保留
   - 重复合并幂等
6. **验证**:desktopTest 全绿 → assembleDebug → 重装 PJZ110(本次升级会触发合并路径;当前设备库收藏已被上次更新清空,合并保数据的效果由测试验证,设备验证不崩即可)
7. **commit**:`feat(db): 资源库升级迁移(收藏/推荐池/最近查看保留)` + journal + gitnexus sync

## 验收对照

- AC1/AC2 → 测试 5.1/5.2
- AC3 → 步骤 2 产物检查
- AC4 → desktopTest 全绿
- AC5 → 三端代码路径走查 + try/catch 降级
