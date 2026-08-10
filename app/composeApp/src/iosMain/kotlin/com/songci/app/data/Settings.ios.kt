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
