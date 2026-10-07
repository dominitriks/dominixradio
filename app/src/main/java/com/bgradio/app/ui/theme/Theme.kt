package com.bgradio.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.bgradio.app.data.model.ThemeMode

// Bulgarian flag inspired accents.
val BgGreen = Color(0xFF00A57A)
val BgRed = Color(0xFFE5402A)

private val DarkColors = darkColorScheme(
    primary = BgGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0E3B31),
    onPrimaryContainer = Color(0xFFB9F3DE),
    secondary = BgRed,
    onSecondary = Color.White,
    tertiary = Color(0xFFFFC857),
    background = Color(0xFF0B0D17),
    onBackground = Color(0xFFECEEF6),
    surface = Color(0xFF0B0D17),
    onSurface = Color(0xFFECEEF6),
    surfaceVariant = Color(0xFF1B1F2E),
    onSurfaceVariant = Color(0xFFA5ABBF),
    surfaceContainer = Color(0xFF151927),
    surfaceContainerHigh = Color(0xFF1B1F2E),
    surfaceContainerHighest = Color(0xFF252A3B),
    surfaceContainerLow = Color(0xFF121522),
    outline = Color(0xFF3A4055),
    outlineVariant = Color(0xFF262B3C),
    error = Color(0xFFFF6B5A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00805F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3F1DF),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFFD03420),
    onSecondary = Color.White,
    tertiary = Color(0xFFB07A00),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF151827),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF151827),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF555B70),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEDEFF5),
    surfaceContainerLow = Color(0xFFFFFFFF),
    outline = Color(0xFFC2C6D3),
    outlineVariant = Color(0xFFDDE0E8),
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Colour per genre — used for logo placeholders and small accents. */
fun genreColor(genre: String): Color = when (genre) {
    "Поп" -> Color(0xFFEC4899)
    "Хитове" -> Color(0xFFF59E0B)
    "Рок" -> Color(0xFFEF4444)
    "Денс" -> Color(0xFF8B5CF6)
    "Българска музика" -> Color(0xFF10B981)
    "Чалга" -> Color(0xFFF97316)
    "Новини" -> Color(0xFF3B82F6)
    "Спорт" -> Color(0xFF22C55E)
    "Джаз" -> Color(0xFFA16207)
    "Класическа музика" -> Color(0xFF6366F1)
    else -> Color(0xFF14B8A6)
}

val SmallCaps = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)

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
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
