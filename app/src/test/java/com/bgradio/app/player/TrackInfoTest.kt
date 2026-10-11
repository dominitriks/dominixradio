package com.bgradio.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cases are real StreamTitle values captured from the stations in stations.json (Oct 2026). */
class TrackInfoTest {

    private fun display(raw: String?, station: String = "Радио Тест", artist: String? = null) =
        TrackInfo.parse(raw, artist, station)?.display

    @Test
    fun normalSongsAreSplitIntoArtistAndTitle() {
        assertEquals(TrackInfo("SASHA", "I Feel Lonely"), TrackInfo.parse("SASHA - I Feel Lonely", null, "Радио 1"))
        assertEquals("ЕМИЛ ДИМИТРОВ — МОЯ СТРАНА", display("ЕМИЛ ДИМИТРОВ - МОЯ СТРАНА", "БГ Радио"))
        assertEquals("Румяна — Мъри Стани Добро", display("Румяна - Мъри Стани Добро"))
        assertEquals("Peter Brown — (Love Is Just) The Game", display("Peter Brown - (Love Is Just) The Game"))
    }

    @Test
    fun keepsTitlesThatContainDashes() {
        assertEquals("DJ POWER — ESSENTIAL HOUSE - SUNDAY SESSION", display(" DJ POWER - ESSENTIAL HOUSE - SUNDAY SESSION", "Power FM"))
    }

    @Test
    fun emptyOrMissingTitlesAreRejected() {
        assertNull(display(""))
        assertNull(display("   "))
        assertNull(display(null))
        assertNull(display(" - "))
    }

    @Test
    fun advertisementsWithWebAddressesAreRejected() {
        assertNull(display("apteka.puls.bg - APTEKI PULS OCTOBER 2026 AVEN BIOTRADE KLORANE"))
        assertNull(display("www.chernivrah.bg - HOSPIS CHERNI VRAH AUGUST 2026"))
        assertNull(display("radiokanon.com - 2084 I Sorokin D Borhianski Mnogaia leta [4Kj9]", "Радио Канон"))
    }

    @Test
    fun stationSlogansAreRejected() {
        assertNull(display("RADIO VERONIKA - ХИТОВЕТЕ СА ТУК!", "Радио Вероника"))
        assertNull(display("Easy Radio - Your Relaxing Music Mix!", "Easy Radio"))
        assertNull(display("Radio CyberFolk - Ти Избираш!", "Радио Cyberfolk"))
    }

    @Test
    fun technicalJunkIsRejected() {
        assertNull(display("Hear_the_music.", "Радио Мая Бургас"))
        assertNull(display("Unknown - Unknown"))
        assertNull(display("Ð\u0098Ð·Ð¿ÑŠÐ»Ð½Ð¸Ñ‚ÐµÐ» - Ð¿ÐµÑÐµÐ½")) // UTF-8 read as Latin-1
    }

    @Test
    fun cleansCodesTrackNumbersQuotesAndMissingArtist() {
        assertEquals("Aneliq — Tvoq Sam 2014", display("Aneliq - Tvoq Sam 2014 [23lo]"))
        assertEquals("Alisiq — Vajno li ti e ft flori", display("01 Alisiq - Vajno li ti e ft flori"))
        assertEquals(
            "Banditos Bonitos & Feat Nina — Gimme Sunshine (BEE GOOB MIX)",
            display("\"Banditos Bonitos & Feat Nina - Gimme Sunshine (BEE GOOB MIX)\""),
        )
        assertEquals("Milostinq", display(" - Milostinq"))
    }

    @Test
    fun artistNamesThatLookLikeNumbersOrContainStationWordsAreKept() {
        assertEquals("50 Cent — In Da Club", display("50 Cent - In Da Club"))
        assertEquals("Bossa Nova Band — Samba", display("Bossa Nova Band - Samba", "Радио Nova"))
    }

    @Test
    fun titleWithoutSeparatorIsShownAsIs() {
        assertEquals("JONAS BLUE", display("JONAS BLUE ", "Авторадио"))
        assertEquals("PRAVA MISYL CHUVSTVO I GOVOR", display("PRAVA MISYL CHUVSTVO I GOVOR"))
    }

    @Test
    fun separateId3ArtistAndTitleAreCombined() {
        assertEquals(TrackInfo("Pink Floyd", "Comfortably Numb"), TrackInfo.parse("Comfortably Numb", "Pink Floyd", "BadRock"))
    }
}
