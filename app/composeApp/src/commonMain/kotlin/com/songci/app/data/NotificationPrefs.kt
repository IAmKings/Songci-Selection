package com.songci.app.data

/**
 * 每日一词通知设置。
 * 持久化沿用单值设置模式(SharedPreferences / NSUserDefaults / Properties 文件),
 * 不引入 DataStore(同 font_scale 的阶梯第 4 条理由)。
 */
data class NotificationPrefs(
    val enabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    /** iOS/macOS 滚动窗口标记:已排期到哪个 epoch day(Android 不用,WorkManager 自行周期)。 */
    val lastScheduledDay: Long = 0L,
)

expect fun loadNotificationPrefs(): NotificationPrefs

expect fun saveNotificationPrefs(prefs: NotificationPrefs)
