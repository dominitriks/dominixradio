package com.bgradio.app.player

import android.os.Bundle
import androidx.media3.session.SessionCommand

/**
 * Shared constants between the UI-side controller and [com.bgradio.app.service.PlaybackService].
 * The service reports reconnect/error status through the session extras.
 */
object PlayerContract {
    const val ACTION_STOP = "com.bgradio.app.STOP"
    val STOP_COMMAND = SessionCommand(ACTION_STOP, Bundle.EMPTY)

    const val EXTRA_STATUS = "status"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_ATTEMPT = "attempt"
    const val EXTRA_MAX_ATTEMPTS = "max_attempts"
    const val EXTRA_STATION_ID = "station_id"
    const val EXTRA_SEQ = "seq"

    const val STATUS_IDLE = "idle"
    const val STATUS_RECONNECTING = "reconnecting"
    const val STATUS_ERROR = "error"
}
