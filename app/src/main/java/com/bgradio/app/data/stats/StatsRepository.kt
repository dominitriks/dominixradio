package com.bgradio.app.data.stats

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local listening statistics in one small JSON file (no database, nothing leaves the phone).
 * The playback service feeds it through [ListeningTracker]; the UI observes [data].
 */
class StatsRepository(private val file: File) : ListeningTracker.Sink {

    private val lock = Any()
    private val _data = MutableStateFlow(StatsData())
    val data: StateFlow<StatsData> = _data.asStateFlow()

    @Volatile private var loaded = false
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    /** Reads the file once (it is small); safe to call repeatedly. */
    fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            _data.value = try {
                if (file.exists()) StatsJson.decode(file.readText()) else StatsData()
            } catch (e: Exception) {
                Log.w(TAG, "Statistics file unreadable, starting fresh", e)
                StatsData()
            }
            loaded = true
        }
    }

    // ---- ListeningTracker.Sink (in memory; written by persist()) -----------------------

    override fun addTime(stationId: String, startWallMs: Long, ms: Long) = mutate { d ->
        val daily = d.daily.mapValues { it.value }.toMutableMap()
        for ((day, part) in Days.split(startWallMs, ms)) {
            val m = (daily[day] ?: emptyMap()).toMutableMap()
            m[stationId] = (m[stationId] ?: 0L) + part // milliseconds, no rounding loss
            daily[day] = m
        }
        d.copy(daily = daily)
    }

    override fun sessionStarted(stationId: String, startWallMs: Long) = mutate { d ->
        d.copy(
            sessions = (listOf(ListeningSession(stationId, startWallMs, 0)) + d.sessions).take(StatsData.MAX_SESSIONS),
            sessionCount = d.sessionCount + 1,
        )
    }

    override fun sessionUpdated(stationId: String, startWallMs: Long, totalMs: Long) = mutate { d ->
        val i = d.sessions.indexOfFirst { it.stationId == stationId && it.startMs == startWallMs }
        if (i < 0) d else d.copy(sessions = d.sessions.toMutableList().also { it[i] = it[i].copy(seconds = totalMs / 1000) })
    }

    private fun mutate(change: (StatsData) -> StatsData) {
        ensureLoaded()
        synchronized(lock) { _data.value = change(_data.value) }
    }

    /** Writes the current state to disk in the background (atomic: temp file + rename). */
    fun persist() {
        val snapshot = _data.value
        io.launch {
            writeMutex.withLock {
                try {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(StatsJson.encode(snapshot))
                    if (!tmp.renameTo(file)) {
                        file.delete()
                        tmp.renameTo(file)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot save statistics", e)
                }
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            _data.value = StatsData()
            loaded = true
        }
        persist()
    }

    private companion object {
        const val TAG = "StatsRepository"
    }
}

/** JSON format of the statistics file. */
object StatsJson {
    /** 2 = daily values in milliseconds. */
    private const val VERSION = 2

    fun encode(d: StatsData): String = JSONObject().apply {
        put("version", VERSION)
        put("sessionCount", d.sessionCount)
        put("daily", JSONObject().apply {
            d.daily.forEach { (day, m) -> put(day, JSONObject().apply { m.forEach { (id, s) -> put(id, s) } }) }
        })
        put("sessions", JSONArray().apply {
            d.sessions.forEach { s -> put(JSONObject().put("s", s.stationId).put("t", s.startMs).put("d", s.seconds)) }
        })
    }.toString()

    fun decode(text: String): StatsData {
        val o = JSONObject(text)
        val dailyJson = o.optJSONObject("daily") ?: JSONObject()
        val daily = dailyJson.keys().asSequence().associateWith { day ->
            val m = dailyJson.getJSONObject(day)
            m.keys().asSequence().associateWith { id -> m.optLong(id) }
        }
        val arr = o.optJSONArray("sessions") ?: JSONArray()
        val sessions = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { s ->
                val id = s.optString("s")
                if (id.isBlank()) null else ListeningSession(id, s.optLong("t"), s.optLong("d"))
            }
        }
        return StatsData(daily, sessions.take(StatsData.MAX_SESSIONS), o.optLong("sessionCount", sessions.size.toLong()))
    }
}
