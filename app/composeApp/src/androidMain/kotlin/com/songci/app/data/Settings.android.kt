package com.songci.app.data

import android.content.Context

actual fun saveFontScaleName(name: String) {
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)
        .edit().putString("font_scale", name).apply()
}

actual fun loadFontScaleName(): String? =
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)
        .getString("font_scale", null)

actual fun saveFontStyle(name: String) {
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)
        .edit().putString("font_style", name).apply()
}

actual fun loadFontStyle(): String? =
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)
        .getString("font_style", null)

private fun prefs() =
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)

actual fun loadNotificationPrefs(): NotificationPrefs = prefs().run {
    NotificationPrefs(
        enabled = getBoolean("notify_enabled", false),
        hour = getInt("notify_hour", 21),
        minute = getInt("notify_minute", 0),
        lastScheduledDay = getLong("notify_last_day", 0L),
    )
}

actual fun saveNotificationPrefs(prefs: NotificationPrefs) {
    prefs().edit()
        .putBoolean("notify_enabled", prefs.enabled)
        .putInt("notify_hour", prefs.hour)
        .putInt("notify_minute", prefs.minute)
        .putLong("notify_last_day", prefs.lastScheduledDay)
        .apply()
}
