package com.bgradio.app.data.repository

import android.content.Context
import android.util.Log
import com.bgradio.app.BuildConfig
import com.bgradio.app.data.catalog.CatalogCache
import com.bgradio.app.data.catalog.CatalogParser
import com.bgradio.app.data.model.Categories
import com.bgradio.app.data.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the stations currently in use come from. */
data class CatalogInfo(
    val downloaded: Boolean,
    val count: Int,
    /** When the downloaded catalog was fetched (ms), null for the built-in one. */
    val updatedAt: Long?,
)

/**
 * The station catalog. Starts from the newest valid copy downloaded from GitHub (if any, for this
 * app version), otherwise from assets/stations.json built into the APK. A newer download replaces
 * it at runtime via [applyDownloaded]; ids stay the same, so favourites and statistics are kept.
 */
class StationRepository(
    private val context: Context,
    private val cache: CatalogCache,
    private val versionCode: Int = BuildConfig.VERSION_CODE,
) {
    private val lock = Any()
    @Volatile private var loaded = false
    @Volatile private var byId: Map<String, Station> = emptyMap()

    private val _catalog = MutableStateFlow<List<Station>>(emptyList())
    /** Current stations; changes when a newer catalog is downloaded. */
    val catalog: StateFlow<List<Station>> = _catalog.asStateFlow()

    private val _info = MutableStateFlow(CatalogInfo(downloaded = false, count = 0, updatedAt = null))
    val info: StateFlow<CatalogInfo> = _info.asStateFlow()

    /** Built-in catalog — the safe fallback, also the reference for validating downloads. */
    private val bundled: List<Station> by lazy { loadBundled() }
    val bundledIds: Set<String> by lazy { bundled.map { it.id }.toSet() }

    val stations: List<Station>
        get() {
            ensureLoaded()
            return _catalog.value
        }

    fun find(id: String?): Station? {
        ensureLoaded()
        return id?.let { byId[it] }
    }

    val genres: List<String> get() = genresOf(stations)

    fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            val cached = try {
                cache.readValid(bundledIds, versionCode)
            } catch (e: Exception) {
                Log.w(TAG, "Downloaded catalog unreadable, using built-in", e)
                null
            }
            if (cached != null) {
                set(cached.first, CatalogInfo(true, cached.first.size, cached.second.updatedAt))
            } else {
                set(bundled, CatalogInfo(false, bundled.size, null))
            }
            loaded = true
        }
    }

    /** Called by the updater with an already validated catalog. */
    fun applyDownloaded(stations: List<Station>, updatedAt: Long) {
        synchronized(lock) {
            set(stations, CatalogInfo(true, stations.size, updatedAt))
            loaded = true
        }
    }

    private fun set(list: List<Station>, info: CatalogInfo) {
        byId = list.associateBy { it.id }
        _catalog.value = list
        _info.value = info
    }

    private fun loadBundled(): List<Station> = try {
        val json = context.assets.open(FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        CatalogParser.parse(json)
    } catch (e: Exception) {
        Log.e(TAG, "Cannot read $FILE", e)
        emptyList()
    }

    companion object {
        private const val TAG = "StationRepository"
        private const val FILE = "stations.json"

        fun genresOf(stations: List<Station>): List<String> {
            val present = stations.map { it.genre }.toSet()
            return Categories.ORDER.filter { it in present } + (present - Categories.ORDER.toSet()).sorted()
        }
    }
}
