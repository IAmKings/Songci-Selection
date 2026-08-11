# 数据库升级迁移:资源库更新时保留用户数据

## Goal

修复升级数据丢失:当前 `db_version.txt` 哈希变化 → 三端驱动整库覆盖设备库,设备上的 favorites / recommendation_pool / recent_views 全部丢失。要求:资源库更新(语料更新或新增表)时,用户数据必须保留。同时修复资源库携带开发期用户数据下发的问题。

## Requirements

- **合并迁移**:三端(Android / iOS / Desktop)在资源库版本不一致触发重新复制时,先把旧设备库中的用户表(favorites、recommendation_pool、recent_views)合并进新库,再替换;任何用户表合并失败不得阻塞应用启动(应用可降级为丢失该表数据)。
- **缺表容错**:旧库缺少某用户表(如旧版本无 recent_views/recommendation_pool)时跳过该表,不崩溃。
- **干净产物**:`prepare_db.py` 输出的资源库用户表必须为空(当前随包下发 3 条开发期收藏)。
- **幂等**:同一库重复合并结果一致(INSERT OR REPLACE 语义)。
- **不做的**:不做跨设备云同步;不合并语料表(以资源库为准);不改变既有"数据更新→重新复制"的触发机制。

## Acceptance Criteria

- [ ] 模拟旧库含收藏/推荐池/最近查看 → 新库合并后三者齐全,无重复行
- [ ] 旧库缺少 recent_views 表时合并不崩溃,favorites 仍被保留
- [ ] prepare_db.py 产物中 favorites / recommendation_pool / recent_views 均为 0 行
- [ ] desktop 回归测试全绿(含既有 favorites/搜索/推荐池用例)
- [ ] 三端驱动代码均走"临时副本 → 合并 → 替换"路径,失败不阻塞启动

## Notes

- 数据安全任务:任何一步失败优先保证应用可启动,其次保数据。
