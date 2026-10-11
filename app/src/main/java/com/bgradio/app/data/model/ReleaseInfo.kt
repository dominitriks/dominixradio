package com.bgradio.app.data.model

import org.json.JSONObject

/** Semantic version "1.2.0" (a leading "v" and suffixes like "-debug" are ignored). */
data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    override fun toString() = parts.joinToString(".")

    companion object {
        private val pattern = Regex("^[vV]?(\\d+(?:\\.\\d+){0,3})")

        fun parse(text: String?): AppVersion? {
            val match = pattern.find(text?.trim().orEmpty()) ?: return null
            return AppVersion(match.groupValues[1].split('.').map { it.toInt() })
        }
    }
}

/** The parts of a GitHub release we use. */
data class ReleaseInfo(
    val version: AppVersion,
    val title: String,
    val notes: String,
    val pageUrl: String,
    /** Direct link to the release APK, or null if the release has no APK asset. */
    val apkUrl: String?,
    val apkSizeBytes: Long,
    /** SHA-256 of the APK as published by GitHub (asset "digest"), lowercase hex; null if absent. */
    val apkSha256: String? = null,
) {
    companion object {
        /** Parses GitHub's `releases/latest` JSON. Returns null for anything unusable. */
        fun parse(json: String): ReleaseInfo? = try {
            val o = JSONObject(json)
            if (o.optBoolean("draft") || o.optBoolean("prerelease")) {
                null
            } else {
                val version = AppVersion.parse(o.optString("tag_name")) ?: AppVersion.parse(o.optString("name"))
                version?.let {
                    val assets = o.optJSONArray("assets")
                    val apks = (0 until (assets?.length() ?: 0))
                        .map { i -> assets!!.getJSONObject(i) }
                        .filter { a -> a.optString("name").endsWith(".apk", ignoreCase = true) }
                    // Prefer the release build over a debug one.
                    val apk = apks.firstOrNull { a -> !a.optString("name").contains("debug", true) } ?: apks.firstOrNull()
                    ReleaseInfo(
                        version = it,
                        title = o.optString("name").ifBlank { "Dominix $it" },
                        notes = cleanNotes(o.optString("body")),
                        pageUrl = o.optString("html_url").ifBlank { AppInfo.RELEASES_URL },
                        apkUrl = apk?.optString("browser_download_url")?.takeIf(::isTrustedApkUrl),
                        apkSizeBytes = apk?.optLong("size") ?: 0L,
                        apkSha256 = apk?.optString("digest")
                            ?.takeIf { d -> d.startsWith("sha256:", ignoreCase = true) }
                            ?.substringAfter(':')?.lowercase()
                            ?.takeIf { h -> h.length == 64 && h.all { c -> c in '0'..'9' || c in 'a'..'f' } },
                    )
                }
            }
        } catch (_: Exception) {
            null
        }

        /**
         * Only APK links of the official repository are accepted — never an arbitrary URL that
         * appears in an API answer.
         */
        fun isTrustedApkUrl(url: String): Boolean {
            if (!url.startsWith(AppInfo.APK_DOWNLOAD_PREFIX)) return false
            val rest = url.removePrefix(AppInfo.APK_DOWNLOAD_PREFIX) // "v1.3.3/Dominix-v1.3.3.apk"
            return !rest.contains("..") && !rest.contains('?') && !rest.contains('#') &&
                rest.count { it == '/' } == 1 && rest.endsWith(".apk", ignoreCase = true)
        }

        /** Markdown release notes → short plain text (max ~12 lines). */
        fun cleanNotes(body: String): String = body
            .replace("\r", "")
            .lines()
            .map { line ->
                line.replace(Regex("^#{1,6}\\s*"), "")
                    .replace("**", "")
                    .replace("`", "")
                    .replace(Regex("^\\s*[-*]\\s+"), "• ")
                    .trimEnd()
            }
            .filterNot { it.startsWith("SHA-256", ignoreCase = true) || it.startsWith("©") }
            .dropWhile { it.isBlank() }
            .take(12)
            .joinToString("\n")
            .trim()
    }
}
