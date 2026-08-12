package com.songci.app.data

import java.io.File
import java.util.Properties

/** 测试可用 -Dsongci.settings.file=... 重定向到临时文件,避免污染真实用户设置。 */
private fun settingsFile(): File =
    System.getProperty(SETTINGS_FILE_PROPERTY)?.let(::File)
        ?: File(File(System.getProperty("user.home"), ".songci"), "settings.properties")

actual fun saveFontScaleName(name: String) {
    val file = settingsFile()
    val props = Properties()
    if (file.exists()) file.inputStream().use(props::load)
    props.setProperty("font_scale", name)
    file.parentFile.mkdirs()
    file.outputStream().use { props.store(it, "songci settings") }
}

actual fun loadFontScaleName(): String? {
    val file = settingsFile()
    if (!file.exists()) return null
    val props = Properties()
    file.inputStream().use(props::load)
    return props.getProperty("font_scale")
}

actual fun saveFontStyle(name: String) {
    val file = settingsFile()
    val props = Properties()
    if (file.exists()) file.inputStream().use(props::load)
    props.setProperty("font_style", name)
    file.parentFile.mkdirs()
    file.outputStream().use { props.store(it, "songci settings") }
}

actual fun loadFontStyle(): String? {
    val file = settingsFile()
    if (!file.exists()) return null
    val props = Properties()
    file.inputStream().use(props::load)
    return props.getProperty("font_style")
}

private fun loadProps(): Properties {
    val props = Properties()
    val file = settingsFile()
    if (file.exists()) file.inputStream().use(props::load)
    return props
}

actual fun loadNotificationPrefs(): NotificationPrefs = loadProps().let {
    NotificationPrefs(
        enabled = it.getProperty("notify_enabled", "false").toBoolean(),
        hour = it.getProperty("notify_hour", "21").toIntOrNull() ?: 21,
        minute = it.getProperty("notify_minute", "0").toIntOrNull() ?: 0,
        lastScheduledDay = it.getProperty("notify_last_day", "0").toLongOrNull() ?: 0L,
    )
}

actual fun saveNotificationPrefs(prefs: NotificationPrefs) {
    val props = loadProps()
    props.setProperty("notify_enabled", prefs.enabled.toString())
    props.setProperty("notify_hour", prefs.hour.toString())
    props.setProperty("notify_minute", prefs.minute.toString())
    props.setProperty("notify_last_day", prefs.lastScheduledDay.toString())
    val file = settingsFile()
    file.parentFile.mkdirs()
    file.outputStream().use { props.store(it, "songci settings") }
}
