package com.bgradio.app.data.repository

import android.content.Context
import android.util.Log
import com.bgradio.app.data.model.Categories
import com.bgradio.app.data.model.Station
import org.json.JSONArray

/**
 * Reads the station catalog from assets/stations.json.
 * Edit that file to add, remove or change stations — no code changes needed.
 */
class StationRepository(private val context: Context) {

    val stations: List<Station> by lazy { load() }

    private val byId: Map<String, Station> by lazy { stations.associateBy { it.id } }

    fun find(id: String?): Station? = id?.let { byId[it] }

    val genres: List<String> by lazy {
        val present = stations.map { it.genre }.toSet()
        Categories.ORDER.filter { it in present } + (present - Categories.ORDER.toSet()).sorted()
    }

    private fun load(): List<Station> = try {
        val json = context.assets.open(FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val name = o.optString("name").trim()
                val url = o.optString("streamUrl").trim()
                if (name.isEmpty() || url.isEmpty()) continue
                val fallbacks = o.optJSONArray("fallbackUrls")?.let { fb ->
                    (0 until fb.length()).map { fb.getString(it) }.filter { it.isNotBlank() }
                }.orEmpty()
                add(
                    Station(
                        id = o.optString("id").ifBlank { name.lowercase().replace(Regex("\\W+"), "_") },
                        name = name,
                        streamUrl = url,
                        fallbackUrls = fallbacks,
                        genre = o.optString("genre").ifBlank { "Други" },
                        city = o.optString("city"),
                        logoUrl = o.optString("logoUrl").ifBlank { o.optString("logo") },
                        popular = o.optBoolean("popular", false),
                        needsCheck = o.optString("status") == "needs_check",
                    )
                )
            }
        }.distinctBy { it.id }
    } catch (e: Exception) {
        Log.e("StationRepository", "Cannot read $FILE", e)
        emptyList()
    }

    private companion object {
        const val FILE = "stations.json"
    }
}
