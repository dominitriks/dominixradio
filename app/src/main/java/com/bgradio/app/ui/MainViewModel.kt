package com.bgradio.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bgradio.app.AppContainer
import com.bgradio.app.BGRadioApp
import com.bgradio.app.data.model.AppSettings
import com.bgradio.app.data.model.Station
import com.bgradio.app.data.model.ThemeMode
import com.bgradio.app.player.PlayerUiState
import com.bgradio.app.utils.SearchUtils
import com.bgradio.app.utils.StationSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val stationRepo = container.stations
    private val prefs = container.prefs
    private val player = container.player

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _genres = MutableStateFlow<List<String>>(emptyList())
    val genres: StateFlow<List<String>> = _genres.asStateFlow()

    val playerState: StateFlow<PlayerUiState> = player.state

    val settings: StateFlow<AppSettings?> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favoriteIds: StateFlow<Set<String>> = prefs.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favorites: StateFlow<List<Station>> = combine(_stations, favoriteIds) { all, ids ->
        all.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Up to 10 recently played stations, newest first. */
    val recentAll: StateFlow<List<Station>> = combine(_stations, prefs.recent) { all, ids ->
        val byId = all.associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The 5 shown on the home screen. */
    val recent: StateFlow<List<Station>> = recentAll.map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val popular: StateFlow<List<Station>> = _stations.map { all -> all.filter { it.popular } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Catalog search & filter ----------------------------------------------------------

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _genre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _genre.asStateFlow()

    private val _sort = MutableStateFlow(StationSort.POPULAR)
    val sort: StateFlow<StationSort> = _sort.asStateFlow()

    val filtered: StateFlow<List<Station>> = combine(_stations, _query, _genre, _sort) { all, q, g, sort ->
        SearchUtils.sort(all.filter { (g == null || it.genre == g) && SearchUtils.matches(it, q) }, sort)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Stations per genre for the current search text (shown on the filter chips). */
    val genreCounts: StateFlow<Map<String, Int>> = combine(_stations, _query) { all, q ->
        all.filter { SearchUtils.matches(it, q) }.groupingBy { it.genre }.eachCount()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { stationRepo.stations }
            _stations.value = all
            _genres.value = stationRepo.genres
            // Show the last listened station on start (paused), ready to continue with one tap.
            val lastId = prefs.recent.first().firstOrNull()
            stationRepo.find(lastId)?.let { player.restoreLastStation(it, all) }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun selectGenre(genre: String?) {
        _genre.value = genre
    }

    fun setSort(sort: StationSort) {
        _sort.value = sort
    }

    fun clearFilters() {
        _query.value = ""
        _genre.value = null
    }

    // ---- Player ---------------------------------------------------------------------------

    fun togglePlay(station: Station, queue: List<Station>) = player.toggle(station, queue)
    fun playFromSearch(query: String) {
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { stationRepo.stations }
            val station = if (query.isBlank()) {
                recent.value.firstOrNull() ?: all.firstOrNull { it.popular }
            } else {
                all.firstOrNull { SearchUtils.normalize(it.name) == SearchUtils.normalize(query) }
                    ?: all.firstOrNull { SearchUtils.matches(it, query) }
            } ?: return@launch
            player.play(station, all)
        }
    }

    fun resume() = player.resume()
    fun pause() = player.pause()
    fun stop() = player.stop()
    fun next() = player.next()
    fun previous() = player.previous()
    fun setVolume(v: Float) = player.setVolume(v)
    fun clearError() = player.clearError()
    fun setSleepTimer(minutes: Int) = player.setSleepTimer(minutes)

    // ---- Favorites & settings -------------------------------------------------------------

    fun toggleFavorite(id: String) {
        viewModelScope.launch { prefs.toggleFavorite(id) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefs.setWifiOnly(enabled) }
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoReconnect(enabled) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MainViewModel((this[APPLICATION_KEY] as BGRadioApp).container) }
        }
    }
}
