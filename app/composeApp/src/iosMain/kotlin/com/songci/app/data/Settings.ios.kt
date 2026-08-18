package com.songci.app.data

import platform.Foundation.NSUserDefaults

actual fun saveFontScaleName(name: String) {
    NSUserDefaults.standardUserDefaults.setObject(name, forKey = "font_scale")
}

actual fun loadFontScaleName(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("font_scale")

actual fun saveFontStyle(name: String) {
    NSUserDefaults.standardUserDefaults.setObject(name, forKey = "font_style")
}

actual fun loadFontStyle(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("font_style")

actual fun saveVerticalLayout(flag: String) {
    NSUserDefaults.standardUserDefaults.setObject(flag, forKey = "vertical_layout")
}

actual fun loadVerticalLayout(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("vertical_layout")

actual fun loadNotificationPrefs(): NotificationPrefs = NSUserDefaults.standardUserDefaults.run {
    // 注意:NSUserDefaults 无 long 方法;lastScheduledDay 用字符串存取;
    // integerForKey 未设置时返回 0,用 objectForKey 区分"未设置"与"0 点"
    NotificationPrefs(
        enabled = boolForKey("notify_enabled"),
        hour = if (objectForKey("notify_hour") != null) integerForKey("notify_hour").toInt() else 21,
        minute = if (objectForKey("notify_minute") != null) integerForKey("notify_minute").toInt() else 0,
        lastScheduledDay = stringForKey("notify_last_day")?.toLongOrNull() ?: 0L,
    )
}

actual fun saveNotificationPrefs(prefs: NotificationPrefs) {
    NSUserDefaults.standardUserDefaults.run {
        setBool(prefs.enabled, forKey = "notify_enabled")
        setInteger(prefs.hour.toLong(), forKey = "notify_hour")
        setInteger(prefs.minute.toLong(), forKey = "notify_minute")
        setObject(prefs.lastScheduledDay.toString(), forKey = "notify_last_day")
    }
}
