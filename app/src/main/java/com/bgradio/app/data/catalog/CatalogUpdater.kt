package com.bgradio.app.data.catalog

import com.bgradio.app.BuildConfig
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** Bookkeeping for the downloaded catalog. */
data class CatalogMeta(
    val etag: String?,
    /** Last time GitHub was asked (ms). */
    val checkedAt: Long,
    /** When the cached catalog was downloaded (ms). */
    val updatedAt: Long,
    /** App version that downloaded it — a new APK ignores older caches. */
    val versionCode: Int,
)

/** Downloaded catalog stored in [dir]; writes are atomic (temp file + rename). */
class CatalogCache(private val dir: File, private val fileName: String = "stations.json") {
    private val catalogFile get() = File(dir, fileName)

    /** Raw cached file, or null (used for files other than the station list, e.g. partners). */
    fun readText(): String? = try {
        if (catalogFile.exists()) catalogFile.readText() else null
    } catch (_: IOException) {
        null
    }
    private val metaFile get() = File(dir, "catalog_meta.json")

    fun meta(): CatalogMeta? = try {
        if (!metaFile.exists()) null else JSONObject(metaFile.readText()).let { o ->
            CatalogMeta(
                etag = o.optString("etag").ifBlank { null },
                checkedAt = o.optLong("checkedAt"),
                updatedAt = o.optLong("updatedAt"),
                versionCode = o.optInt("versionCode"),
            )
        }
    } catch (_: Exception) {
        null
    }

    /** The cached catalog if it belongs to this app version and still passes validation. */
    fun readValid(bundledIds: Set<String>, versionCode: Int): Pair<List<Station>, CatalogMeta>? {
        val meta = meta() ?: return null
        if (meta.versionCode != versionCode || meta.updatedAt <= 0 || !catalogFile.exists()) return null
        val json = try { catalogFile.readText() } catch (_: IOException) { return null }
        return when (val check = CatalogParser.validateRemote(json, bundledIds)) {
            is CatalogCheck.Accepted -> check.stations to meta
            is CatalogCheck.Rejected -> null
        }
    }

    fun write(json: String, meta: CatalogMeta) {
        dir.mkdirs()
        writeAtomic(catalogFile, json)
        writeMeta(meta)
    }

    fun writeMeta(meta: CatalogMeta) {
        dir.mkdirs()
        writeAtomic(
            metaFile,
            JSONObject()
                .put("etag", meta.etag ?: "")
                .put("checkedAt", meta.checkedAt)
                .put("updatedAt", meta.updatedAt)
                .put("versionCode", meta.versionCode)
                .toString(),
        )
    }

    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) throw IOException("Cannot write ${target.name}")
        }
    }
}

/** One HTTP answer for the catalog. */
data class CatalogAnswer(val code: Int, val body: String, val etag: String?)

fun interface CatalogFetcher {
    /** @throws IOException on connection problems. */
    fun fetch(etag: String?): CatalogAnswer
}

/** Downloads stations.json from the official GitHub repository (conditional request with ETag). */
class HttpCatalogFetcher(private val url: String = AppInfo.CATALOG_URL) : CatalogFetcher {
    override fun fetch(etag: String?): CatalogAnswer {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Dominix/${BuildConfig.VERSION_NAME} (Android)")
            if (etag != null) c.setRequestProperty("If-None-Match", etag)
            val code = c.responseCode
            val body = if (code == 200) {
                c.inputStream.use { input ->
                    val bytes = input.readNBytesCompat(CatalogParser.MAX_BYTES + 1)
                    String(bytes, Charsets.UTF_8)
                }
            } else {
                ""
            }
            return CatalogAnswer(code, body, c.getHeaderField("ETag"))
        } finally {
            c.disconnect()
        }
    }

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (out.size() < max) {
            val n = read(buf, 0, minOf(buf.size, max - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}

sealed interface CatalogUpdateResult {
    data class Updated(val count: Int) : CatalogUpdateResult
    data object NotModified : CatalogUpdateResult
    /** Checked recently — nothing done. */
    data object NotDue : CatalogUpdateResult
    data class Failed(val message: String) : CatalogUpdateResult
}

/**
 * Keeps the station catalog fresh: at most once a day (or on request) it asks GitHub whether
 * stations.json changed; a valid new catalog is stored and applied at once. Any problem keeps the
 * current catalog — the app always has the built-in one to fall back to.
 */
class CatalogUpdater(
    private val cache: CatalogCache,
    private val apply: (List<Station>, Long) -> Unit,
    private val bundledIds: () -> Set<String>,
    private val isOnline: () -> Boolean,
    private val fetcher: CatalogFetcher = HttpCatalogFetcher(),
    private val versionCode: Int = BuildConfig.VERSION_CODE,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val running = AtomicBoolean(false)

    /** @return the result, or null if a refresh is already running. */
    suspend fun refresh(force: Boolean): CatalogUpdateResult? {
        if (!running.compareAndSet(false, true)) return null
        try {
            return withContext(Dispatchers.IO) { refreshNow(force) }
        } finally {
            running.set(false)
        }
    }

    private fun refreshNow(force: Boolean): CatalogUpdateResult {
        val meta = cache.meta()?.takeIf { it.versionCode == versionCode }
        if (!force && meta != null && now() - meta.checkedAt in 0 until INTERVAL_MS) return CatalogUpdateResult.NotDue
        if (!isOnline()) return CatalogUpdateResult.Failed("Няма интернет връзка.")

        // Conditional request only if we really have that version cached.
        val etag = meta?.etag?.takeIf { meta.updatedAt > 0 }
        val answer = try {
            fetcher.fetch(etag)
        } catch (_: SocketTimeoutException) {
            return CatalogUpdateResult.Failed("GitHub не отговори навреме.")
        } catch (_: Exception) {
            return CatalogUpdateResult.Failed("Неуспешна връзка с GitHub.")
        }
        val time = now()
        return when (answer.code) {
            304 -> {
                meta?.let { cache.writeMeta(it.copy(checkedAt = time)) }
                CatalogUpdateResult.NotModified
            }
            200 -> when (val check = CatalogParser.validateRemote(answer.body, bundledIds())) {
                is CatalogCheck.Rejected -> {
                    // Remember the check so a broken file isn't downloaded again every start.
                    cache.writeMeta((meta ?: CatalogMeta(null, 0, 0, versionCode)).copy(checkedAt = time))
                    CatalogUpdateResult.Failed(check.reason)
                }
                is CatalogCheck.Accepted -> {
                    try {
                        cache.write(answer.body, CatalogMeta(answer.etag, time, time, versionCode))
                    } catch (_: IOException) {
                        return CatalogUpdateResult.Failed("Каталогът не можа да бъде записан.")
                    }
                    apply(check.stations, time)
                    CatalogUpdateResult.Updated(check.stations.size)
                }
            }
            else -> CatalogUpdateResult.Failed("GitHub върна грешка (HTTP ${answer.code}).")
        }
    }

    companion object {
        const val INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
