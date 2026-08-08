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
