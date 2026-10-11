package com.bgradio.app.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.bgradio.app.BuildConfig
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

    /** Final, verified update file. */
    fun fileFor(release: ReleaseInfo): File? = dir?.let { File(it, "Dominix-v${release.version}.apk") }

    /** Temporary file the download is written to; only renamed to [fileFor] after verification. */
    fun partFor(release: ReleaseInfo): File? = dir?.let { File(it, "Dominix-v${release.version}-partial.apk") }

    /** Starts the download into the temporary file; returns its id, or null if it could not start. */
    fun start(release: ReleaseInfo): Long? {
        val url = release.apkUrl?.takeIf(ReleaseInfo::isTrustedApkUrl) ?: return null
        val part = partFor(release) ?: return null
        val keep = fileFor(release)?.name
        // Leftovers of other versions and old partial downloads are not needed.
        part.parentFile?.listFiles { f -> f.name.startsWith("Dominix-") && f.name.endsWith(".apk") && f.name != keep }
            ?.forEach { it.delete() }
        return try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("DOMINIX ${release.version}")
                .setDescription("Изтегляне на нова версия")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(part))
            dm?.enqueue(request)
        } catch (_: Exception) {
            null
        }
    }

    fun cancel(id: Long) {
        runCatching { dm?.remove(id) }
    }

    fun state(id: Long, part: File): DownloadState {
        val cursor = dm?.query(DownloadManager.Query().setFilterById(id))
            ?: return DownloadState.Failed("Свалянето не е достъпно на това устройство.")
        cursor.use { c ->
            if (!c.moveToFirst()) return DownloadState.Failed("Свалянето беше прекъснато.")
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return mapStatus(status, done, total, reason, part)
        }
    }

    /**
     * Verifies a finished download and moves it into place. Returns the installable file, or the
     * problem. A failed check deletes the temporary file and leaves any earlier good file alone.
     */
    suspend fun verifyAndFinalize(part: File, release: ReleaseInfo): Pair<File?, String?> = withContext(Dispatchers.IO) {
        val problem = verify(part, release)
        val target = fileFor(release)
        val finalFile = if (target == null) null else UpdateVerifier.finalize(part, target, verified = problem == null)
        when {
            problem != null -> null to problem
            finalFile == null -> null to "Файлът не можа да бъде записан. Опитайте отново."
            else -> finalFile to null
        }
    }

    /** Checks an update file (also one downloaded earlier) — see [UpdateVerifier.check]. */
    suspend fun verify(file: File, release: ReleaseInfo): String? = withContext(Dispatchers.IO) {
        UpdateVerifier.check(
            file = file,
            expectedSize = release.apkSizeBytes,
            expectedSha256 = release.apkSha256,
            facts = apkFacts(file),
            ourPackage = context.packageName,
            installedVersionCode = BuildConfig.VERSION_CODE.toLong(),
        )
    }

    /** Package name, versionCode and signer of an APK file as Android reads them. */
    private fun apkFacts(file: File): ApkFacts? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
        val info = try {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(file.path, flags)
        } catch (_: Exception) {
            null
        } ?: return null
        return ApkFacts(
            packageName = info.packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            sameSigner = sameSigner(info),
        )
    }

    /** Compares signing certificates with the installed app; null when Android can't tell. */
    private fun sameSigner(archive: PackageInfo): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val theirs = archive.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet()
            val ours = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet()
            if (theirs.isNullOrEmpty() || ours.isNullOrEmpty()) null else theirs == ours
        } catch (_: Exception) {
            null
        }
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

        /** DownloadManager status → state. Pure — unit tested. */
        fun mapStatus(status: Int, done: Long, total: Long, reason: Int, part: File): DownloadState = when (status) {
            DownloadManager.STATUS_SUCCESSFUL ->
                if (part.exists() && part.length() > 0) DownloadState.Done(part)
                else DownloadState.Failed("Файлът не беше намерен след свалянето. Опитайте отново.")
            DownloadManager.STATUS_FAILED ->
                DownloadState.Failed("Свалянето беше прекъснато (код $reason). Проверете интернет връзката и опитайте отново.")
            else -> DownloadState.Running(if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else null)
        }

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
