package com.bgradio.app

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bgradio.app.data.repository.StationRepository
import com.bgradio.app.data.repository.StreamChecker
import com.bgradio.app.data.stats.StatsRepository
import java.io.File
import com.bgradio.app.data.repository.UpdateRepository
import com.bgradio.app.data.repository.UserPreferencesRepository
import com.bgradio.app.player.PlayerConnection
import com.bgradio.app.utils.NetworkMonitor

/** Simple manual dependency container — one instance per app process. */
class AppContainer(context: Context) {
    val stations = StationRepository(context)
    val prefs = UserPreferencesRepository(context)
    val network = NetworkMonitor(context)
    val player = PlayerConnection(context, stations)
    val updates = UpdateRepository(network)
    val checker = StreamChecker(network)
    val stats = StatsRepository(File(context.filesDir, "listening_stats.json"))
}

class BGRadioApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Logos are small and cached on disk, so they load once and work offline afterwards. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("logo_cache"))
                .maxSizeBytes(50L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
