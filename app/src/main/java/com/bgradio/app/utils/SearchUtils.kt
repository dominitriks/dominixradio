package com.bgradio.app.utils

import com.bgradio.app.data.model.Station

/**
 * Search that tolerates Latin/Cyrillic: "radio 1", "радио 1", "rok" and "рок" all match.
 */
object SearchUtils {

    private val translit = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ж' to "zh",
        'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f",
        'х' to "h", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sht", 'ъ' to "a", 'ь' to "y",
        'ю' to "yu", 'я' to "ya",
    )

    private val spaces = Regex("\\s+")

    fun normalize(text: String): String {
        val sb = StringBuilder()
        for (ch in text.lowercase()) {
            when {
                translit.containsKey(ch) -> sb.append(translit[ch])
                ch.isLetterOrDigit() -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        // Common Latin spelling variants so "rock" ~ "рок", "x" ~ "х", "w" ~ "в".
        return sb.toString().replace(spaces, " ").trim()
            .replace("ck", "k").replace("x", "h").replace("w", "v")
    }

    fun matches(station: Station, query: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        val haystack = normalize("${station.name} ${station.genre} ${station.city}")
        return q.split(' ').all { haystack.contains(it) }
    }
}
