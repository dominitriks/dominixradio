package com.bgradio.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.bgradio.app.data.model.ThemeMode

// ---- DOMINIX "Dark Premium + Radio Experience" palette ------------------------------------

/** Brand accents. Purple leads; blue and cyan support; used sparingly. */
object Brand {
    /** Accent for text and icons on dark surfaces (≥ 4.5:1 contrast). */
    val Purple = Color(0xFF9B7BFF)
    /** Deep purple for filled accent surfaces with white content (white on it ≥ 4.5:1). */
    val PurpleDeep = Color(0xFF7C4DFF)
    val Blue = Color(0xFF6B93FF)
    val BlueDeep = Color(0xFF3E63E6)
    val Cyan = Color(0xFF22D3EE)

    // Bulgarian tricolour — only as a thin identity line (same as in the app icon).
    val FlagWhite = Color(0xFFF2F2F2)
    val FlagGreen = Color(0xFF00966E)
    val FlagRed = Color(0xFFD62612)

    /** The brand slogan — exact wording, do not change. */
    const val SLOGAN = "Няма нищо по-хубаво от лошото време"
}

private val DarkColors = darkColorScheme(
    primary = Brand.Purple,
    onPrimary = Color(0xFF14082F),
    primaryContainer = Color(0xFF2B2150),
    onPrimaryContainer = Color(0xFFE4DBFF),
    secondary = Brand.Blue,
    onSecondary = Color(0xFF06143D),
    secondaryContainer = Color(0xFF1C2747),
    onSecondaryContainer = Color(0xFFD7E0FF),
    tertiary = Brand.Cyan,
    onTertiary = Color(0xFF00222A),
    background = Color(0xFF090A10),
    onBackground = Color(0xFFEDEEF5),
    surface = Color(0xFF090A10),
    onSurface = Color(0xFFEDEEF5),
    surfaceVariant = Color(0xFF1C1E2B),
    onSurfaceVariant = Color(0xFFA3A7BA),
    surfaceContainerLowest = Color(0xFF07080D),
    surfaceContainerLow = Color(0xFF0F1018),
    surfaceContainer = Color(0xFF14151F),
    surfaceContainerHigh = Color(0xFF1A1C28),
    surfaceContainerHighest = Color(0xFF222433),
    outline = Color(0xFF3B3E52),
    outlineVariant = Color(0xFF24263A),
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF4A1518),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D3FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E0FF),
    onPrimaryContainer = Color(0xFF22005F),
    secondary = Color(0xFF2F5BE0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE4FF),
    onSecondaryContainer = Color(0xFF001550),
    tertiary = Color(0xFF00788C),
    onTertiary = Color.White,
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF14151F),
    surface = Color(0xFFF6F6FB),
    onSurface = Color(0xFF14151F),
    surfaceVariant = Color(0xFFE7E7F2),
    onSurfaceVariant = Color(0xFF545869),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F1F8),
    surfaceContainerHighest = Color(0xFFE9E9F3),
    outline = Color(0xFFC3C5D4),
    outlineVariant = Color(0xFFE0E1EC),
)

/** Meanings that must not share one colour (each also has an icon / text, never colour alone). */
@Immutable
data class DominixColors(
    /** "Playing now" indicator. */
    val playing: Color,
    /** Favourite heart. */
    val favorite: Color,
    /** Connecting / temporarily unavailable. */
    val warning: Color,
    /** Soft glow behind the artwork in the player. */
    val glowPrimary: Color,
    val glowSecondary: Color,
)

private val DarkExtra = DominixColors(
    playing = Brand.Cyan,
    favorite = Color(0xFFFF5C8A),
    warning = Color(0xFFFFC266),
    glowPrimary = Brand.PurpleDeep,
    glowSecondary = Brand.BlueDeep,
)

private val LightExtra = DominixColors(
    playing = Color(0xFF00788C),
    favorite = Color(0xFFD81B60),
    warning = Color(0xFF9A6200),
    glowPrimary = Color(0xFF6D3FE0),
    glowSecondary = Color(0xFF2F5BE0),
)

private val LocalDominixColors = staticCompositionLocalOf { DarkExtra }

/** Spacing, corner radii and sizes shared by all screens. */
object Dimens {
    val ScreenPadding = 20.dp
    val ListPadding = 16.dp
    val ItemGap = 12.dp
    val SectionGap = 24.dp

    val RadiusSmall = 12.dp
    val RadiusMedium = 16.dp
    val RadiusLarge = 20.dp

    /** Minimum touch target. */
    val Touch = 48.dp

    val CardShape = RoundedCornerShape(RadiusMedium)
    val PanelShape = RoundedCornerShape(RadiusLarge)
}

object DominixTheme {
    val colors: DominixColors
        @Composable @ReadOnlyComposable get() = LocalDominixColors.current
}

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    )
}

/** Brand slogan style: light serif italic — elegant, quiet, readable. */
val SloganStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.2.sp,
)

/** Small uppercase label (status badges, section captions). */
val SmallCaps = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

/**
 * Colour per genre — only for the initials tile of stations without a logo. Muted so the brand
 * accents stay in charge.
 */
fun genreColor(genre: String): Color = when (genre) {
    "Поп" -> Color(0xFFB8578A)
    "Хитове" -> Color(0xFFB98A3E)
    "Рок" -> Color(0xFFB0504F)
    "Денс" -> Color(0xFF7A5BC4)
    "Българска музика" -> Color(0xFF3F9A7C)
    "Чалга" -> Color(0xFFB8703F)
    "Новини" -> Color(0xFF4F72C0)
    "Спорт" -> Color(0xFF4C9A5E)
    "Джаз" -> Color(0xFF8F7343)
    "Класическа музика" -> Color(0xFF6468B8)
    else -> Color(0xFF3F8F97)
}

@Composable
fun BGRadioTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalDominixColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Exposed for tests (contrast checks). */
internal object Palette {
    val darkBackground = DarkColors.background
    val darkSurface = DarkColors.surfaceContainer
    val darkText = DarkColors.onSurface
    val darkMuted = DarkColors.onSurfaceVariant
    val darkPrimary = DarkColors.primary
    val darkOnPrimary = DarkColors.onPrimary
    val darkSurfaceHighest = DarkColors.surfaceContainerHighest
    val darkSecondary = DarkColors.secondary
    val darkPlaying = DarkExtra.playing
    val darkFavorite = DarkExtra.favorite
    val darkWarning = DarkExtra.warning
    val darkError = DarkColors.error
    val lightBackground = LightColors.background
    val lightSurface = LightColors.surfaceContainer
    val lightText = LightColors.onSurface
    val lightMuted = LightColors.onSurfaceVariant
    val lightPrimary = LightColors.primary
    val lightPlaying = LightExtra.playing
    val lightFavorite = LightExtra.favorite
    val lightWarning = LightExtra.warning
}
