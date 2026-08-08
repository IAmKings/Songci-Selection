package com.songci.app.data

/**
 * 单值设置持久化(字号档位名)。
 * 阶梯第 4 条:各平台原生键值存储即可(SharedPreferences / NSUserDefaults / Properties 文件),
 * 单枚举值不值得引入 DataStore + okio 依赖。
 */
/** 桌面测试接缝:重定向设置文件路径的系统属性名。 */
internal const val SETTINGS_FILE_PROPERTY = "songci.settings.file"

expect fun saveFontScaleName(name: String)

expect fun loadFontScaleName(): String?
