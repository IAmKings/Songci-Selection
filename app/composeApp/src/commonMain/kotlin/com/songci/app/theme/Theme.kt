package com.songci.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import songci.composeapp.generated.resources.*

/** DESIGN.md「Classical Manuscript」颜色 token 的 Compose 落地。 */
object SongciColors {
    val background = Color(0xFFFBF9F2)
    val onBackground = Color(0xFF1B1C18)
    val surface = Color(0xFFFBF9F2)
    val surfaceContainerLow = Color(0xFFF5F4ED)
    val surfaceContainer = Color(0xFFEFEEE7)
    val surfaceContainerHigh = Color(0xFFE9E8E1)
    val surfaceContainerHighest = Color(0xFFE3E3DC)
    val surfaceVariant = Color(0xFFE3E3DC)
    val surfaceTint = Color(0xFF465F88)
    val primary = Color(0xFF002046)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFF1B365D)
    val onPrimaryContainer = Color(0xFF87A0CD)
    val secondary = Color(0xFF605E59)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFE6E2DB)
    val onSecondaryContainer = Color(0xFF66645F)
    val tertiary = Color(0xFF212119)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFF37362D)
    val onTertiaryContainer = Color(0xFFA19F93)
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF93000A)
    val outline = Color(0xFF74777F)
    val outlineVariant = Color(0xFFC4C6CF)
    // 语义补充色(kami 保留,用于正文/分隔/页脚标注)
    val nearBlack = Color(0xFF141413)
    val stone = Color(0xFF6B6A64)
    val warmSand = Color(0xFFE8E6DC)
    val line = Color(0xFFD8D5C8)
    val backdrop = Color(0xFF3D3D3A)
}

/** 字体:LXGW WenKai(霞鹜文楷,OFL)文学内容,Inter 界面标注(DESIGN.md typography)。Font 为 @Composable 资源加载。 */
val NotoSerifFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.lxgw_wenkai_regular, FontWeight.Normal),
        Font(Res.font.lxgw_wenkai_medium, FontWeight.Medium),
    )

val InterFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
    )

/** DESIGN.md typography:标题 46/36、正文 20/18、行高 2.05–2.1、标注 Inter 加宽字距。 */
val SongciTypography: Typography
    @Composable get() = Typography(
    headlineLarge = TextStyle(
        fontFamily = NotoSerifFamily, fontSize = 46.sp, fontWeight = FontWeight.Medium,
        lineHeight = 55.sp, letterSpacing = 0.06.em,
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSerifFamily, fontSize = 36.sp, fontWeight = FontWeight.Medium,
        lineHeight = 43.sp, letterSpacing = 0.06.em,
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSerifFamily, fontSize = 15.sp, fontWeight = FontWeight.Normal,
        lineHeight = 22.5.sp, letterSpacing = 0.02.em,
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSerifFamily, fontSize = 20.sp, fontWeight = FontWeight.Normal,
        lineHeight = 42.sp, letterSpacing = 0.02.em,   // 平板:行高 2.1
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSerifFamily, fontSize = 18.sp, fontWeight = FontWeight.Normal,
        lineHeight = 37.sp, letterSpacing = 0.02.em,   // 手机:行高 2.05
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        lineHeight = 14.sp, letterSpacing = 0.05.em,   // ui-nav
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal,
        lineHeight = 17.sp, letterSpacing = 0.14.em,   // label-metadata
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal,
        lineHeight = 15.sp, letterSpacing = 0.14.em,
    ),
)

/** DESIGN.md「Shapes:严格直角」—— 全部 0dp。 */
val SongciShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
)

/** DESIGN.md 全 token → Material3 浅色 ColorScheme;无 dark 变体(M1 切出亮度调节)。 */
val SongciColorScheme: ColorScheme = lightColorScheme(
    primary = SongciColors.primary,
    onPrimary = SongciColors.onPrimary,
    primaryContainer = SongciColors.primaryContainer,
    onPrimaryContainer = SongciColors.onPrimaryContainer,
    secondary = SongciColors.secondary,
    onSecondary = SongciColors.onSecondary,
    secondaryContainer = SongciColors.secondaryContainer,
    onSecondaryContainer = SongciColors.onSecondaryContainer,
    tertiary = SongciColors.tertiary,
    onTertiary = SongciColors.onTertiary,
    tertiaryContainer = SongciColors.tertiaryContainer,
    onTertiaryContainer = SongciColors.onTertiaryContainer,
    error = SongciColors.error,
    onError = SongciColors.onError,
    errorContainer = SongciColors.errorContainer,
    onErrorContainer = SongciColors.onErrorContainer,
    background = SongciColors.background,
    onBackground = SongciColors.onBackground,
    surface = SongciColors.surface,
    onSurface = SongciColors.nearBlack,
    surfaceVariant = SongciColors.surfaceVariant,
    onSurfaceVariant = SongciColors.stone,
    outline = SongciColors.outline,
    outlineVariant = SongciColors.outlineVariant,
    surfaceTint = SongciColors.surfaceTint,
)

@Composable
fun SongciTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SongciColorScheme,
        typography = SongciTypography,
        shapes = SongciShapes,
        content = content,
    )
}
