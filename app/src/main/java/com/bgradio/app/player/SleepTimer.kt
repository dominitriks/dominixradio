package com.bgradio.app.player

/**
 * Sleep timer state, independent of Android so it can be unit-tested.
 * Times are in the `SystemClock.elapsedRealtime()` time base (keeps counting in deep sleep and
 * is not affected by the user changing the clock).
 */
class SleepTimer(private val now: () -> Long) {

    /** elapsedRealtime when playback should stop, or null when the timer is off. */
    var endsAt: Long? = null
        private set

    val isActive: Boolean get() = endsAt != null

    /** Starts (or replaces) the timer. [minutes] <= 0 cancels it. Returns the delay in ms. */
    fun start(minutes: Int): Long? {
        if (minutes <= 0) {
            cancel()
            return null
        }
        val delay = minutes * 60_000L
        endsAt = now() + delay
        return delay
    }

    fun cancel() {
        endsAt = null
    }

    /** Remaining time in ms (never negative), or null when off. */
    fun remainingMs(): Long? = endsAt?.let { (it - now()).coerceAtLeast(0) }

    companion object {
        /** Choices offered in the UI, in minutes. */
        val OPTIONS = listOf(15, 30, 45, 60, 90, 120)

        /** Fade-out before pausing so the radio doesn't stop abruptly. */
        const val FADE_OUT_MS = 10_000L
        const val FADE_STEPS = 20

        /** "MM:SS" or "H:MM:SS" for the countdown label. */
        fun format(ms: Long): String {
            val total = (ms + 999) / 1000
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }
    }
}
