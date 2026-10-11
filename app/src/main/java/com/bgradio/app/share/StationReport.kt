package com.bgradio.app.share

import android.net.Uri
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.Station
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ReportReason(val label: String) {
    WONT_START("Радиото не се стартира"),
    DROPS("Потокът прекъсва постоянно"),
    WRONG_CONTENT("Чува се грешно съдържание или друга станция"),
    WRONG_INFO("Логото или информацията са грешни"),
    OTHER("Друга причина"),
}

/**
 * A problem report about one station. Contains only: station name and id, reason, app version,
 * date/time and the user's optional text — no device identifiers, location or contacts.
 */
data class StationReport(
    val station: Station,
    val reason: ReportReason,
    val description: String,
    val appVersion: String,
    val timeMs: Long,
    val zone: TimeZone = TimeZone.getDefault(),
) {
    val cleanDescription: String get() = description.trim().take(MAX_DESCRIPTION)

    val subject: String get() = "Проблем със станция: ${station.name} — ${reason.label}"

    val text: String
        get() {
            val time = SimpleDateFormat("dd.MM.yyyy HH:mm z", Locale("bg", "BG")).apply { timeZone = zone }.format(Date(timeMs))
            return buildString {
                appendLine("Доклад за радиостанция — DOMINIX")
                appendLine()
                appendLine("Станция: ${station.name}")
                appendLine("Идентификатор: ${station.id}")
                appendLine("Проблем: ${reason.label}")
                appendLine("Версия на DOMINIX: $appVersion")
                appendLine("Дата и час: $time")
                if (cleanDescription.isNotEmpty()) {
                    appendLine()
                    appendLine("Описание:")
                    appendLine(cleanDescription)
                }
            }.trimEnd()
        }

    /** Pre-filled GitHub issue; the user reviews and submits it in the browser. */
    fun githubIssueUrl(): String = Uri.parse(AppInfo.NEW_ISSUE_URL).buildUpon()
        .appendQueryParameter("title", subject)
        .appendQueryParameter("body", text)
        .build().toString()

    companion object {
        const val MAX_DESCRIPTION = 500
    }
}
