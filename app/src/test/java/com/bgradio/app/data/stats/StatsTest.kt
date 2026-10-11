package com.bgradio.app.data.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatsTest {

    private val sofia = TimeZone.getTimeZone("Europe/Sofia")

    private fun wallAt(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0, s: Int = 0): Long =
        Calendar.getInstance(sofia).apply { clear(); set(y, mo - 1, d, h, mi, s) }.timeInMillis

    /** In-memory sink that applies increments exactly like StatsRepository does. */
    private class MemorySink(private val zone: TimeZone) : ListeningTracker.Sink {
        var data = StatsData()
        override fun addTime(stationId: String, startWallMs: Long, ms: Long) {
            val daily = data.daily.toMutableMap()
            for ((day, part) in Days.split(startWallMs, ms, zone)) {
                val m = (daily[day] ?: emptyMap()).toMutableMap()
                m[stationId] = (m[stationId] ?: 0L) + part
                daily[day] = m
            }
            data = data.copy(daily = daily)
        }
        override fun sessionStarted(stationId: String, startWallMs: Long) {
            data = data.copy(sessions = listOf(ListeningSession(stationId, startWallMs, 0)) + data.sessions, sessionCount = data.sessionCount + 1)
        }
        override fun sessionUpdated(stationId: String, startWallMs: Long, totalMs: Long) {
            data = data.copy(sessions = data.sessions.map {
                if (it.stationId == stationId && it.startMs == startWallMs) it.copy(seconds = totalMs / 1000) else it
            })
        }
        fun ms(day: String, id: String) = data.daily[day]?.get(id) ?: 0L
    }

    private var wall = 0L
    private var elapsed = 1_000_000L
    private val sink = MemorySink(sofia)
    private val tracker = ListeningTracker({ wall }, { elapsed }, sink)

    private fun advance(ms: Long) {
        wall += ms
        elapsed += ms
    }

    @Test
    fun countsOnlyPlayingTime() {
        wall = wallAt(2026, 10, 11, 10)
        tracker.onPlaying("radio1")
        advance(5 * 60_000)
        tracker.onStopped() // paused
        advance(60 * 60_000) // an hour paused: not counted
        assertEquals(5 * 60_000L, sink.ms("2026-10-11", "radio1"))
    }

    @Test
    fun periodicFlushesNeverDoubleCount() {
        wall = wallAt(2026, 10, 11, 10)
        tracker.onPlaying("radio1")
        repeat(10) {
            advance(60_000)
            tracker.flush()
            tracker.flush() // a second flush right away adds nothing
        }
        tracker.onStopped()
        tracker.onStopped()
        tracker.finish()
        assertEquals(10 * 60_000L, sink.ms("2026-10-11", "radio1"))
        assertEquals(1L, sink.data.sessionCount)
        assertEquals(600L, sink.data.sessions.single().seconds)
    }

    @Test
    fun sessionAcrossMidnightIsSplitBetweenDays() {
        wall = wallAt(2026, 10, 11, 23, 50)
        tracker.onPlaying("darik")
        advance(30 * 60_000) // until 00:20
        tracker.finish()
        assertEquals(10 * 60_000L, sink.ms("2026-10-11", "darik"))
        assertEquals(20 * 60_000L, sink.ms("2026-10-12", "darik"))
        assertEquals(1L, sink.data.sessionCount) // still one session
    }

    @Test
    fun shortBufferingKeepsTheSameSessionButIsNotCounted() {
        wall = wallAt(2026, 10, 11, 8)
        tracker.onPlaying("zrock")
        advance(3 * 60_000)
        tracker.onStopped() // buffering
        advance(20_000)
        tracker.onPlaying("zrock")
        advance(2 * 60_000)
        tracker.finish()
        assertEquals(5 * 60_000L, sink.ms("2026-10-11", "zrock"))
        assertEquals(1L, sink.data.sessionCount)
        assertEquals(300L, sink.data.sessions.single().seconds)
    }

    @Test
    fun longPauseOrStationChangeStartsNewSession() {
        wall = wallAt(2026, 10, 11, 8)
        tracker.onPlaying("zrock")
        advance(60_000)
        tracker.onStopped()
        advance(10 * 60_000) // longer than the grace period
        tracker.onPlaying("zrock")
        advance(60_000)
        tracker.onPlaying("radio1") // switched station while playing
        advance(60_000)
        tracker.finish()
        assertEquals(3L, sink.data.sessionCount)
        assertEquals(listOf("radio1", "zrock", "zrock"), sink.data.sessions.map { it.stationId })
    }

    @Test
    fun zappingUnderTenSecondsCountsTimeButNoSession() {
        wall = wallAt(2026, 10, 11, 8)
        tracker.onPlaying("a")
        advance(4_000)
        tracker.onPlaying("b")
        advance(4_000)
        tracker.finish()
        assertEquals(0L, sink.data.sessionCount)
        assertEquals(4_000L, sink.ms("2026-10-11", "a"))
    }

    @Test
    fun summaryTotalsTodayWeekMonthAndTop5() {
        val now = wallAt(2026, 10, 11, 12)
        val h = 3_600_000L
        val data = StatsData(
            daily = mapOf(
                "2026-10-11" to mapOf("radio1" to 1 * h, "darik" to 2 * h),
                "2026-10-06" to mapOf("radio1" to 1 * h), // inside last 7 days
                "2026-10-01" to mapOf("zrock" to 4 * h), // this month, not last 7 days
                "2026-09-30" to mapOf("zrock" to 1 * h, "a" to h, "b" to h, "c" to h), // last month
            ),
            sessionCount = 9,
        )
        val s = StatsSummary.of(data, now, chartDays = 14, zone = sofia)
        assertEquals(12 * 3600L, s.totalSeconds)
        assertEquals(3 * 3600L, s.todaySeconds)
        assertEquals(4 * 3600L, s.last7DaysSeconds)
        assertEquals(8 * 3600L, s.thisMonthSeconds)
        assertEquals(9L, s.sessionCount)
        assertEquals(listOf("zrock", "darik", "radio1", "a", "b"), s.topStations.map { it.first })
        assertEquals(14, s.chart.size)
        assertEquals("2026-10-11", s.chart.last().first)
        assertEquals(3 * 3600L, s.chart.last().second)
    }

    @Test
    fun jsonRoundTrip() {
        val d = StatsData(
            daily = mapOf("2026-10-11" to mapOf("radio1" to 123_456L)),
            sessions = listOf(ListeningSession("radio1", 1_760_000_000_000, 123)),
            sessionCount = 7,
        )
        assertEquals(d, StatsJson.decode(StatsJson.encode(d)))
    }

    @Test
    fun formatsDurationsInBulgarian() {
        assertEquals("45 сек", StatsSummary.formatDuration(45))
        assertEquals("12 мин", StatsSummary.formatDuration(12 * 60))
        assertEquals("2 ч 05 мин", StatsSummary.formatDuration(2 * 3600 + 5 * 60))
        assertTrue(StatsSummary.of(StatsData(), wallAt(2026, 10, 11, 12), zone = sofia).isEmpty)
    }
}
