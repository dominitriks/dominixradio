package com.bgradio.app

import com.bgradio.app.data.partners.PartnersRepository
import com.bgradio.app.data.catalog.CatalogCache
import com.bgradio.app.data.catalog.CatalogUpdater
import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bgradio.app.data.repository.StationRepository
import com.bgradio.app.data.repository.StreamChecker
import com.bgradio.app.data.repository.UpdateDownloader
import com.bgradio.app.data.stats.StatsRepository
import java.io.File
import com.bgradio.app.data.repository.UpdateRepository
import com.bgradio.app.data.repository.UserPreferencesRepository
import com.bgradio.app.player.PlayerConnection
import com.bgradio.app.utils.NetworkMonitor

/** Simple manual dependency container — one instance per app process. */
class AppContainer(context: Context) {
    val network = NetworkMonitor(context)
    private val catalogCache = CatalogCache(File(context.filesDir, "catalog"))
    val stations = StationRepository(context, catalogCache)
    val catalogUpdater = CatalogUpdater(
        cache = catalogCache,
        apply = stations::applyDownloaded,
        bundledIds = { stations.bundledIds },
        isOnline = network::isOnline,
    )
    val partners = PartnersRepository(
        cache = CatalogCache(File(context.filesDir, "partners"), fileName = "partners.json"),
        bundled = { context.assets.open("partners.json").bufferedReader(Charsets.UTF_8).use { it.readText() } },
        isOnline = network::isOnline,
    )
    val prefs = UserPreferencesRepository(context)
    val player = PlayerConnection(context, stations)
    val updates = UpdateRepository(isOnline = network::isOnline)
    val checker = StreamChecker(network)
    val downloader = UpdateDownloader(context)
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
