package com.songci.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.songci.app.data.AppContextHolder
import com.songci.app.data.NotificationPrefs
import com.songci.app.data.rescheduleDailyNotification
import kotlinx.datetime.offsetAt
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "daily-poem"
private const val CHANNEL_ID = "daily_poem"

/**
 * 每日一词 Worker:触发时刻检查授权 → 随机选词 → 通知(点击 songci://poem/{id} 深链)。
 * 未授权(Android 13+ 权限或系统关闭)→ 不通知(静默成功)。
 */
class DailyPoemWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }
        val poem = com.songci.app.data.pickRandomPoem() ?: return Result.success()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("songci://poem/${poem.id}"))
        val pending = PendingIntent.getActivity(
            applicationContext, poem.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = "${poem.rhythmic}${if (poem.authorName.isEmpty()) "" else " · ${poem.authorName}"}"
        val firstLine = poem.notificationFirstLine()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)   // ponytail: 通用图标,品牌图标后续
            .setContentTitle(title)
            .setContentText("「$firstLine」")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(poem.id.toInt(), notification)
        return Result.success()
    }
}

/** 距下次本地 hh:mm 的毫秒数(纯算术,同 msUntilNextMidnight 模式,任何时区恒成立)。 */
internal fun millisUntilNext(hour: Int, minute: Int): Long {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val offset = kotlinx.datetime.TimeZone.currentSystemDefault()
        .offsetAt(kotlinx.datetime.Instant.fromEpochMilliseconds(now)).totalSeconds * 1000L
    val local = now + offset
    val day = 86_400_000L
    val target = local - local % day + hour * 3_600_000L + minute * 60_000L
    var delta = target - local
    if (delta <= 0L) delta += day
    return delta
}

private fun ensureChannel(ctx: Context) {
    // 渠道仅 Android 8+(API 26);以下版本无渠道概念,通知直接可用
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CHANNEL_ID, "每日一词", NotificationManager.IMPORTANCE_DEFAULT,
    ).apply { description = "每日定时推送一首词" }
    ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

actual fun notificationPermissionGranted(): Boolean =
    NotificationManagerCompat.from(AppContextHolder.context).areNotificationsEnabled()

actual fun requestNotificationPermission() {
    // Android 13+ 需运行时权限:未授权弹系统授权框;已授权/低版本直接跳过
    if (notificationPermissionGranted()) return
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
    AppContextHolder.activity?.requestPermissions(
        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_CODE,
    )
}

private const val REQUEST_NOTIFICATION_CODE = 1001

actual fun rescheduleDailyNotification(prefs: NotificationPrefs) {
    val ctx = AppContextHolder.context
    val wm = WorkManager.getInstance(ctx)
    if (!prefs.enabled || !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
        wm.cancelUniqueWork(WORK_NAME)
        return
    }
    ensureChannel(ctx)
    val request = PeriodicWorkRequestBuilder<DailyPoemWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(millisUntilNext(prefs.hour, prefs.minute), TimeUnit.MILLISECONDS)
        .build()
    wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
}
