package com.bgradio.app.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bgradio.app.data.model.Station
import com.bgradio.app.data.repository.StationRepository
import com.bgradio.app.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlayerUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    /** User wants audio (pressed Play) — true also while loading/reconnecting. */
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = false,
    val nowPlaying: String? = null,
    val reconnectMessage: String? = null,
    val errorMessage: String? = null,
    /** Changes every time the service reports a new error, so the UI can show it once. */
    val errorSeq: Int = 0,
    val volume: Float = 1f,
) {
    val isLoading: Boolean get() = playWhenReady && (isBuffering || reconnectMessage != null) && !isPlaying
    val hasStation: Boolean get() = station != null
}

/**
 * UI-side handle to [PlaybackService]. Connects a MediaController while the activity is visible
 * and exposes the player state as a StateFlow.
 */
@OptIn(UnstableApi::class)
class PlayerConnection(
    private val context: Context,
    private val stations: StationRepository,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrElse {
                Log.e(TAG, "Cannot connect to playback service", it)
                future = null
                pending.clear()
                return@addListener
            }
            if (future !== f) { // released meanwhile
                c.release()
                return@addListener
            }
            controller = c
            c.addListener(playerListener)
            syncFromController(c)
            // Show an ongoing reconnect, but don't replay an old error when the app is reopened.
            if (c.sessionExtras.getString(PlayerContract.EXTRA_STATUS) == PlayerContract.STATUS_RECONNECTING) {
                applyExtras(c.sessionExtras)
            }
            pending.toList().forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        val f = future
        controller?.removeListener(playerListener)
        controller = null
        future = null
        pending.clear()
        f?.let { MediaController.releaseFuture(it) }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null && c.isConnected) block(c) else {
            pending += block
            connect()
        }
    }

    // ---- Commands -------------------------------------------------------------------------

    /** Plays [station]; [queue] defines what Previous/Next move through. */
    fun play(station: Station, queue: List<Station>) {
        _state.update { it.copy(station = station, playWhenReady = true, errorMessage = null, reconnectMessage = null) }
        withController { c ->
            val list = queue.takeIf { q -> q.any { it.id == station.id } } ?: listOf(station)
            val start = list.indexOfFirst { it.id == station.id }
            c.setMediaItems(list.map { requestItem(it) }, start, C.TIME_UNSET)
            c.prepare()
            c.play()
        }
    }

    fun toggle(station: Station, queue: List<Station>) {
        val s = _state.value
        if (s.station?.id == station.id && controller?.currentMediaItem?.mediaId == station.id) {
            if (s.playWhenReady) pause() else resume()
        } else {
            play(station, queue)
        }
    }

    fun resume() = withController { c ->
        _state.update { it.copy(errorMessage = null) }
        if (c.mediaItemCount == 0) {
            _state.value.station?.let { play(it, listOf(it)) }
        } else {
            c.play()
        }
    }

    fun pause() = withController { it.pause() }

    fun stop() = withController { c ->
        c.sendCustomCommand(PlayerContract.STOP_COMMAND, Bundle.EMPTY)
        _state.update { PlayerUiState(volume = it.volume) }
    }

    fun next() = withController { it.seekToNext() }

    fun previous() = withController { it.seekToPrevious() }

    fun setVolume(volume: Float) {
        _state.update { it.copy(volume = volume) }
        withController { it.volume = volume.coerceIn(0f, 1f) }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }

    /** Only the id travels to the service; it resolves the stream URL itself. */
    private fun requestItem(station: Station): MediaItem = MediaItem.Builder()
        .setMediaId(station.id)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(station.name).build())
        .build()

    // ---- State ----------------------------------------------------------------------------

    private fun syncFromController(c: Player) {
        val id = c.currentMediaItem?.mediaId
        val station = stations.find(id)
        // The service puts the current song (if the stream sends one) into "artist".
        val song = c.mediaMetadata.artist?.toString()
        _state.update {
            it.copy(
                station = station ?: if (c.mediaItemCount == 0) null else it.station,
                isPlaying = c.isPlaying,
                playWhenReady = c.playWhenReady,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                nowPlaying = song?.takeIf { t -> t.isNotBlank() && t != station?.subtitle && t != station?.name },
                volume = c.volume,
            )
        }
    }

    private fun applyExtras(extras: Bundle) {
        val status = extras.getString(PlayerContract.EXTRA_STATUS) ?: return
        val message = extras.getString(PlayerContract.EXTRA_MESSAGE)
        val seq = extras.getInt(PlayerContract.EXTRA_SEQ)
        _state.update {
            when (status) {
                PlayerContract.STATUS_RECONNECTING -> it.copy(
                    reconnectMessage = "Повторно свързване… (опит ${extras.getInt(PlayerContract.EXTRA_ATTEMPT)} от " +
                        "${extras.getInt(PlayerContract.EXTRA_MAX_ATTEMPTS)})",
                    errorMessage = null,
                )

                PlayerContract.STATUS_ERROR -> it.copy(
                    reconnectMessage = null,
                    errorMessage = message ?: PlaybackErrors.CONNECT_FAILED,
                    errorSeq = seq,
                    playWhenReady = false,
                )

                else -> it.copy(reconnectMessage = null, errorMessage = null)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncFromController(player)
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) = applyExtras(extras)

        override fun onDisconnected(controller: MediaController) {
            // The service went away (e.g. stopped after Stop / idle timeout): nothing is playing.
            if (this@PlayerConnection.controller === controller) {
                this@PlayerConnection.controller = null
                future = null
                _state.update { PlayerUiState(volume = it.volume) }
            }
        }
    }

    private companion object {
        const val TAG = "PlayerConnection"
    }
}
