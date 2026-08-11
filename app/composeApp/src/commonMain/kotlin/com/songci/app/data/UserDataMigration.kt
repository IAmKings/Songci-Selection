package com.songci.app.data

/**
 * 升级迁移:资源库版本不一致触发重新复制时,把旧设备库的用户表合并进新库(ATTACH 方式)。
 *
 * 幂等(INSERT OR REPLACE);旧库缺表(老版本)时跳过该表;失败抛异常由驱动调用方降级
 * (直接替换,保启动优先)。语料表(poems/authors)以资源库为准,不合并。
 */
expect fun mergeUserData(oldDbPath: String, newDbPath: String)

/** 用户数据表清单(与 app/data/tools/prepare_db.py 的 USER_TABLES 保持一致)。 */
internal val USER_TABLES = listOf("favorites", "recommendation_pool", "recent_views")

/** SQL 字面量转义(ATTACH 路径等)。 */
internal fun quoteLiteral(s: String) = s.replace("'", "''")
