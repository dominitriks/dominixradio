package com.bgradio.app.data.stats

import java.util.Calendar
import java.util.TimeZone

/** One finished listening session (continuous listening to one station). */
data class ListeningSession(
    val stationId: String,
    /** Wall-clock start, ms since epoch. */
    val startMs: Long,
    /** Seconds actually playing (pauses / buffering are not counted). */
    val seconds: Long,
)

/**
 * All statistics, stored locally.
 * [daily] = day key ("2026-10-11", local time) → station id → milliseconds listened that day.
 * Every total is derived from [daily], so the session history can be capped without losing totals.
 */
data class StatsData(
    val daily: Map<String, Map<String, Long>> = emptyMap(),
    /** Newest first, capped at [MAX_SESSIONS]. */
    val sessions: List<ListeningSession> = emptyList(),
    /** All sessions ever counted (history list itself is capped). */
    val sessionCount: Long = 0,
) {
    companion object {
        const val MAX_SESSIONS = 200
    }
}

/** Day keys and midnight handling with java.util.Calendar (java.time needs API 26; app supports 24). */
object Days {
    fun key(wallMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = wallMs }
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** Start of the next local day after [wallMs]. */
    fun nextMidnight(wallMs: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(zone).apply {
            timeInMillis = wallMs
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

    /** Splits [startMs, startMs + durationMs) into per-day millisecond amounts. */
    fun split(startMs: Long, durationMs: Long, zone: TimeZone = TimeZone.getDefault()): List<Pair<String, Long>> {
        val out = ArrayList<Pair<String, Long>>()
        var t = startMs
        val end = startMs + durationMs
        while (t < end) {
            val boundary = minOf(nextMidnight(t, zone), end)
            out += key(t, zone) to (boundary - t)
            t = boundary
        }
        return out
    }

    /** The last [count] day keys ending with the day of [nowMs], oldest first. */
    fun lastDays(nowMs: Long, count: Int, zone: TimeZone = TimeZone.getDefault()): List<String> {
        val c = Calendar.getInstance(zone).apply { timeInMillis = nowMs }
        val keys = ArrayList<String>(count)
        repeat(count) {
            keys += key(c.timeInMillis, zone)
            c.add(Calendar.DAY_OF_MONTH, -1)
        }
        return keys.reversed()
    }
}

/** Numbers shown on the statistics screen. */
data class StatsSummary(
    val totalSeconds: Long,
    val todaySeconds: Long,
    val last7DaysSeconds: Long,
    val thisMonthSeconds: Long,
    val sessionCount: Long,
    /** station id → seconds, best first (max 5). */
    val topStations: List<Pair<String, Long>>,
    /** day key → seconds for the chart, oldest first. */
    val chart: List<Pair<String, Long>>,
    val recentSessions: List<ListeningSession>,
) {
    val isEmpty: Boolean get() = totalSeconds == 0L && recentSessions.isEmpty()

    companion object {
        fun of(data: StatsData, nowMs: Long, chartDays: Int = 14, zone: TimeZone = TimeZone.getDefault()): StatsSummary {
            val today = Days.key(nowMs, zone)
            val week = Days.lastDays(nowMs, 7, zone).toSet()
            val month = today.substring(0, 7) // "2026-10"
            fun dayMs(day: String) = data.daily[day]?.values?.sum() ?: 0L

            val perStation = HashMap<String, Long>()
            data.daily.values.forEach { m -> m.forEach { (id, ms) -> perStation[id] = (perStation[id] ?: 0L) + ms } }

            return StatsSummary(
                totalSeconds = perStation.values.sum() / 1000,
                todaySeconds = dayMs(today) / 1000,
                last7DaysSeconds = week.sumOf(::dayMs) / 1000,
                thisMonthSeconds = data.daily.filterKeys { it.startsWith(month) }.values.sumOf { it.values.sum() } / 1000,
                sessionCount = data.sessionCount,
                topStations = perStation.entries.filter { it.value >= 1000 }
                    .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
                    .take(5).map { it.key to it.value / 1000 },
                chart = Days.lastDays(nowMs, chartDays, zone).map { it to dayMs(it) / 1000 },
                recentSessions = data.sessions,
            )
        }

        /** "2 ч 05 мин", "12 мин", "45 сек". */
        fun formatDuration(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            return when {
                h > 0 -> "$h ч %02d мин".format(m)
                m > 0 -> "$m мин"
                else -> "$seconds сек"
            }
        }
    }
}
