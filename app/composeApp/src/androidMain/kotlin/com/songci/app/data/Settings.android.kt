package com.songci.app.data

import android.content.Context

actual fun saveFontScaleName(name: String) {
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)
        .edit().putString("font_scale", name).apply()
}

actual fun loadFontScaleName(): String? =
    AppContextHolder.context.getSharedPreferences("songci_settings", Context.MODE_PRIVATE)
        .getString("font_scale", null)
