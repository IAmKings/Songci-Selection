package com.songci.app

import android.app.Application
import com.songci.app.data.AppContextHolder
import com.songci.app.widget.scheduleNextMidnight

/**
 * 进程入口。AppContextHolder 原只在 MainActivity.onCreate 赋值,小组件冷启动
 * (launcher 直接触发 receiver,MainActivity 未运行)会 UninitializedPropertyAccessException,
 * 提前到 Application 层兜底。
 */
class SongciApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext
        scheduleNextMidnight(this)   // 凌晨小组件自动更新(REPLACE 幂等,重复启动安全)
    }
}
