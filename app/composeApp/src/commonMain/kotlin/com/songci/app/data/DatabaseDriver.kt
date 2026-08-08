package com.songci.app.data

import app.cash.sqldelight.db.SqlDriver

/** composeResources 中数据库资源的路径(Android 上为 assets 内路径)。 */
const val DB_RESOURCE_PATH = "files/songci.db"

/** 创建指向预建库 songci.db 的 SQLDelight 驱动;各平台负责把资源复制到可写位置。 */
expect suspend fun createDatabaseDriver(): SqlDriver
