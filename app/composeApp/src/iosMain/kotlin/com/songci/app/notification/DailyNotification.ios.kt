package com.songci.app.data

import com.songci.app.data.NotificationPrefs
import com.songci.app.data.loadNotificationPrefs
import com.songci.app.data.pickRandomPoem
import com.songci.app.data.rescheduleDailyNotification
import com.songci.app.data.saveNotificationPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/** 滚动窗口:预排未来 N 天(每天一条非重复,调度时各随机选词;官方推荐模式,pending 上限 64 内)。 */
private const val WINDOW_DAYS = 7L

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private suspend fun authorizationStatus(): UNAuthorizationStatus =
    suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            cont.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
        }
    }

actual fun requestNotificationPermission() {
    // 仅在用户主动开启开关时调用(系统弹授权框);拒绝后不引导
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
    ) { _, _ -> }
}

actual fun notificationPermissionGranted(): Boolean =
    runBlocking { authorizationStatus() == UNAuthorizationStatusAuthorized }   // ponytail: 阻塞查询,低频调用可接受

/** 触发日期:daysFromNow 天后的 hh:mm(经 NSCalendar 加天,DST 安全;非重复触发必须带 y/m/d,否则全部落在同一"下次匹配"日)。 */
private fun triggerDate(daysFromNow: Int, hour: Int, minute: Int): NSDateComponents {
    val cal = NSCalendar.currentCalendar
    val future = cal.dateByAddingUnit(
        NSCalendarUnitDay, value = daysFromNow.toLong(),
        toDate = NSDate(), options = 0u,
    ) ?: NSDate()
    val comps = cal.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = future)
    comps.setHour(hour.toLong())      // NSDateComponents 属性只读,经 setter 写入
    comps.setMinute(minute.toLong())
    return comps
}

private fun epochDay(): Long = (NSDate().timeIntervalSince1970 / 86_400.0).toLong()

actual fun rescheduleDailyNotification(prefs: NotificationPrefs) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    if (!prefs.enabled) {
        center.removeAllPendingNotificationRequests()
        return
    }
    scope.launch {
        if (authorizationStatus() != UNAuthorizationStatusAuthorized) return@launch   // 未授权:不排(不引导)
        // 幂等滚动窗口:每次启动/设置变更都清掉重排未来 7 天(改时间即刻生效;last_day=0 首次不排到 1970)
        center.removeAllPendingNotificationRequests()
        val current = loadNotificationPrefs()   // 用最新设置(保存与调度间可能有更新)
        val today = epochDay()
        // 滚动窗口:每次启动/设置变更从"今天(未过时刻)或明天"起排 7 天 —— 改时间/选词即刻生效;
        // lastScheduledDay 仅记录进度(推进式曾致 1970 立即触发无限通知,桌面已实测)
        val nowComps = NSCalendar.currentCalendar.components(NSCalendarUnitHour or NSCalendarUnitMinute, fromDate = NSDate())
        val firstDay = if (nowComps.hour * 60 + nowComps.minute < current.hour * 60 + current.minute) today else today + 1
        var day = firstDay
        val lastDay = firstDay + WINDOW_DAYS - 1
        while (day <= lastDay) {
            val poem = pickRandomPoem() ?: return@launch
            val title = if (poem.authorName.isEmpty()) poem.rhythmic else "${poem.rhythmic} · ${poem.authorName}"
            val firstLine = poem.content.lineSequence().firstOrNull() ?: ""
            val content = UNMutableNotificationContent()   // 属性只读,经 setter 写入
            content.setTitle(title)
            content.setBody("「$firstLine」")
            content.setUserInfo(mapOf("poemId" to poem.id))
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                triggerDate((day - today).toInt(), current.hour, current.minute), repeats = false,
            )
            center.addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier("daily-poem-$day", content, trigger),
            ) { }
            day++
        }
        saveNotificationPrefs(current.copy(lastScheduledDay = lastDay))
    }
}
