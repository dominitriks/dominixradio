package com.bgradio.app.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Regression test: the MediaSession must receive every playback event from [RadioPlayer].
 * If it doesn't, Media3 never sees the player playing, shows no notification and never starts
 * the foreground service — and the radio stops as soon as the phone is locked.
 */
class RadioPlayerListenerTest {

    private var innerListener: Player.Listener? = null
    private var icyTitle: String? = null

    private val stationItem = MediaItem.Builder()
        .setMediaId("radio1")
        .setMediaMetadata(MediaMetadata.Builder().setTitle("Радио 1").setArtist("Хитове · София").build())
        .build()

    /** Minimal fake ExoPlayer: records the listener RadioPlayer registers. */
    private val fakePlayer = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, args ->
        when (method.name) {
            "addListener" -> { innerListener = args[0] as Player.Listener; null }
            "removeListener" -> { innerListener = null; null }
            "getCurrentMediaItem" -> stationItem
            "getMediaMetadata" -> MediaMetadata.Builder()
                .setTitle(icyTitle ?: "Радио 1")
                .setArtist("Хитове · София")
                .build()
            "getMediaItemCount" -> 1
            else -> defaultValue(method.returnType)
        }
    } as Player

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        else -> null
    }

    private class RecordingListener : Player.Listener {
        val events = mutableListOf<String>()
        var metadata: MediaMetadata? = null
        override fun onPlaybackStateChanged(playbackState: Int) { events += "state:$playbackState" }
        override fun onIsPlayingChanged(isPlaying: Boolean) { events += "playing:$isPlaying" }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { events += "pwr:$playWhenReady" }
        override fun onEvents(player: Player, events: Player.Events) { this.events += "events" }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { metadata = mediaMetadata }
    }

    @Test
    fun sessionListenerReceivesPlaybackEvents() {
        val radio = RadioPlayer(fakePlayer, canPlay = { true })
        val session = RecordingListener()
        radio.addListener(session)

        val inner = innerListener!!
        inner.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        inner.onPlaybackStateChanged(Player.STATE_BUFFERING)
        inner.onPlaybackStateChanged(Player.STATE_READY)
        inner.onIsPlayingChanged(true)
        inner.onEvents(fakePlayer, Player.Events(androidx.media3.common.FlagSet.Builder().build()))

        assertEquals(
            listOf("pwr:true", "state:${Player.STATE_BUFFERING}", "state:${Player.STATE_READY}", "playing:true", "events"),
            session.events,
        )
    }

    @Test
    fun stationNameStaysTitleAndSongBecomesSubtitle() {
        val radio = RadioPlayer(fakePlayer, canPlay = { true })
        val session = RecordingListener()
        radio.addListener(session)

        icyTitle = "Artist - Song"
        innerListener!!.onMediaMetadataChanged(MediaMetadata.EMPTY)

        assertEquals("Радио 1", session.metadata?.title.toString())
        assertEquals("Artist — Song", session.metadata?.artist.toString())
    }

    @Test
    fun junkStreamTitleIsNotShown() {
        val radio = RadioPlayer(fakePlayer, canPlay = { true })
        val session = RecordingListener()
        radio.addListener(session)

        icyTitle = "apteka.puls.bg - APTEKI PULS OCTOBER 2026"
        innerListener!!.onMediaMetadataChanged(MediaMetadata.EMPTY)

        assertEquals("Радио 1", session.metadata?.title.toString())
        assertEquals("Хитове · София", session.metadata?.artist.toString()) // falls back to genre · city
    }

    @Test
    fun removeListenerUnregistersWrapper() {
        val radio = RadioPlayer(fakePlayer, canPlay = { true })
        val session = RecordingListener()
        radio.addListener(session)
        assertTrue(innerListener != null)
        radio.removeListener(session)
        assertEquals(null, innerListener)
    }
}
