package com.bgradio.app.data.partners

import com.bgradio.app.BuildConfig
import com.bgradio.app.data.catalog.CatalogCache
import com.bgradio.app.data.catalog.CatalogFetcher
import com.bgradio.app.data.catalog.CatalogMeta
import com.bgradio.app.data.catalog.HttpCatalogFetcher
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.stats.Days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/** Basic = name + link. Premium (monthly or yearly) = logo + name + link, shown more prominently. */
enum class PartnerTier { BASIC, PREMIUM }

data class Partner(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String?,
    val tier: PartnerTier,
    /** Last day shown, "yyyy-MM-dd" (inclusive); null = no end date. */
    val until: String?,
) {
    /** Host shown under the name so people see where the link goes. */
    val domain: String get() = runCatching { URI(url).host.removePrefix("www.") }.getOrDefault(url)
}

/**
 * partners.json — a JSON array maintained in the GitHub repository:
 * `{"id":"shop","name":"Магазин","url":"https://…","logoUrl":"https://…","tier":"premium","until":"2027-10-31"}`
 * Pure parsing / validation — unit tested.
 */
object PartnersParser {

    const val MAX_BYTES = 128 * 1024
    const val MAX_PARTNERS = 50

    private val idPattern = Regex("^[a-z0-9_]{1,40}$")
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /** All well-formed entries, or null if the file itself is unusable. */
    fun parse(json: String): List<Partner>? {
        if (json.length > MAX_BYTES) return null
        val array = try { JSONArray(json) } catch (_: Exception) { return null }
        val out = ArrayList<Partner>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            val name = o.optString("name").trim()
            val url = o.optString("url").trim()
            if (!idPattern.matches(id) || name.isEmpty() || name.length > 60 || !isSafeLink(url)) continue
            val tier = if (o.optString("tier").equals("premium", ignoreCase = true)) PartnerTier.PREMIUM else PartnerTier.BASIC
            val logo = o.optString("logoUrl").trim().takeIf { isSafeLink(it) }
            val until = o.optString("until").trim().takeIf { datePattern.matches(it) }
            out += Partner(id, name, url, logo, tier, until)
        }
        return out.distinctBy { it.id }.take(MAX_PARTNERS)
    }

    /** Partners to show today: not expired; premium first, then by name. */
    fun active(all: List<Partner>, today: String): List<Partner> =
        all.filter { it.until == null || it.until >= today }
            .sortedWith(compareBy<Partner> { it.tier != PartnerTier.PREMIUM }.thenBy { it.name.lowercase() })

    /** Only https web links (no app schemes, no plain http). */
    fun isSafeLink(url: String): Boolean {
        if (!url.startsWith("https://") || url.length > 300 || url.any { it.isWhitespace() }) return false
        return runCatching { !URI(url).host.isNullOrBlank() }.getOrDefault(false)
    }
}

sealed interface PartnersUpdateResult {
    data class Updated(val count: Int) : PartnersUpdateResult
    data object NotModified : PartnersUpdateResult
    data object NotDue : PartnersUpdateResult
    data class Failed(val message: String) : PartnersUpdateResult
}

/**
 * Partners list: starts from the last downloaded copy (or the empty one built into the APK) and
 * is refreshed from GitHub at most once a day, exactly like the station catalog.
 */
class PartnersRepository(
    private val cache: CatalogCache,
    private val bundled: () -> String,
    private val isOnline: () -> Boolean,
    private val fetcher: CatalogFetcher = HttpCatalogFetcher(AppInfo.PARTNERS_URL),
    private val now: () -> Long = System::currentTimeMillis,
    private val versionCode: Int = BuildConfig.VERSION_CODE,
) {
    private val _all = MutableStateFlow<List<Partner>>(emptyList())
    /** Every valid partner (including expired ones — filter with [active]). */
    val all: StateFlow<List<Partner>> = _all.asStateFlow()

    private val running = AtomicBoolean(false)
    @Volatile private var loaded = false

    fun active(): List<Partner> = PartnersParser.active(_all.value, Days.key(now()))

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val cached = cache.readText()?.let(PartnersParser::parse)
            _all.value = cached ?: runCatching { PartnersParser.parse(bundled()) }.getOrNull().orEmpty()
            loaded = true
        }
    }

    /** @return null if a refresh is already running. */
    suspend fun refresh(force: Boolean): PartnersUpdateResult? {
        if (!running.compareAndSet(false, true)) return null
        try {
            return withContext(Dispatchers.IO) { refreshNow(force) }
        } finally {
            running.set(false)
        }
    }

    private fun refreshNow(force: Boolean): PartnersUpdateResult {
        ensureLoaded()
        val meta = cache.meta()
        if (!force && meta != null && now() - meta.checkedAt in 0 until INTERVAL_MS) return PartnersUpdateResult.NotDue
        if (!isOnline()) return PartnersUpdateResult.Failed("Няма интернет връзка.")
        val answer = try {
            fetcher.fetch(meta?.etag?.takeIf { meta.updatedAt > 0 && cache.readText() != null })
        } catch (_: Exception) {
            return PartnersUpdateResult.Failed("Неуспешна връзка с GitHub.")
        }
        val time = now()
        return when (answer.code) {
            304 -> {
                meta?.let { cache.writeMeta(it.copy(checkedAt = time)) }
                PartnersUpdateResult.NotModified
            }
            200 -> {
                val list = PartnersParser.parse(answer.body)
                if (list == null) {
                    cache.writeMeta((meta ?: CatalogMeta(null, 0, 0, versionCode)).copy(checkedAt = time))
                    PartnersUpdateResult.Failed("Списъкът с партньори не е валиден.")
                } else {
                    try {
                        cache.write(answer.body, CatalogMeta(answer.etag, time, time, versionCode))
                    } catch (_: IOException) {
                        return PartnersUpdateResult.Failed("Списъкът не можа да бъде записан.")
                    }
                    _all.value = list
                    PartnersUpdateResult.Updated(list.size)
                }
            }
            else -> PartnersUpdateResult.Failed("GitHub върна грешка (HTTP ${answer.code}).")
        }
    }

    companion object {
        const val INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
