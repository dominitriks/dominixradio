package com.bgradio.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG 2.1 contrast checks for the DOMINIX palette (text 4.5:1, large icons 3:1). */
class PaletteContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun assertText(name: String, fg: Color, bg: Color, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$name contrast %.2f < $min".format(r), r >= min)
    }

    @Test
    fun darkThemeTextIsReadable() {
        for (bg in listOf(Palette.darkBackground, Palette.darkSurface, Palette.darkSurfaceHighest)) {
            assertText("text", Palette.darkText, bg, 7.0)
            assertText("muted text", Palette.darkMuted, bg)
            assertText("primary as text", Palette.darkPrimary, bg)
            assertText("secondary as text", Palette.darkSecondary, bg)
            assertText("playing label", Palette.darkPlaying, bg)
            assertText("favorite", Palette.darkFavorite, bg)
            assertText("warning label", Palette.darkWarning, bg)
            assertText("error", Palette.darkError, bg)
        }
    }

    @Test
    fun contentOnFilledAccentsIsReadable() {
        assertText("onPrimary on primary", Palette.darkOnPrimary, Palette.darkPrimary)
        // White play icon on the deep purple / blue gradient of the big Play button.
        assertText("white on deep purple", Color.White, Brand.PurpleDeep)
        assertText("white on deep blue", Color.White, Brand.BlueDeep)
    }

    @Test
    fun lightThemeTextIsReadable() {
        for (bg in listOf(Palette.lightBackground, Palette.lightSurface)) {
            assertText("text", Palette.lightText, bg, 7.0)
            assertText("muted text", Palette.lightMuted, bg)
            assertText("primary", Palette.lightPrimary, bg)
            assertText("playing", Palette.lightPlaying, bg)
            assertText("favorite", Palette.lightFavorite, bg)
            assertText("warning", Palette.lightWarning, bg)
        }
    }

    @Test
    fun initialsOnGenreTilesAreReadable() {
        val dark = Color(0xFF101322)
        listOf("Поп", "Хитове", "Рок", "Денс", "Българска музика", "Чалга", "Новини", "Спорт", "Джаз", "Класическа музика", "Други")
            .forEach { g ->
                val tile = genreColor(g).copy(alpha = 0.7f).compositeOver(dark) // lightest end of the tile gradient
                assertText("initials on $g", Color.White, tile)
            }
    }

    @Test
    fun sloganIsExactlyAsGiven() {
        assertEquals("Няма нищо по-хубаво от лошото време", Brand.SLOGAN)
    }
}
