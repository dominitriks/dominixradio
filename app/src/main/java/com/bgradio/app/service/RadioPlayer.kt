package com.bgradio.app.service

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.bgradio.app.player.TrackInfo

/**
 * Radio-specific behaviour on top of ExoPlayer:
 *  - Previous/Next always switch station (and wrap around), never "seek to start".
 *  - Resuming after a longer pause jumps back to the live edge.
 *  - [canPlay] lets the service block playback (e.g. "Wi-Fi only").
 */
@OptIn(UnstableApi::class)
class RadioPlayer(
    player: Player,
    private val canPlay: () -> Boolean,
) : ForwardingPlayer(player) {

    private var pausedAt = 0L

    override fun play() {
        if (mediaItemCount == 0 || !canPlay()) return
        if (playbackState == STATE_IDLE) {
            prepare()
        } else if (pausedAt != 0L && SystemClock.elapsedRealtime() - pausedAt > LIVE_RESYNC_MS) {
            seekToDefaultPosition()
        }
        pausedAt = 0L
        super.play()
    }

    override fun pause() {
        pausedAt = SystemClock.elapsedRealtime()
        super.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) play() else pause()
    }

    override fun seekToNext() = moveBy(1)
    override fun seekToNextMediaItem() = moveBy(1)
    override fun seekToPrevious() = moveBy(-1)
    override fun seekToPreviousMediaItem() = moveBy(-1)

    override fun hasNextMediaItem(): Boolean = mediaItemCount > 1
    override fun hasPreviousMediaItem(): Boolean = mediaItemCount > 1

    override fun isCommandAvailable(command: Int): Boolean =
        if (command in STATION_SWITCH_COMMANDS) mediaItemCount > 1 else super.isCommandAvailable(command)

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        if (mediaItemCount <= 1) return base
        return base.buildUpon().addAll(*STATION_SWITCH_COMMANDS).build()
    }

    /**
     * Streams send the current song (ICY StreamTitle / HLS ID3) as "title" (and sometimes
     * "artist"). Keep the station name as the title in the notification / lock screen and show the
     * song as the subtitle — but only when [TrackInfo] considers it real track information.
     * Artwork always stays the station logo (no covers from the stream).
     */
    override fun getMediaMetadata(): MediaMetadata {
        val live = super.getMediaMetadata()
        val item = currentMediaItem?.mediaMetadata ?: return live
        val stationName = item.title?.toString().orEmpty()
        val streamTitle = live.title?.toString()?.takeIf { it != stationName }
        val streamArtist = live.artist?.toString()?.takeIf { it != item.artist?.toString() }
        val track = TrackInfo.parse(streamTitle, streamArtist, stationName)
        return live.buildUpon()
            .setTitle(item.title)
            .setDisplayTitle(item.title)
            .setArtist(track?.display ?: item.artist)
            .setArtworkData(null, null)
            .setArtworkUri(item.artworkUri)
            .build()
    }

    private val listenerWrappers = HashMap<Player.Listener, Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        val wrapper = MetadataRewritingListener(this, listener)
        listenerWrappers[listener] = wrapper
        super.addListener(wrapper)
    }

    override fun removeListener(listener: Player.Listener) {
        listenerWrappers.remove(listener)?.let { super.removeListener(it) }
    }

    private fun moveBy(delta: Int) {
        val count = mediaItemCount
        if (count == 0) return
        val target = ((currentMediaItemIndex + delta) % count + count) % count
        seekToDefaultPosition(target)
        play()
    }

    private companion object {
        const val LIVE_RESYNC_MS = 10_000L
        val STATION_SWITCH_COMMANDS = intArrayOf(
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )
    }
}
