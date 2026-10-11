package com.bgradio.app.utils

import com.bgradio.app.data.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {

    private fun station(id: String, name: String, genre: String, city: String = "", popular: Boolean = false) =
        Station(id, name, "http://x/$id", emptyList(), genre, city, "", popular, needsCheck = false)

    private val radio1 = station("radio1", "Радио 1", "Хитове", "София", popular = true)
    private val zrock = station("zrock", "Z-Rock", "Рок", "София")
    private val darik = station("darik", "Дарик Радио", "Новини", "София", popular = true)
    private val katra = station("katra", "Катра FM", "Хитове", "Пловдив")

    @Test
    fun matchesByNameGenreAndCity() {
        assertTrue(SearchUtils.matches(radio1, "Радио 1"))
        assertTrue(SearchUtils.matches(zrock, "рок"))
        assertTrue(SearchUtils.matches(katra, "Пловдив"))
        assertFalse(SearchUtils.matches(katra, "София"))
    }

    @Test
    fun latinQueryFindsCyrillicNames() {
        assertTrue(SearchUtils.matches(radio1, "radio 1"))
        assertTrue(SearchUtils.matches(darik, "darik"))
        assertTrue(SearchUtils.matches(katra, "plovdiv"))
        // "rock" and "рок" are treated alike
        assertTrue(SearchUtils.matches(zrock, "rock"))
    }

    @Test
    fun allWordsMustMatch() {
        assertTrue(SearchUtils.matches(darik, "дарик софия"))
        assertFalse(SearchUtils.matches(darik, "дарик пловдив"))
    }

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(SearchUtils.matches(zrock, "   "))
    }

    @Test
    fun popularSortKeepsCatalogOrderWithinGroups() {
        val sorted = SearchUtils.sort(listOf(zrock, radio1, katra, darik), StationSort.POPULAR)
        assertEquals(listOf("radio1", "darik", "zrock", "katra"), sorted.map { it.id })
    }

    @Test
    fun nameSortUsesBulgarianAlphabet() {
        val sorted = SearchUtils.sort(listOf(radio1, katra, darik, zrock), StationSort.NAME)
        // Latin before Cyrillic, then Д < К < Р
        assertEquals(listOf("zrock", "darik", "katra", "radio1"), sorted.map { it.id })
    }
}
