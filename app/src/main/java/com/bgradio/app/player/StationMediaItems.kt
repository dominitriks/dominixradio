package com.bgradio.app.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.bgradio.app.data.model.Station

/** Converts stations to Media3 items. The mediaId is always the station id. */
object StationMediaItems {

    /**
     * @param defaultArtwork shown on the notification / lock screen / car display when the
     *   station has no logo.
     */
    fun toMediaItem(station: Station, url: String = station.streamUrl, defaultArtwork: Uri? = null): MediaItem {
        val artwork = station.logoUrl.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: defaultArtwork
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setDisplayTitle(station.name)
            .setStation(station.name)
            .setArtist(station.subtitle)
            .setGenre(station.genre)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .apply { if (artwork != null) setArtworkUri(artwork) }
            .build()
        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(url)
            .apply { mimeTypeFor(url)?.let { setMimeType(it) } }
            .setMediaMetadata(metadata)
            .build()
    }

    /** A browsable folder for Android Auto / media browsers. */
    fun folder(id: String, title: String, artwork: Uri? = null): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                .apply { if (artwork != null) setArtworkUri(artwork) }
                .build()
        )
        .build()

    fun withUrl(item: MediaItem, url: String): MediaItem =
        item.buildUpon().setUri(url).setMimeType(mimeTypeFor(url)).build()

    private fun mimeTypeFor(url: String): String? =
        if (url.contains(".m3u8", ignoreCase = true)) MimeTypes.APPLICATION_M3U8 else null
}
