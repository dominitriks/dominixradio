package com.bgradio.app.data.catalog

import com.bgradio.app.data.model.Station
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads stations.json (the same format for the copy built into the APK and the one downloaded
 * from GitHub). Pure — unit tested.
 */
object CatalogParser {

    /** A downloaded catalog larger than this is rejected (the real file is ~33 KB). */
    const val MAX_BYTES = 512 * 1024

    private val idPattern = Regex("^[a-z0-9_]{1,64}$")
    private val nonWord = Regex("\\W+")

    /** Parses a catalog; entries that are not usable are skipped. Throws on malformed JSON. */
    fun parse(json: String): List<Station> {
        val array = JSONArray(json)
        val out = ArrayList<Station>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            entry(o)?.let(out::add)
        }
        return out.distinctBy { it.id }
    }

    private fun entry(o: JSONObject): Station? {
        val name = o.optString("name").trim()
        val url = o.optString("streamUrl").trim()
        if (name.isEmpty() || name.length > 100 || !isStreamUrl(url)) return null
        val id = o.optString("id").trim().ifBlank { name.lowercase().replace(nonWord, "_") }
        if (!idPattern.matches(id)) return null
        val fallbacks = o.optJSONArray("fallbackUrls")?.let { fb ->
            (0 until fb.length()).map { fb.optString(it).trim() }.filter(::isStreamUrl)
        }.orEmpty()
        val logo = o.optString("logoUrl").ifBlank { o.optString("logo") }.trim()
        return Station(
            id = id,
            name = name,
            streamUrl = url,
            fallbackUrls = fallbacks,
            genre = o.optString("genre").trim().ifBlank { "Други" }.take(40),
            city = o.optString("city").trim().take(40),
            logoUrl = logo.takeIf(::isWebUrl).orEmpty(),
            popular = o.optBoolean("popular", false),
            needsCheck = o.optString("status") == "needs_check",
        )
    }

    private fun isWebUrl(u: String) =
        (u.startsWith("https://") || u.startsWith("http://")) && u.length in 12..500 && u.none { it.isWhitespace() }

    private fun isStreamUrl(u: String) = isWebUrl(u)

    /**
     * Decides whether a downloaded catalog may replace the current one.
     * Guards against an empty, truncated or completely different file being published by mistake.
     */
    fun validateRemote(json: String, bundledIds: Set<String>): CatalogCheck {
        if (json.length > MAX_BYTES) return CatalogCheck.Rejected("Каталогът е прекалено голям.")
        val stations = try {
            parse(json)
        } catch (_: Exception) {
            return CatalogCheck.Rejected("Каталогът не е валиден JSON.")
        }
        val minCount = maxOf(10, bundledIds.size / 2)
        if (stations.size < minCount) {
            return CatalogCheck.Rejected("Каталогът съдържа твърде малко станции (${stations.size}).")
        }
        if (bundledIds.isNotEmpty()) {
            val kept = stations.count { it.id in bundledIds }
            if (kept * 2 < bundledIds.size) {
                return CatalogCheck.Rejected("Каталогът не съвпада с познатите станции.")
            }
        }
        return CatalogCheck.Accepted(stations)
    }
}

sealed interface CatalogCheck {
    data class Accepted(val stations: List<Station>) : CatalogCheck
    data class Rejected(val reason: String) : CatalogCheck
}
