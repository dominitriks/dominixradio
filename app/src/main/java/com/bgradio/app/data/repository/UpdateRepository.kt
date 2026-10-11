package com.bgradio.app.data.repository

import com.bgradio.app.BuildConfig
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.AppVersion
import com.bgradio.app.data.model.ReleaseInfo
import com.bgradio.app.utils.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateResult {
    data class Available(val release: ReleaseInfo) : UpdateResult
    data class UpToDate(val latest: AppVersion) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/**
 * Asks the public GitHub Releases API which version is the newest. Only reads — it never
 * downloads or installs anything; the user opens the APK link themselves.
 */
class UpdateRepository(private val network: NetworkMonitor) {

    val installedVersion: AppVersion = AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion(listOf(0))

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        if (!network.isOnline()) return@withContext UpdateResult.Failed("Няма интернет връзка. Опитайте по-късно.")
        val connection = try {
            URL(AppInfo.LATEST_RELEASE_API).openConnection() as HttpURLConnection
        } catch (_: IOException) {
            return@withContext UpdateResult.Failed(FAILED)
        }
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Dominix/${BuildConfig.VERSION_NAME} (Android)")
            when (val code = connection.responseCode) {
                200 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    evaluate(ReleaseInfo.parse(body), installedVersion)
                }
                404 -> UpdateResult.Failed("Все още няма публикувани версии.")
                403, 429 -> UpdateResult.Failed("GitHub временно ограничи проверките. Опитайте след около час.")
                else -> UpdateResult.Failed("$FAILED (HTTP $code)")
            }
        } catch (_: IOException) {
            UpdateResult.Failed(FAILED)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val FAILED = "Неуспешна проверка за нова версия."

        fun evaluate(release: ReleaseInfo?, installed: AppVersion): UpdateResult = when {
            release == null -> UpdateResult.Failed("Получен е невалиден отговор от GitHub.")
            release.version > installed -> UpdateResult.Available(release)
            else -> UpdateResult.UpToDate(release.version)
        }
    }
}
