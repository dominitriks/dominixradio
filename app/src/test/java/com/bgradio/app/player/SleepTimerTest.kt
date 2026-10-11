package com.bgradio.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {

    private var clock = 1_000_000L
    private val timer = SleepTimer { clock }

    @Test
    fun startSetsEndTimeAndDelay() {
        val delay = timer.start(30)
        assertEquals(30 * 60_000L, delay)
        assertEquals(1_000_000L + 30 * 60_000L, timer.endsAt)
        assertTrue(timer.isActive)
    }

    @Test
    fun remainingCountsDownAndNeverGoesNegative() {
        timer.start(15)
        clock += 5 * 60_000L
        assertEquals(10 * 60_000L, timer.remainingMs())
        clock += 60 * 60_000L
        assertEquals(0L, timer.remainingMs())
    }

    @Test
    fun zeroOrNegativeCancels() {
        timer.start(45)
        assertNull(timer.start(0))
        assertFalse(timer.isActive)
        assertNull(timer.remainingMs())
    }

    @Test
    fun restartReplacesPreviousTimer() {
        timer.start(90)
        timer.start(15)
        assertEquals(15 * 60_000L, timer.remainingMs())
    }

    @Test
    fun formatsCountdown() {
        assertEquals("15:00", SleepTimer.format(15 * 60_000L))
        assertEquals("00:01", SleepTimer.format(1))
        assertEquals("1:30:00", SleepTimer.format(90 * 60_000L))
    }
}
