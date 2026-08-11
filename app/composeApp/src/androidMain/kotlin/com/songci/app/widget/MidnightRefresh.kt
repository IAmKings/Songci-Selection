package com.songci.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.songci.app.ui.msUntilNextMidnight
import java.util.concurrent.TimeUnit

/**
 * 凌晨小组件自动更新:每日 0 点附近刷新四规格 widget(重随机)。
 * WorkManager 一次性延迟任务 + 尾部重排,设备重启自动恢复,无权限要求。
 */
private const val WORK_NAME = "midnight-refresh"

/** 入队凌晨刷新任务(幂等:REPLACE 防重复堆积)。 */
fun scheduleNextMidnight(context: Context) {
    val delay = msUntilNextMidnight()
    val request = OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
}

class MidnightRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        listOf(SongciWidgetSmall(), SongciWidgetBanner(), SongciWidgetMedium(), SongciWidgetLarge())
            .forEach { it.updateAll(applicationContext) }
        scheduleNextMidnight(applicationContext)   // 尾部重排下一个凌晨
        return Result.success()
    }
}
