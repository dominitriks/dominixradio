package com.bgradio.app.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.bgradio.app.data.model.AppVersion
import com.bgradio.app.data.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** State of an in-app update download. */
sealed interface DownloadState {
    data class Running(val percent: Int?) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * Downloads a Dominix update with the system DownloadManager (no browser involved — some
 * browsers hold APK files at 100 %), verifies it and hands it to the system installer.
 * Nothing is installed without the user confirming in Android's own install screen.
 */
class UpdateDownloader(private val context: Context) {

    private val dm = context.getSystemService(DownloadManager::class.java)
    private val dir: File? get() = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    fun fileFor(release: ReleaseInfo): File? = dir?.let { File(it, "Dominix-v${release.version}.apk") }

    /** Starts the download; returns its id, or null if it could not be started. */
    fun start(release: ReleaseInfo): Long? {
        val url = release.apkUrl ?: return null
        val target = fileFor(release) ?: return null
        // Old update files are no longer needed.
        target.parentFile?.listFiles { f -> f.name.startsWith("Dominix-") && f.name.endsWith(".apk") }?.forEach { it.delete() }
        return try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Dominix ${release.version}")
                .setDescription("Изтегляне на нова версия")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(target))
            dm?.enqueue(request)
        } catch (_: Exception) {
            null
        }
    }

    fun cancel(id: Long) {
        runCatching { dm?.remove(id) }
    }

    fun state(id: Long, target: File): DownloadState {
        val cursor = dm?.query(DownloadManager.Query().setFilterById(id))
            ?: return DownloadState.Failed("Свалянето не е достъпно на това устройство.")
        cursor.use { c ->
            if (!c.moveToFirst()) return DownloadState.Failed("Свалянето беше прекъснато.")
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL ->
                    if (target.exists()) DownloadState.Done(target) else DownloadState.Failed("Файлът не беше намерен след свалянето.")
                DownloadManager.STATUS_FAILED -> DownloadState.Failed(
                    "Свалянето не успя (код ${c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}). Проверете интернет връзката."
                )
                else -> DownloadState.Running(if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else null)
            }
        }
    }

    /**
     * Checks the downloaded file before offering to install it: checksum from GitHub (when
     * published), that it really is Dominix, and that it is the expected version.
     * Returns null when OK, otherwise a message for the user.
     */
    suspend fun verify(file: File, release: ReleaseInfo): String? = withContext(Dispatchers.IO) {
        if (release.apkSizeBytes > 0 && file.length() != release.apkSizeBytes) {
            return@withContext "Изтегленият файл е непълен. Опитайте отново."
        }
        release.apkSha256?.let { expected ->
            if (sha256(file) != expected) return@withContext "Изтегленият файл е повреден (контролната сума не съвпада). Опитайте отново."
        }
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: return@withContext "Изтегленият файл не е валидно Android приложение."
        if (info.packageName != context.packageName) {
            return@withContext "Изтегленият файл не е обновление на Dominix."
        }
        val fileVersion = AppVersion.parse(info.versionName)
        if (fileVersion == null || fileVersion < release.version) {
            return@withContext "Изтегленият файл е с неочаквана версия (${info.versionName})."
        }
        null
    }

    /** Android 8+: the user must allow Dominix to install apps once ("Install unknown apps"). */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && launch(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
        )

    /** Opens Android's installer for [file]; the user confirms there. */
    fun install(file: File): Boolean {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            return false
        }
        return launch(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"

        fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
