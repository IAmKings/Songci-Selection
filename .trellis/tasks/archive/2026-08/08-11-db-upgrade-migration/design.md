# 设计:升级迁移(ATTACH 合并)

## 数据流

```
设备启动 → 读资源版本 vs 缓存版本 → 不一致?
  ├─ 否 → 直接打开设备库(现状不变)
  └─ 是 → ① 资源字节写临时副本(不覆盖设备库!)
         ② mergeUserData(旧设备库, 临时副本):ATTACH 旧库 → 逐表 INSERT OR REPLACE → DETACH
         ③ 临时副本替换设备库;写版本标记
```

关键:合并发生在覆盖之前,旧库文件必须存活到第 ② 步。

## 共享契约(commonMain)

```kotlin
expect fun mergeUserData(oldDbPath: String, newDbPath: String)   // 幂等,失败抛异常由调用方降级
internal val USER_TABLES = listOf("favorites", "recommendation_pool", "recent_views")
internal fun quoteLiteral(s: String) = s.replace("'", "''")
```

逐表逻辑(三端一致,仅执行 API 不同):
1. `SELECT 1 FROM olddb.sqlite_master WHERE type='table' AND name='<t>'` → 缺表跳过(旧版本无新表不崩)
2. 存在 → `INSERT OR REPLACE INTO <t> SELECT * FROM olddb.<t>`(幂等;PK 冲突 = 已存在,覆盖)

## 平台实现

| 平台 | 执行 API |
|------|---------|
| Android | `SQLiteDatabase.openDatabase` ×2 + `attachDatabase(path, "olddb")` + `execSQL`;旧库 OPEN_READONLY |
| Desktop | JDBC `DriverManager.getConnection("jdbc:sqlite:new")` + Statement `ATTACH`/`INSERT`/`DETACH` |
| iOS | `NativeSqliteDriver(SongciDb.Schema, newPath)` + `driver.execute(null, sql)`(ATTACH 为可 prepare 语句) |

路径引号:ATTACH 的路径统一 `quoteLiteral` 转义。

## 降级策略

驱动层 merge 调用包 try/catch:合并抛异常 → 记日志(应用零日志约定,仅忽略)→ 仍以临时副本替换(保启动优先,数据本次丢失)。ATTACH 失败(detach 等)同理,用 try/finally 保证连接关闭。

## prepare_db.py 产物净化

复制后对 DST 执行 `DELETE FROM favorites / recommendation_pool / recent_views`,再设 user_version、**哈希改为对 DST(净化后字节)**计算——版本标记与随包字节一致。

## 边界

- 用户表 schema 变化(未来新增列)→ SELECT * 与目标表不匹配会抛错 → 被降级捕获,该表数据本次丢失;将来需要列级迁移时扩展 USER_TABLES 为显式列清单(ponytail: 现无此需求,不做)。
- 不合并语料表:poems/authors 以资源为准。
- db/songci.db(dev 源库)保留 build.py 的"重建保留应用表"行为;净化只发生在 prepare_db.py 产物侧。
