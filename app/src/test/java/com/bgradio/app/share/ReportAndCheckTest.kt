package com.bgradio.app.share

import com.bgradio.app.data.model.Station
import com.bgradio.app.data.repository.CheckResult
import com.bgradio.app.data.repository.ProbeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReportAndCheckTest {

    private val station = Station("zrock", "Z-Rock", "http://193.108.24.6:8000/zrock", emptyList(), "Рок", "София", "", true, false)
    private val sofia = TimeZone.getTimeZone("Europe/Sofia")
    private val time = Calendar.getInstance(sofia).apply { clear(); set(2026, 9, 11, 19, 20) }.timeInMillis

    @Test
    fun reportContainsRequiredFieldsOnly() {
        val r = StationReport(station, ReportReason.DROPS, "  Прекъсва на всеки 2 минути  ", "1.3.0", time, sofia)
        val text = r.text
        assertTrue(text.contains("Станция: Z-Rock"))
        assertTrue(text.contains("Идентификатор: zrock"))
        assertTrue(text.contains("Проблем: Потокът прекъсва постоянно"))
        assertTrue(text.contains("Версия на DOMINIX: 1.3.0"))
        assertTrue(text.contains("Дата и час: 11.10.2026 19:20"))
        assertTrue(text.contains("Прекъсва на всеки 2 минути"))
        assertFalse(text.contains("  Прекъсва")) // trimmed
        assertEquals("Проблем със станция: Z-Rock — Потокът прекъсва постоянно", r.subject)
    }

    @Test
    fun emptyDescriptionIsOmittedAndLongOneIsCut() {
        assertFalse(StationReport(station, ReportReason.OTHER, "   ", "1.3.0", time, sofia).text.contains("Описание"))
        val long = StationReport(station, ReportReason.OTHER, "а".repeat(2000), "1.3.0", time, sofia)
        assertEquals(StationReport.MAX_DESCRIPTION, long.cleanDescription.length)
    }

    @Test
    fun checkNeverBlamesStationWhenOffline() {
        assertEquals(CheckResult.NoInternet, CheckResult.classify(false, listOf(listOf(ProbeResult.Failed("x")))))
    }

    @Test
    fun checkNeedsAllAttemptsToFailBeforeReportingNotResponding() {
        val ok = CheckResult.classify(true, listOf(listOf(ProbeResult.Failed("изтече времето"), ProbeResult.Audio)))
        assertEquals(CheckResult.Works, ok)

        val fallback = CheckResult.classify(
            true,
            listOf(listOf(ProbeResult.Failed("HTTP 404"), ProbeResult.Failed("HTTP 404")), listOf(ProbeResult.Audio)),
        )
        assertEquals(CheckResult.WorksViaFallback, fallback)

        val down = CheckResult.classify(true, listOf(listOf(ProbeResult.Failed("a"), ProbeResult.Failed("изтече времето"))))
        assertEquals(CheckResult.NotResponding(2, "изтече времето"), down)
        assertTrue(down.message.contains("може да е временно"))
    }
}
