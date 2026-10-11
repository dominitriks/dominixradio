package com.bgradio.app.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorsTest {

    private fun error(code: Int) = PlaybackException("test", null, code)

    @Test
    fun offlineAlwaysSaysNoInternet() {
        assertEquals(
            PlaybackErrors.NO_INTERNET,
            PlaybackErrors.message(error(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS), online = false),
        )
    }

    @Test
    fun messagesAreInBulgarian() {
        val timeout = PlaybackErrors.message(error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT), true)
        assertTrue(timeout.contains("не отговаря"))
        val unavailable = PlaybackErrors.message(error(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS), true)
        assertTrue(unavailable.contains("временно недостъпна"))
        val invalid = PlaybackErrors.message(error(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED), true)
        assertTrue(invalid.contains("невалиден"))
    }

    @Test
    fun networkErrorsAreRetriedButBrokenStreamsAreNot() {
        assertTrue(PlaybackErrors.isRecoverable(error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)))
        assertTrue(PlaybackErrors.isRecoverable(error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)))
        assertFalse(PlaybackErrors.isRecoverable(error(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)))
        assertFalse(PlaybackErrors.isRecoverable(error(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED)))
        assertFalse(PlaybackErrors.isRecoverable(error(PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED)))
    }
}
