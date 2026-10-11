package com.bgradio.app.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.bgradio.app.BGRadioApp
import com.bgradio.app.BuildConfig
import com.bgradio.app.MainActivity
import com.bgradio.app.R
import com.bgradio.app.data.model.AppSettings
import com.bgradio.app.data.model.Categories
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlaybackErrors
import com.bgradio.app.player.PlayerContract
import com.bgradio.app.player.SleepTimer
import com.bgradio.app.player.StationMediaItems
import com.bgradio.app.utils.SearchUtils
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The audio engine. Runs independently of the UI:
 *
 *  UI → ViewModel → PlayerConnection (MediaController) → MediaSession → this service → ExoPlayer
 *
 * - While audio plays, Media3 keeps this a foreground service (type mediaPlayback) with a media
 *   notification, so playback continues with the screen off / phone locked / app in background.
 *   ExoPlayer holds a partial wake lock + Wi-Fi lock (WAKE_MODE_NETWORK) during playback only.
 * - The MediaSession drives notification, lock screen, Bluetooth headset / car buttons.
 * - As a MediaLibraryService it also exposes a browse tree for Android Auto.
 * - Stop, or 10 minutes paused, releases everything and stops the service.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var exoPlayer: ExoPlayer
    private var session: MediaLibrarySession? = null
    private val container get() = (application as BGRadioApp).container
    private lateinit var defaultArtwork: Uri

    @Volatile private var settings = AppSettings()
    @Volatile private var favoriteIds: Set<String> = emptySet()
    @Volatile private var recentIds: List<String> = emptyList()

    // Per-station connection state.
    private var currentStationId: String? = null
    private var urlIndex = 0
    @Volatile private var hasUntriedFallback = false
    private var endedRestarts = 0
    private var reconnecting = false
    private var recordedStationId: String? = null
    private var statusSeq = 0

    private val idleStop = Runnable {
        if (!exoPlayer.playWhenReady) stopPlayback()
    }

    // Sleep timer: fades out, then pauses. Lives here so it works with the app closed.
    private val sleepTimer = SleepTimer(SystemClock::elapsedRealtime)
    private var fadeStep = 0
    private var fading = false
    private var volumeBeforeFade = 1f
    private val sleepRunnable = Runnable { startFadeOut() }
    private val fadeRunnable = object : Runnable {
        override fun run() {
            fadeStep++
            if (fadeStep >= SleepTimer.FADE_STEPS || !exoPlayer.playWhenReady) {
                finishSleep()
            } else {
                exoPlayer.volume = volumeBeforeFade * (1f - fadeStep.toFloat() / SleepTimer.FADE_STEPS)
                handler.postDelayed(this, SleepTimer.FADE_OUT_MS / SleepTimer.FADE_STEPS)
            }
        }
    }

    // Last published status, re-sent together with the sleep timer state.
    private var lastStatus = PlayerContract.STATUS_IDLE
    private var lastMessage: String? = null
    private var lastAttempt = 0
    private var lastMaxAttempts = 0

    override fun onCreate() {
        super.onCreate()
        defaultArtwork = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .appendPath(R.raw.default_artwork.toString())
            .build()

        val loadErrorPolicy = RadioLoadErrorPolicy(
            maxAttempts = {
                if (settings.autoReconnect) RadioLoadErrorPolicy.MAX_ATTEMPTS
                else RadioLoadErrorPolicy.MAX_ATTEMPTS_NO_AUTO_RECONNECT
            },
            // A different address is worth trying when we are online but this one keeps failing.
            preferFallback = { count -> count >= 3 && hasUntriedFallback && container.network.isOnline() },
            onRetry = { attempt, max -> handler.post { onReconnectAttempt(attempt, max) } },
        )

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Dominix/${BuildConfig.VERSION_NAME} (Android)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(this, httpFactory))
                    .setLoadErrorHandlingPolicy(loadErrorPolicy)
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 45_000, 2_000, 4_000)
                    .build()
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_NETWORK)   // CPU + Wi-Fi stay awake while playing, screen off
            .build()
        exoPlayer.addListener(playerListener)

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        @Suppress("DEPRECATION")
        val stopButton = CommandButton.Builder()
            .setDisplayName(getString(R.string.stop))
            .setIconResId(R.drawable.ic_stop)
            .setSessionCommand(PlayerContract.STOP_COMMAND)
            .build()

        session = MediaLibrarySession.Builder(this, RadioPlayer(exoPlayer, ::checkCanPlay), LibraryCallback())
            .setSessionActivity(openApp)
            .setCustomLayout(ImmutableList.of(stopButton))
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_notification)
            }
        )

        scope.launch { container.prefs.settings.collect { settings = it } }
        scope.launch { container.prefs.favorites.collect { favoriteIds = it } }
        scope.launch { container.prefs.recent.collect { recentIds = it } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away keeps the radio playing. If nothing plays, clean up completely.
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopPlayback()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    // ---- Playback control -----------------------------------------------------------------

    private fun checkCanPlay(): Boolean {
        if (settings.wifiOnly && !container.network.isOnWifi()) {
            publishStatus(PlayerContract.STATUS_ERROR, PlaybackErrors.WIFI_ONLY)
            return false
        }
        return true
    }

    private fun setSleepTimer(minutes: Int) {
        cancelSleepTimer(restoreVolume = true)
        sleepTimer.start(minutes)?.let { delay ->
            handler.postDelayed(sleepRunnable, (delay - SleepTimer.FADE_OUT_MS).coerceAtLeast(0))
        }
        pushExtras()
    }

    private fun cancelSleepTimer(restoreVolume: Boolean) {
        handler.removeCallbacks(sleepRunnable)
        if (fading && restoreVolume) exoPlayer.volume = volumeBeforeFade
        fading = false
        handler.removeCallbacks(fadeRunnable)
        sleepTimer.cancel()
    }

    private fun startFadeOut() {
        if (!exoPlayer.playWhenReady) {
            finishSleep()
            return
        }
        volumeBeforeFade = exoPlayer.volume
        fadeStep = 0
        fading = true
        handler.post(fadeRunnable)
    }

    private fun finishSleep() {
        handler.removeCallbacks(fadeRunnable)
        exoPlayer.pause()
        if (fading) exoPlayer.volume = volumeBeforeFade
        fading = false
        sleepTimer.cancel()
        pushExtras()
    }

    /** Stop button: stop audio, drop the notification, release the service. */
    private fun stopPlayback() {
        handler.removeCallbacks(idleStop)
        cancelSleepTimer(restoreVolume = true)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentStationId = null
        reconnecting = false
        publishStatus(PlayerContract.STATUS_IDLE)
        // With no media items Media3 removes the notification and leaves the foreground state;
        // stopSelf() ends the service as soon as no UI is bound to it.
        stopSelf()
    }

    private fun onReconnectAttempt(attempt: Int, max: Int) {
        reconnecting = true
        val message = PlaybackErrors.message(null, container.network.isOnline())
        publishStatus(PlayerContract.STATUS_RECONNECTING, message, attempt, max)
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId
            if (id != currentStationId) {
                currentStationId = id
                recordedStationId = null
                urlIndex = 0
                endedRestarts = 0
                reconnecting = false
                hasUntriedFallback = (container.stations.find(id)?.fallbackUrls?.size ?: 0) > 0
                publishStatus(PlayerContract.STATUS_IDLE)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> if (reconnecting) {
                    reconnecting = false
                    publishStatus(PlayerContract.STATUS_IDLE)
                }
                // A live stream never really "ends" — the server closed the connection. Reconnect.
                Player.STATE_ENDED -> if (exoPlayer.playWhenReady) {
                    if (endedRestarts++ < 3) {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                    } else {
                        handleFatalError(null)
                    }
                }
                else -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) endedRestarts = 0
            val id = currentStationId
            if (isPlaying && id != null && id != recordedStationId) {
                recordedStationId = id
                scope.launch { container.prefs.addRecent(id) }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            handler.removeCallbacks(idleStop)
            if (!playWhenReady) {
                if (reconnecting) {
                    reconnecting = false
                    publishStatus(PlayerContract.STATUS_IDLE)
                }
                // Don't keep a paused player (and this service) around forever.
                handler.postDelayed(idleStop, IDLE_STOP_MS)
            }
        }

        override fun onPlayerError(error: PlaybackException) = handleFatalError(error)
    }

    /** Called when ExoPlayer has given up on the current address. */
    private fun handleFatalError(error: PlaybackException?) {
        val item = exoPlayer.currentMediaItem ?: return
        val station = container.stations.find(item.mediaId) ?: return

        if (error?.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            exoPlayer.seekToDefaultPosition()
            exoPlayer.prepare()
            return
        }

        // Try the station's alternative stream addresses (playback continues in BUFFERING).
        val urls = station.allStreamUrls
        if (urlIndex + 1 < urls.size) {
            urlIndex++
            hasUntriedFallback = urlIndex + 1 < urls.size
            exoPlayer.replaceMediaItem(
                exoPlayer.currentMediaItemIndex,
                StationMediaItems.withUrl(item, urls[urlIndex]),
            )
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            return
        }

        reconnecting = false
        urlIndex = 0
        hasUntriedFallback = station.fallbackUrls.isNotEmpty()
        // Put the primary URL back so a later Play starts clean.
        exoPlayer.replaceMediaItem(exoPlayer.currentMediaItemIndex, StationMediaItems.withUrl(item, station.streamUrl))
        exoPlayer.playWhenReady = false
        publishStatus(PlayerContract.STATUS_ERROR, PlaybackErrors.message(error, container.network.isOnline()))
    }

    private fun publishStatus(status: String, message: String? = null, attempt: Int = 0, max: Int = 0) {
        lastStatus = status
        lastMessage = message
        lastAttempt = attempt
        lastMaxAttempts = max
        statusSeq++
        pushExtras()
    }

    /** Sends playback status + sleep timer to all controllers (UI). */
    private fun pushExtras() {
        val extras = Bundle().apply {
            putString(PlayerContract.EXTRA_STATUS, lastStatus)
            putString(PlayerContract.EXTRA_MESSAGE, lastMessage)
            putInt(PlayerContract.EXTRA_ATTEMPT, lastAttempt)
            putInt(PlayerContract.EXTRA_MAX_ATTEMPTS, lastMaxAttempts)
            putString(PlayerContract.EXTRA_STATION_ID, currentStationId)
            putInt(PlayerContract.EXTRA_SEQ, statusSeq)
            sleepTimer.endsAt?.let { putLong(PlayerContract.EXTRA_SLEEP_ENDS_AT, it) }
        }
        session?.setSessionExtras(extras)
    }

    // ---- Session / library callback -------------------------------------------------------

    private fun mediaItem(station: Station) = StationMediaItems.toMediaItem(station, defaultArtwork = defaultArtwork)

    private fun stationsIn(parentId: String): List<Station>? {
        val all = container.stations.stations
        return when {
            parentId == NODE_ALL -> all
            parentId == NODE_FAVORITES -> all.filter { it.id in favoriteIds }
            parentId == NODE_RECENT -> recentIds.mapNotNull { container.stations.find(it) }
            parentId.startsWith(NODE_GENRE_PREFIX) -> parentId.removePrefix(NODE_GENRE_PREFIX)
                .let { g -> all.filter { it.genre == g } }
            else -> null
        }
    }

    private fun childrenOf(parentId: String): List<MediaItem>? = when (parentId) {
        NODE_ROOT -> listOf(
            StationMediaItems.folder(NODE_FAVORITES, "Любими"),
            StationMediaItems.folder(NODE_RECENT, "Последно слушани"),
            StationMediaItems.folder(NODE_ALL, "Всички радиостанции"),
            StationMediaItems.folder(NODE_GENRES, "Категории"),
        )
        NODE_GENRES -> container.stations.genres.map {
            StationMediaItems.folder(NODE_GENRE_PREFIX + it, Categories.shortLabel(it))
        }
        else -> stationsIn(parentId)?.map(::mediaItem)
    }

    /** Station for a requested item: by id, or by voice search ("пусни Радио 1"). */
    private fun resolve(item: MediaItem): Station? {
        container.stations.find(item.mediaId)?.let { return it }
        val query = item.requestMetadata.searchQuery ?: return null
        val all = container.stations.stations
        if (query.isBlank()) return recentIds.firstNotNullOfOrNull { container.stations.find(it) } ?: all.firstOrNull()
        return all.firstOrNull { SearchUtils.normalize(it.name) == SearchUtils.normalize(query) }
            ?: all.firstOrNull { SearchUtils.matches(it, query) }
    }

    /** Builds the queue Previous/Next move through when a single station is started. */
    private fun queueFor(station: Station): MediaSession.MediaItemsWithStartPosition {
        val queue = when {
            station.id in favoriteIds -> container.stations.stations.filter { it.id in favoriteIds }
            else -> container.stations.stations
        }
        return MediaSession.MediaItemsWithStartPosition(
            queue.map(::mediaItem),
            queue.indexOfFirst { it.id == station.id }.coerceAtLeast(0),
            C.TIME_UNSET,
        )
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(PlayerContract.STOP_COMMAND)
                .add(PlayerContract.SLEEP_TIMER_COMMAND)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                PlayerContract.ACTION_STOP -> {
                    stopPlayback()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                PlayerContract.ACTION_SLEEP_TIMER -> {
                    setSleepTimer(args.getInt(PlayerContract.EXTRA_SLEEP_MINUTES, 0))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        // Controllers send only station ids; stream URLs are resolved here from stations.json.
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
            mediaItems.mapNotNull { item -> resolve(item)?.let(::mediaItem) }.toMutableList()
        )

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // A car / Bluetooth / Assistant picks one station: give it a queue for Next/Previous.
            if (mediaItems.size == 1) {
                resolve(mediaItems[0])?.let { return Futures.immediateFuture(queueFor(it)) }
            }
            val resolved = mediaItems.mapNotNull { container.stations.find(it.mediaId) }
            val start = mediaItems.getOrNull(startIndex)?.mediaId
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    resolved.map(::mediaItem),
                    resolved.indexOfFirst { it.id == start }.coerceAtLeast(0),
                    C.TIME_UNSET,
                )
            )
        }

        /** "Play" from Bluetooth headphones / car when the app isn't running: resume the last station. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val station = recentIds.firstNotNullOfOrNull { container.stations.find(it) }
                ?: container.stations.stations.firstOrNull { it.popular }
                ?: container.stations.stations.firstOrNull()
                ?: return Futures.immediateFailedFuture(UnsupportedOperationException("No stations"))
            return Futures.immediateFuture(queueFor(station))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(StationMediaItems.folder(NODE_ROOT, "Dominix"), params)
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = childrenOf(parentId)
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            val from = (page.toLong() * pageSize).coerceAtMost(children.size.toLong()).toInt()
            val to = (from.toLong() + pageSize).coerceAtMost(children.size.toLong()).toInt()
            return Futures.immediateFuture(LibraryResult.ofItemList(children.subList(from, to), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = container.stations.find(mediaId)?.let(::mediaItem)
                ?: childrenOf(NODE_ROOT)?.firstOrNull { it.mediaId == mediaId }
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            val count = container.stations.stations.count { SearchUtils.matches(it, query) }
            session.notifySearchResultChanged(browser, query, count, params)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val found = container.stations.stations
                .filter { SearchUtils.matches(it, query) }
                .map(::mediaItem)
            val from = (page.toLong() * pageSize).coerceAtMost(found.size.toLong()).toInt()
            val to = (from.toLong() + pageSize).coerceAtMost(found.size.toLong()).toInt()
            return Futures.immediateFuture(LibraryResult.ofItemList(found.subList(from, to), params))
        }
    }

    private companion object {
        const val IDLE_STOP_MS = 10 * 60 * 1000L

        const val NODE_ROOT = "root"
        const val NODE_FAVORITES = "node_favorites"
        const val NODE_RECENT = "node_recent"
        const val NODE_ALL = "node_all"
        const val NODE_GENRES = "node_genres"
        const val NODE_GENRE_PREFIX = "genre:"
    }
}
