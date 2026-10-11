package com.bgradio.app.data.stats

/**
 * Counts only time when audio is really playing (ExoPlayer `isPlaying == true`).
 *
 * - Durations use a monotonic clock ([elapsed]) so changing the phone's clock can't distort them;
 *   the wall clock ([wall]) only decides which day the time belongs to.
 * - A short gap (buffering, a quick pause) on the same station continues the same session;
 *   switching station, or a gap longer than [SESSION_GAP_MS], starts a new one.
 * - Time is handed to [sink] as increments; each piece of time is reported exactly once, so
 *   flushing, stopping and restarting can never count it twice.
 */
class ListeningTracker(
    private val wall: () -> Long,
    private val elapsed: () -> Long,
    private val sink: Sink,
) {
    interface Sink {
        /** Listening time to add: [ms] that started at wall time [startWallMs]. */
        fun addTime(stationId: String, startWallMs: Long, ms: Long)

        /** A session that reached [MIN_SESSION_MS] is counted (once). */
        fun sessionStarted(stationId: String, startWallMs: Long)

        /** Current length of a counted session changed / session finished. */
        fun sessionUpdated(stationId: String, startWallMs: Long, totalMs: Long)
    }

    private var stationId: String? = null
    private var sessionStartWall = 0L
    private var sessionMs = 0L
    private var sessionCounted = false

    /** Running segment (audio playing right now). */
    private var segmentStartElapsed = -1L
    private var segmentStartWall = 0L
    /** When playback last stopped, for the "same session" grace period. */
    private var stoppedAtElapsed = -1L

    val isPlaying: Boolean get() = segmentStartElapsed >= 0

    fun onPlaying(id: String) {
        if (isPlaying && id == stationId) return
        if (isPlaying) onStopped() // station changed while playing
        val now = elapsed()
        val continues = id == stationId && stoppedAtElapsed >= 0 && now - stoppedAtElapsed <= SESSION_GAP_MS
        if (!continues) {
            finishSession()
            stationId = id
            sessionStartWall = wall()
            sessionMs = 0
            sessionCounted = false
        }
        segmentStartElapsed = now
        segmentStartWall = wall()
    }

    /** Pause, buffering, error, stop — anything that isn't audible playback. */
    fun onStopped() {
        flush()
        segmentStartElapsed = -1
        stoppedAtElapsed = elapsed()
    }

    /** Reports the time played so far without ending the segment (called periodically). */
    fun flush() {
        val id = stationId ?: return
        if (!isPlaying) return
        val now = elapsed()
        val ms = now - segmentStartElapsed
        if (ms <= 0) return
        sink.addTime(id, segmentStartWall, ms)
        sessionMs += ms
        segmentStartElapsed = now
        segmentStartWall += ms
        if (!sessionCounted && sessionMs >= MIN_SESSION_MS) {
            sessionCounted = true
            sink.sessionStarted(id, sessionStartWall)
        }
        if (sessionCounted) sink.sessionUpdated(id, sessionStartWall, sessionMs)
    }

    /** Ends everything (Stop, service destroyed). */
    fun finish() {
        onStopped()
        finishSession()
        stationId = null
    }

    private fun finishSession() {
        stoppedAtElapsed = -1
    }

    companion object {
        /** Gaps up to this on the same station keep the same session (buffering, short pause). */
        const val SESSION_GAP_MS = 2 * 60_000L
        /** Shorter listening still counts as time, but not as a separate session (station zapping). */
        const val MIN_SESSION_MS = 10_000L
    }
}
