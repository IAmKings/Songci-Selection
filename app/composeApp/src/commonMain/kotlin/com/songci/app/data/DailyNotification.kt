package com.songci.app.data

import com.songci.app.data.db.SongciDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 每日一词通知:按设置重排(开启→排期;关闭→取消)。
 * 平台实现:Android WorkManager / iOS K/N 直调 UNUserNotificationCenter(滚动排 7 天)/
 * macOS JVM + JNA 绑定(同 iOS 滚动窗口)。
 */
expect fun rescheduleDailyNotification(prefs: NotificationPrefs)

/**
 * 请求通知授权(用户主动开启开关时调用一次;iOS/macOS 弹系统授权框,
 * Android 13+ 经 MainActivity 存的活动引用弹运行时授权框)。
 */
expect fun requestNotificationPermission()

/** 当前是否已获通知授权(设置页进入时判断,未授权则引导询问)。 */
expect fun notificationPermissionGranted(): Boolean

/**
 * 通知选词:独立随机,复用 randomPoems 查询(异常字符过滤 ⿰/缺字/超长词牌/单行,
 * 与首页/小组件同源推荐规范)。每次调用打开驱动(排期/触发低频场景,开销可接受)。
 */
suspend fun pickRandomPoem(): Poem? = withContext(Dispatchers.Default) {
    SongciDb(createDatabaseDriver()).songciDbQueries.randomPoems(1L).executeAsList().firstOrNull()
        ?.let { Poem(it.id, it.rhythmic, it.content, it.author_id, it.author_name) }
}
