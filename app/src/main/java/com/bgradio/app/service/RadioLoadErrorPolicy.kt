package com.bgradio.app.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo

/**
 * Reconnect strategy for live radio.
 *
 * Network errors are retried *inside* ExoPlayer, so the player stays in BUFFERING instead of
 * dropping to IDLE. That keeps the foreground service and the media notification alive while the
 * connection is down (screen off, in the car, in a tunnel...). Retries are limited and spaced out
 * so a dead station never drains the battery.
 *
 * Called on the playback thread.
 */
@OptIn(UnstableApi::class)
class RadioLoadErrorPolicy(
    /** Max retries for the current stream (0 = give up immediately). */
    private val maxAttempts: () -> Int,
    /** True when it is better to stop retrying and switch to a fallback URL. */
    private val preferFallback: (errorCount: Int) -> Boolean,
    private val onRetry: (attempt: Int, max: Int) -> Unit,
) : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
        val error = loadErrorInfo.exception
        // Parser errors, 404, cleartext blocked... — retrying will not help.
        if (super.getRetryDelayMsFor(loadErrorInfo) == C.TIME_UNSET) return C.TIME_UNSET
        if (error is HttpDataSource.InvalidResponseCodeException && error.responseCode in 400..499) {
            return C.TIME_UNSET
        }
        val count = loadErrorInfo.errorCount
        val max = maxAttempts()
        if (count > max || preferFallback(count)) return C.TIME_UNSET
        onRetry(count, max)
        return DELAYS_MS[(count - 1).coerceIn(0, DELAYS_MS.lastIndex)]
    }

    /** Errors surface only when we give up (TIME_UNSET above), never while still retrying. */
    override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE

    companion object {
        /** ~4 minutes in total before giving up. */
        val DELAYS_MS = longArrayOf(1_000, 2_000, 3_000, 5_000, 8_000, 10_000, 15_000, 20_000, 30_000, 30_000, 30_000, 30_000, 30_000)
        val MAX_ATTEMPTS = DELAYS_MS.size
        const val MAX_ATTEMPTS_NO_AUTO_RECONNECT = 2
    }
}
