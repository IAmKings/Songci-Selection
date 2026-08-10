package com.songci.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘图标:core 图标集(48 个基础)无书签/填充书本,统一在此定义。
 * 语义:书页挑选(书本)→ 保留书签(书签),与页面/收藏逻辑闭环。
 */

/** 索引 tab 图标:填充书本(Material Symbols menu_book filled)。 */
val IndexIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Index", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(21f, 5f)
            curveToRelative(-1.11f, -0.35f, -2.33f, -0.5f, -3.5f, -0.5f)
            curveToRelative(-1.95f, 0f, -4.05f, 0.4f, -5.5f, 1.5f)
            curveToRelative(-1.45f, -1.1f, -3.55f, -1.5f, -5.5f, -1.5f)
            curveToRelative(-1.17f, 0f, -2.39f, 0.15f, -3.5f, 0.5f)
            verticalLineToRelative(14.65f)
            curveToRelative(0f, 0.25f, 0.25f, 0.5f, 0.5f, 0.5f)
            curveToRelative(0.1f, 0f, 0.15f, -0.05f, 0.25f, -0.05f)
            curveTo(3.1f, 20.45f, 5.05f, 20f, 6.5f, 20f)
            curveToRelative(1.95f, 0f, 4.05f, 0.4f, 5.5f, 1.5f)
            curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
            curveToRelative(1.17f, 0f, 2.39f, 0.15f, 3.5f, 0.5f)
            verticalLineTo(5f)
            close()
            moveTo(12f, 18.07f)
            curveToRelative(-1.55f, -0.81f, -3.7f, -1.07f, -5.5f, -1.07f)
            curveToRelative(-0.9f, 0f, -1.87f, 0.08f, -2.75f, 0.23f)
            verticalLineTo(6.22f)
            curveToRelative(0.88f, -0.15f, 1.85f, -0.22f, 2.75f, -0.22f)
            curveToRelative(1.8f, 0f, 3.95f, 0.26f, 5.5f, 1.07f)
            verticalLineTo(18.07f)
            close()
        }
    }.build()
}

/** 书签(Material bookmark):同 path 双形态,filled 实心 / border 描边。 */
private val BookmarkPath: ImageVector.Builder.() -> Unit = {
    path(fill = SolidColor(Color.Black)) {
        moveTo(17f, 3f)
        horizontalLineTo(7f)
        curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(5f, 21f)
        lineToRelative(7f, -3f)
        lineToRelative(7f, 3f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
    }
}

/** 收藏 tab/已收藏:实心书签。 */
val BookmarkIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Bookmark", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f).apply(BookmarkPath).build()
}

/** 未收藏:描边书签。 */
val BookmarkBorderIcon: ImageVector by lazy {
    ImageVector.Builder(name = "BookmarkBorder", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            moveTo(17f, 3f)
            horizontalLineTo(7f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(5f, 21f)
            lineToRelative(7f, -3f)
            lineToRelative(7f, 3f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
        }
    }.build()
}
