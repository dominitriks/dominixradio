package com.bgradio.app.player

import com.bgradio.app.utils.SearchUtils

/**
 * "Now playing" information taken from the stream (ICY StreamTitle or HLS ID3 tags).
 *
 * Streams send a lot of things that are not songs: empty titles, the station's own slogan,
 * advertisements with web addresses, internal codes, broken encodings. [parse] only accepts text
 * that looks like real track information and returns null for everything else — the app then just
 * shows "На живо" instead of inventing anything.
 */
data class TrackInfo(val artist: String?, val title: String) {

    /** "Изпълнител — Песен", or just the title. */
    val display: String get() = if (artist.isNullOrBlank()) title else "$artist — $title"

    companion object {
        private val spaces = Regex("\\s+")
        private val trailingCode = Regex("\\s*\\[[A-Za-z0-9]{3,6}]\\s*$")
        // "01 Song", "3. Song" — but not "50 Cent"
        private val leadingTrackNo = Regex("^(0\\d|\\d{1,3}[.)])\\s+(?=\\p{L})")
        private val webAddress = Regex(
            "(https?://|www\\.|@?[\\p{L}0-9-]+\\.(bg|com|net|org|eu|fm|info|io|tv|ru|de|uk|me)\\b)",
            RegexOption.IGNORE_CASE,
        )
        private val mojibake = Regex("[\\uFFFD]|Ã.|Ð.|Ñ.")
        private val letters = Regex("\\p{L}")
        private val genericJunk = setOf(
            "unknown", "unknown artist", "untitled", "n/a", "na", "null", "none", "live", "on air",
            "advertisement", "advert", "commercial", "jingle", "news", "radio", "stream", "-",
        )

        /**
         * @param rawTitle StreamTitle (ICY) or TIT2 title (ID3)
         * @param rawArtist TPE1 artist (ID3) if the stream provides it separately
         */
        fun parse(rawTitle: String?, rawArtist: String? = null, stationName: String = ""): TrackInfo? {
            val combined = when {
                !rawArtist.isNullOrBlank() && !rawTitle.isNullOrBlank() -> "$rawArtist - $rawTitle"
                else -> rawTitle ?: return null
            }
            var text = clean(combined)
            if (text.length < 2 || text.length > 200) return null
            if (mojibake.containsMatchIn(text)) return null
            if (webAddress.containsMatchIn(text)) return null // ads: "apteka.puls.bg - ..."
            if (!text.contains(' ') && text.contains('_')) return null // "Hear_the_music."
            if (!letters.containsMatchIn(text)) return null

            text = text.replace(trailingCode, "").replace(leadingTrackNo, "").trim()

            val sep = text.indexOf(" - ")
            var artist: String? = null
            var title = text
            if (sep >= 0) {
                artist = text.substring(0, sep).trim().trim('-', ' ').ifBlank { null }
                title = text.substring(sep + 3).trim().trim('-', ' ')
            } else if (text.startsWith("- ") || text.startsWith("-")) {
                title = text.trimStart('-', ' ')
            }
            if (title.isBlank()) {
                title = artist ?: return null
                artist = null
            }
            if (isJunk(title) || (artist != null && isJunk(artist))) return null
            if (isStationSelfReference(artist, title, stationName)) return null
            return TrackInfo(artist, title)
        }

        private fun clean(s: String): String =
            s.trim().trim('"', '\'', '“', '”', '„').replace(spaces, " ").trim()

        private fun isJunk(part: String): Boolean = part.lowercase().trim('.', '!', ' ') in genericJunk

        /** "RADIO VERONIKA - ХИТОВЕТЕ СА ТУК!", "Easy Radio - Your Relaxing Music Mix!" */
        private fun isStationSelfReference(artist: String?, title: String, stationName: String): Boolean {
            val station = core(stationName)
            val a = artist?.let(::core).orEmpty()
            if (station.length >= 3 && (a == station || core(title) == station)) return true
            // Artist named "Radio X" is almost always a slogan, not a performer.
            return SearchUtils.normalize(artist.orEmpty()).startsWith("radio ")
        }

        /** Station name without the words "радио/radio/fm" for comparisons, transliterated. */
        private fun core(name: String): String = SearchUtils.normalize(name)
            .split(' ')
            .filter { it.isNotBlank() && it !in setOf("radio", "fm", "the") }
            .joinToString(" ")
    }
}
