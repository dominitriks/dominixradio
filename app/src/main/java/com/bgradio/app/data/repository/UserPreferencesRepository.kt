package com.bgradio.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bgradio.app.data.model.AppSettings
import com.bgradio.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bg_radio_prefs")

/** Favorites, recently played and settings — stored locally on the phone only. */
class UserPreferencesRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val favorites: Flow<Set<String>> = data.map { it[FAVORITES] ?: emptySet() }.distinctUntilChanged()

    val recent: Flow<List<String>> = data.map { prefs ->
        prefs[RECENT]?.split(SEP)?.filter { it.isNotBlank() }.orEmpty()
    }.distinctUntilChanged()

    val settings: Flow<AppSettings> = data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            wifiOnly = prefs[WIFI_ONLY] ?: false,
            autoReconnect = prefs[AUTO_RECONNECT] ?: true,
        )
    }.distinctUntilChanged()

    suspend fun toggleFavorite(id: String) {
        store.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (id in current) current - id else current + id
        }
    }

    suspend fun addRecent(id: String) {
        store.edit { prefs ->
            val list = prefs[RECENT]?.split(SEP)?.filter { it.isNotBlank() }.orEmpty()
            prefs[RECENT] = (listOf(id) + list.filter { it != id }).take(MAX_RECENT).joinToString(SEP)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        store.edit { it[WIFI_ONLY] = enabled }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        store.edit { it[AUTO_RECONNECT] = enabled }
    }

    private companion object {
        const val SEP = ","
        const val MAX_RECENT = 10
        val FAVORITES = stringSetPreferencesKey("favorites")
        val RECENT = stringPreferencesKey("recent")
        val THEME = stringPreferencesKey("theme_mode")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    }
}
