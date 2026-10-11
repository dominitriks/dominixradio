package com.bgradio.app.ui

import com.bgradio.app.data.partners.Partner
import com.bgradio.app.data.partners.PartnersParser
import com.bgradio.app.data.stats.Days
import com.bgradio.app.data.catalog.CatalogUpdateResult
import com.bgradio.app.data.repository.CatalogInfo
import com.bgradio.app.data.repository.StationRepository
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
import com.bgradio.app.data.repository.UpdateResult
import com.bgradio.app.data.repository.DownloadState
import com.bgradio.app.data.model.ReleaseInfo
import kotlinx.coroutines.delay
import com.bgradio.app.data.repository.CheckResult
import com.bgradio.app.share.ReportReason
import com.bgradio.app.share.StationReport
import com.bgradio.app.BuildConfig
import kotlinx.coroutines.Job
import com.bgradio.app.data.stats.StatsData
import com.bgradio.app.data.stats.StatsSummary
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
    private val updates = container.updates
    private val stats = container.stats
    private val checker = container.checker

    // ---- Station actions: share, QR, report, check (one dialog at a time) -----------------

    sealed interface StationDialog {
        val station: Station
        data class Menu(override val station: Station) : StationDialog
        data class Qr(override val station: Station) : StationDialog
        data class Report(override val station: Station) : StationDialog
        data class Check(override val station: Station) : StationDialog
    }

    private val _stationDialog = MutableStateFlow<StationDialog?>(null)
    val stationDialog: StateFlow<StationDialog?> = _stationDialog.asStateFlow()

    private val _checkResult = MutableStateFlow<CheckResult?>(null)
    val checkResult: StateFlow<CheckResult?> = _checkResult.asStateFlow()
    private var checkJob: Job? = null

    /** Ignored while another station dialog is open (prevents stacked duplicates). */
    fun openStationMenu(station: Station) {
        if (_stationDialog.value == null) _stationDialog.value = StationDialog.Menu(station)
    }

    fun showStationDialog(dialog: StationDialog) {
        _stationDialog.value = dialog
        if (dialog is StationDialog.Check) startCheck(dialog.station)
    }

    fun closeStationDialog() {
        checkJob?.cancel()
        checkJob = null
        _checkResult.value = null
        _stationDialog.value = null
    }

    private fun startCheck(station: Station) {
        checkJob?.cancel()
        _checkResult.value = null
        checkJob = viewModelScope.launch {
            val result = checker.check(station)
            _checkResult.value = result
            if (result == CheckResult.Works || result == CheckResult.WorksViaFallback) player.clearIssue(station.id)
        }
    }

    fun buildReport(station: Station, reason: ReportReason, description: String) =
        StationReport(station, reason, description, BuildConfig.VERSION_NAME, System.currentTimeMillis())

    // ---- Listening statistics -------------------------------------------------------------

    val statsSummary: StateFlow<StatsSummary> = stats.data
        .map { StatsSummary.of(it, System.currentTimeMillis()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSummary.of(StatsData(), System.currentTimeMillis()))

    fun clearStats() = stats.clear()

    // ---- Update check (GitHub Releases) ---------------------------------------------------

    sealed interface UpdateUi {
        data object Idle : UpdateUi
        data object Checking : UpdateUi
        data class Result(val result: UpdateResult, val manual: Boolean) : UpdateUi
    }

    private val _update = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val update: StateFlow<UpdateUi> = _update.asStateFlow()

    /** Shown as a dialog: a new version found automatically, or any result of a manual check. */
    private val _updateDialog = MutableStateFlow<UpdateUi.Result?>(null)
    val updateDialog: StateFlow<UpdateUi.Result?> = _updateDialog.asStateFlow()

    val installedVersion: String get() = updates.installedVersion.toString()

    // ---- In-app update download (DownloadManager → verify → system installer) -------------

    sealed interface UpdateDownloadUi {
        data object Idle : UpdateDownloadUi
        data class Downloading(val percent: Int?) : UpdateDownloadUi
        data object Verifying : UpdateDownloadUi
        data class Ready(val file: java.io.File) : UpdateDownloadUi
        data class Failed(val message: String) : UpdateDownloadUi
        /** Android asks the user once to allow Dominix to install updates. */
        data object NeedsPermission : UpdateDownloadUi
    }

    private val downloader = container.downloader
    private val _download = MutableStateFlow<UpdateDownloadUi>(UpdateDownloadUi.Idle)
    val download: StateFlow<UpdateDownloadUi> = _download.asStateFlow()
    private var downloadJob: Job? = null
    private var downloadId: Long? = null

    fun downloadUpdate(release: ReleaseInfo) {
        if (_download.value is UpdateDownloadUi.Downloading || _download.value == UpdateDownloadUi.Verifying) return
        if (release.version <= updates.installedVersion) {
            _download.value = UpdateDownloadUi.Failed("Тази версия не е по-нова от инсталираната.")
            return
        }
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val target = downloader.fileFor(release)
            val part = downloader.partFor(release)
            // Already downloaded earlier and still valid → straight to install.
            if (target != null && target.exists() && downloader.verify(target, release) == null) {
                readyFile = target
                _download.value = UpdateDownloadUi.Ready(target)
                return@launch
            }
            val id = downloader.start(release)
            if (id == null || target == null || part == null) {
                _download.value = UpdateDownloadUi.Failed("Свалянето не можа да започне. Опитайте „Отвори в браузъра“.")
                return@launch
            }
            downloadId = id
            _download.value = UpdateDownloadUi.Downloading(null)
            while (true) {
                when (val st = withContext(Dispatchers.IO) { downloader.state(id, part) }) {
                    is DownloadState.Running -> _download.value = UpdateDownloadUi.Downloading(st.percent)
                    is DownloadState.Failed -> {
                        _download.value = UpdateDownloadUi.Failed(st.message)
                        break
                    }
                    is DownloadState.Done -> {
                        _download.value = UpdateDownloadUi.Verifying
                        val (file, problem) = downloader.verifyAndFinalize(st.file, release)
                        if (file != null) readyFile = file
                        _download.value = if (file != null) UpdateDownloadUi.Ready(file)
                        else UpdateDownloadUi.Failed(problem ?: "Проверката на файла не успя. Опитайте отново.")
                        break
                    }
                }
                delay(500)
            }
            downloadId = null
        }
    }

    /** The verified file waiting for installation (kept while the user grants permission). */
    private var readyFile: java.io.File? = null

    /** Opens Android's installer (the user confirms there). */
    fun installUpdate() {
        val file = readyFile?.takeIf { it.exists() } ?: return
        if (!downloader.canInstall()) {
            _download.value = UpdateDownloadUi.NeedsPermission
            return
        }
        _download.value = UpdateDownloadUi.Ready(file)
        if (!downloader.install(file)) {
            _download.value = UpdateDownloadUi.Failed("Инсталаторът на Android не можа да се отвори.")
        }
    }

    fun openInstallPermissionSettings() = downloader.openInstallPermissionSettings()

    fun cancelUpdateDownload() {
        downloadJob?.cancel()
        downloadId?.let(downloader::cancel)
        downloadId = null
        _download.value = UpdateDownloadUi.Idle
    }

    /**
     * Automatic check on every app start (manual = false): silent unless a newer version is
     * confirmed. Manual check from Settings: always shows the result. Never blocks the UI or
     * playback; a second check while one runs is ignored.
     */
    fun checkForUpdates(manual: Boolean) {
        if (_update.value == UpdateUi.Checking) return
        viewModelScope.launch {
            _update.value = UpdateUi.Checking
            val result = updates.check()
            if (result == null) return@launch // another check is already running
            val ui = UpdateUi.Result(result, manual)
            _update.value = ui
            if (manual || result is UpdateResult.Available) _updateDialog.value = ui
        }
    }

    /** "По-късно" — nothing is remembered; the next app start checks again. */
    fun dismissUpdateDialog(@Suppress("UNUSED_PARAMETER") rememberVersion: Boolean = false) {
        _updateDialog.value = null
    }

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
        viewModelScope.launch(Dispatchers.IO) { stats.ensureLoaded() }
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { stationRepo.stations }
            // Show the last listened station on start (paused), ready to continue with one tap.
            val lastId = prefs.recent.first().firstOrNull()
            stationRepo.find(lastId)?.let { player.restoreLastStation(it, all) }
            checkForUpdates(manual = false)
            refreshCatalog(manual = false)
            withContext(Dispatchers.IO) { container.partners.ensureLoaded() }
            container.partners.refresh(force = false)
        }
        // The catalog can change at runtime when a newer stations.json is downloaded.
        viewModelScope.launch {
            stationRepo.catalog.collect { list ->
                _stations.value = list
                _genres.value = StationRepository.genresOf(list)
            }
        }
    }

    // ---- Station catalog updates (stations.json from GitHub) -----------------------------

    val catalogInfo: StateFlow<CatalogInfo> = stationRepo.info

    /** Active (not expired) partners, premium first. */
    val partners: StateFlow<List<Partner>> = container.partners.all
        .map { PartnersParser.active(it, Days.key(System.currentTimeMillis())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed interface CatalogUi {
        data object Idle : CatalogUi
        data object Checking : CatalogUi
        data class Done(val result: CatalogUpdateResult) : CatalogUi
    }

    private val _catalogUi = MutableStateFlow<CatalogUi>(CatalogUi.Idle)
    val catalogUi: StateFlow<CatalogUi> = _catalogUi.asStateFlow()

    /** Automatic: at most once a day, silently. Manual (Settings): always asks GitHub. */
    fun refreshCatalog(manual: Boolean) {
        if (_catalogUi.value == CatalogUi.Checking) return
        viewModelScope.launch {
            if (manual) _catalogUi.value = CatalogUi.Checking
            // A manual check also refreshes the partner list (same source on GitHub).
            if (manual) launch { container.partners.refresh(force = true) }
            val result = container.catalogUpdater.refresh(force = manual) ?: return@launch
            if (manual || result is CatalogUpdateResult.Updated) _catalogUi.value = CatalogUi.Done(result)
            else if (_catalogUi.value == CatalogUi.Checking) _catalogUi.value = CatalogUi.Idle
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
    fun retry() = player.retry()
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
