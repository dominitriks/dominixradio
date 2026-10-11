package com.bgradio.app.data.repository

import java.io.File

/** What Android reports about a downloaded APK (filled from PackageManager). */
data class ApkFacts(
    val packageName: String?,
    val versionCode: Long?,
    /** null = Android could not tell (old versions) — the system installer still enforces it. */
    val sameSigner: Boolean?,
)

/**
 * Decides whether a downloaded file may be offered for installation. Pure — unit tested.
 * Returns null when everything is fine, otherwise a message for the user.
 */
object UpdateVerifier {

    fun check(
        file: File,
        expectedSize: Long,
        expectedSha256: String?,
        facts: ApkFacts?,
        ourPackage: String,
        installedVersionCode: Long,
        sha256: (File) -> String = UpdateDownloader::sha256,
    ): String? {
        if (!file.exists() || file.length() == 0L) return "Изтегленият файл е празен. Опитайте отново."
        if (expectedSize > 0 && file.length() != expectedSize) return "Изтегленият файл е непълен. Опитайте отново."
        if (expectedSha256 != null && sha256(file) != expectedSha256) {
            return "Изтегленият файл е повреден (контролната сума не съвпада). Опитайте отново."
        }
        if (facts == null) return "Изтегленият файл не е валидно Android приложение."
        if (facts.packageName != ourPackage) return "Изтегленият файл не е обновление на DOMINIX."
        val code = facts.versionCode ?: return "Версията на изтегления файл не може да бъде прочетена."
        if (code <= installedVersionCode) return "Изтегленият файл не е по-нова версия от инсталираната."
        if (facts.sameSigner == false) {
            return "Изтегленият файл е подписан с различен ключ и не може да обнови DOMINIX."
        }
        return null
    }

    /**
     * Moves a verified temporary download into place. The previous good file is only replaced
     * after verification succeeded; a failed download is deleted and never overwrites anything.
     */
    fun finalize(part: File, target: File, verified: Boolean): File? {
        if (!verified) {
            part.delete()
            return null
        }
        if (target.exists() && !target.delete()) return null
        return if (part.renameTo(target)) target else null
    }
}
