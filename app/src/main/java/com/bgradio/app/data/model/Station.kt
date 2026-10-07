package com.bgradio.app.data.model

/**
 * One radio station, as described in assets/stations.json.
 * [fallbackUrls] are tried in order when [streamUrl] fails.
 */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val fallbackUrls: List<String>,
    val genre: String,
    val city: String,
    val logoUrl: String,
    val popular: Boolean,
    val needsCheck: Boolean,
) {
    val allStreamUrls: List<String> get() = listOf(streamUrl) + fallbackUrls

    val subtitle: String get() = if (city.isBlank()) genre else "$genre · $city"
}
