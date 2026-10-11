package com.bgradio.app.data.repository

import com.bgradio.app.BuildConfig
import com.bgradio.app.data.model.Station
import com.bgradio.app.utils.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

/** Outcome of one attempt against one address. */
sealed interface ProbeResult {
    data object Audio : ProbeResult
    data class Failed(val reason: String) : ProbeResult
}

/** What the user is told after "Провери радиостанцията". */
sealed interface CheckResult {
    data object NoInternet : CheckResult
    data object Works : CheckResult
    /** Main address failed, an alternative one works (the player uses it automatically). */
    data object WorksViaFallback : CheckResult
    data class NotResponding(val attempts: Int, val lastReason: String) : CheckResult

    val message: String
        get() = when (this) {
            NoInternet -> "Няма интернет връзка — проверката не е възможна. Проблемът не е в станцията."
            Works -> "Станцията отговаря и изпраща аудио."
            WorksViaFallback -> "Основният адрес не отговори, но резервният работи — Dominix ще го използва автоматично."
            is NotResponding -> "Станцията не отговори при $attempts опита ($lastReason). " +
                "Това може да е временно — опитайте по-късно или докладвайте проблема."
        }

    companion object {
        /**
         * One failure proves nothing: every address gets [ATTEMPTS] tries before a station is
         * reported as not responding.
         */
        fun classify(online: Boolean, perUrl: List<List<ProbeResult>>): CheckResult {
            if (!online) return NoInternet
            if (perUrl.firstOrNull()?.any { it == ProbeResult.Audio } == true) return Works
            if (perUrl.drop(1).any { tries -> tries.any { it == ProbeResult.Audio } }) return WorksViaFallback
            val fails = perUrl.flatten().filterIsInstance<ProbeResult.Failed>()
            return NotResponding(fails.size, fails.lastOrNull()?.reason ?: "няма отговор")
        }

        const val ATTEMPTS = 2
    }
}

/**
 * On-demand check of a station's stream, started only by the user. It opens its own short
 * connection (max ~32 KB) and never touches the player, so current playback is not interrupted.
 */
class StreamChecker(private val network: NetworkMonitor) {

    suspend fun check(station: Station): CheckResult = withContext(Dispatchers.IO) {
        if (!network.isOnline()) return@withContext CheckResult.NoInternet
        val results = ArrayList<List<ProbeResult>>()
        for (url in station.allStreamUrls) {
            val tries = ArrayList<ProbeResult>()
            repeat(CheckResult.ATTEMPTS) { attempt ->
                coroutineContext.ensureActive()
                if (attempt > 0) delay(1_500)
                val r = probe(url)
                tries += r
                if (r == ProbeResult.Audio) {
                    results += tries
                    return@withContext CheckResult.classify(network.isOnline(), results)
                }
            }
            results += tries
        }
        // Connection may have dropped during the check — don't blame the station then.
        CheckResult.classify(network.isOnline(), results)
    }

    private fun probe(url: String): ProbeResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Dominix/${BuildConfig.VERSION_NAME} (Android)")
                setRequestProperty("Icy-MetaData", "0")
            }
            val code = connection.responseCode
            if (code !in 200..299) return ProbeResult.Failed("HTTP $code")
            val type = connection.contentType.orEmpty().lowercase()
            if (type.startsWith("text/html")) return ProbeResult.Failed("не е аудио поток")
            val buf = ByteArray(32 * 1024)
            var total = 0
            connection.inputStream.use { input ->
                while (total < buf.size) {
                    val n = input.read(buf, total, buf.size - total)
                    if (n < 0) break
                    total += n
                    if (total >= 7 && String(buf, 0, 7, Charsets.US_ASCII) == "#EXTM3U") {
                        return ProbeResult.Audio // HLS playlist answered
                    }
                }
            }
            if (total >= 8 * 1024) ProbeResult.Audio else ProbeResult.Failed("твърде малко данни")
        } catch (_: SocketTimeoutException) {
            ProbeResult.Failed("изтече времето за отговор")
        } catch (_: UnknownHostException) {
            ProbeResult.Failed("сървърът не е намерен")
        } catch (e: Exception) {
            ProbeResult.Failed("грешка при връзката")
        } finally {
            connection?.disconnect()
        }
    }
}
