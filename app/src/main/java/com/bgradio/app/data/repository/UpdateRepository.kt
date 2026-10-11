package com.bgradio.app.data.repository

import com.bgradio.app.BuildConfig
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.AppVersion
import com.bgradio.app.data.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

sealed interface UpdateResult {
    data class Available(val release: ReleaseInfo) : UpdateResult
    data class UpToDate(val latest: AppVersion) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/** One HTTP answer from the GitHub API. */
data class HttpAnswer(val code: Int, val body: String)

/** Network access for the update check — replaceable in tests. */
fun interface ReleaseFetcher {
    /** @throws IOException on connection problems (SocketTimeoutException on timeout). */
    fun fetchLatest(): HttpAnswer
}

/** Real fetcher: GitHub Releases API with short timeouts so a slow GitHub never blocks the app. */
class HttpReleaseFetcher(private val url: String = AppInfo.LATEST_RELEASE_API) : ReleaseFetcher {
    override fun fetchLatest(): HttpAnswer {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Dominix/${BuildConfig.VERSION_NAME} (Android)")
            val code = connection.responseCode
            val body = if (code == 200) connection.inputStream.bufferedReader().use { it.readText() } else ""
            return HttpAnswer(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}

/**
 * Asks the public GitHub Releases API which stable version is the newest. Only reads.
 * At most one check runs at a time; a second call while one is running returns null.
 */
class UpdateRepository(
    private val isOnline: () -> Boolean,
    private val fetcher: ReleaseFetcher = HttpReleaseFetcher(),
    val installedVersion: AppVersion = AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion(listOf(0)),
) {
    private val running = AtomicBoolean(false)

    /** @return the result, or null if another check is already in progress. */
    suspend fun check(): UpdateResult? {
        if (!running.compareAndSet(false, true)) return null
        try {
            return withContext(Dispatchers.IO) { checkNow() }
        } finally {
            running.set(false)
        }
    }

    private fun checkNow(): UpdateResult {
        if (!isOnline()) return UpdateResult.Failed(NO_INTERNET)
        val answer = try {
            fetcher.fetchLatest()
        } catch (_: SocketTimeoutException) {
            return UpdateResult.Failed("GitHub не отговори навреме. Опитайте по-късно.")
        } catch (_: IOException) {
            return UpdateResult.Failed(if (isOnline()) FAILED else NO_INTERNET)
        } catch (_: Exception) {
            return UpdateResult.Failed(FAILED)
        }
        return when (answer.code) {
            200 -> evaluate(ReleaseInfo.parse(answer.body), installedVersion)
            404 -> UpdateResult.Failed("Все още няма публикувани версии.")
            403, 429 -> UpdateResult.Failed("GitHub временно ограничи проверките. Опитайте след около час.")
            else -> UpdateResult.Failed("$FAILED (HTTP ${answer.code})")
        }
    }

    companion object {
        private const val FAILED = "Неуспешна проверка за нова версия."
        private const val NO_INTERNET = "Няма интернет връзка. Опитайте по-късно."

        /** Only a confirmed newer stable version is reported as available. */
        fun evaluate(release: ReleaseInfo?, installed: AppVersion): UpdateResult = when {
            release == null -> UpdateResult.Failed("Получен е невалиден отговор от GitHub.")
            release.version > installed -> UpdateResult.Available(release)
            else -> UpdateResult.UpToDate(release.version)
        }
    }
}
