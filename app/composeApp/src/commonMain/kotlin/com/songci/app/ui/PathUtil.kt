package com.songci.app.ui

/** 路径参数百分号编码(commonMain 无 java.net.Uri;CMP navigation 会自动解码路径参数)。 */
internal fun encodePath(s: String): String = buildString {
    val hex = "0123456789ABCDEF"
    s.encodeToByteArray().forEach { byte ->
        val c = byte.toInt() and 0xFF
        val safe = c in 0x30..0x39 || c in 0x41..0x5A || c in 0x61..0x7A ||
            c == '-'.code || c == '_'.code || c == '.'.code
        if (safe) append(c.toChar())
        else append('%').append(hex[c ushr 4]).append(hex[c and 0xF])
    }
}
