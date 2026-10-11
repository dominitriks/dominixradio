package com.bgradio.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.ReleaseInfo
import com.bgradio.app.data.repository.UpdateResult
import com.bgradio.app.ui.MainViewModel

/**
 * Result of an update check. For a new version it shows the number, the release notes and a
 * button that opens the real APK link from GitHub in the browser — nothing is downloaded or
 * installed without the user doing it.
 */
@Composable
fun UpdateDialog(
    result: UpdateResult,
    installed: String,
    download: MainViewModel.UpdateDownloadUi,
    onDownload: (ReleaseInfo) -> Unit,
    onInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: (rememberVersion: Boolean) -> Unit,
) {
    val context = LocalContext.current
    when (result) {
        is UpdateResult.Available -> {
            val r = result.release
            AlertDialog(
                onDismissRequest = { onDismiss(false) },
                title = { Text("Налична е нова версия на DOMINIX") },
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        Text("Текуща версия: $installed", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Нова версия: ${r.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (r.notes.isNotBlank()) {
                            Text("Какво ново:", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(r.notes, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                        }
                        DownloadStatus(download, r)
                        if (r.apkUrl != null) {
                            TextButton(onClick = { openUrl(context, r.apkUrl) }) { Text("Отвори в браузъра") }
                        }
                    }
                },
                confirmButton = {
                    when {
                        r.apkUrl == null -> TextButton(onClick = {
                            openUrl(context, r.pageUrl)
                            onDismiss(false)
                        }) { Text("Към GitHub") }
                        download is MainViewModel.UpdateDownloadUi.Ready ||
                            download is MainViewModel.UpdateDownloadUi.NeedsPermission ->
                            TextButton(onClick = onInstall) { Text("Инсталирай") }
                        download is MainViewModel.UpdateDownloadUi.Downloading ||
                            download is MainViewModel.UpdateDownloadUi.Verifying ->
                            TextButton(onClick = onCancelDownload) { Text("Отказ") }
                        else -> TextButton(onClick = { onDownload(r) }) {
                            Text(if (download is MainViewModel.UpdateDownloadUi.Failed) "Опитай отново" else "Обнови сега")
                        }
                    }
                },
                dismissButton = {
                    if (download is MainViewModel.UpdateDownloadUi.NeedsPermission) {
                        TextButton(onClick = onOpenInstallSettings) { Text("Към настройката") }
                    } else {
                        TextButton(onClick = { onDismiss(false) }) { Text("По-късно") }
                    }
                },
            )
        }

        is UpdateResult.UpToDate -> AlertDialog(
            onDismissRequest = { onDismiss(false) },
            title = { Text("Няма нова версия") },
            text = { Text("Използвате най-новата версия ($installed).") },
            confirmButton = { TextButton(onClick = { onDismiss(false) }) { Text("Добре") } },
        )

        is UpdateResult.Failed -> AlertDialog(
            onDismissRequest = { onDismiss(false) },
            title = { Text("Проверката не успя") },
            text = { Text(result.message) },
            confirmButton = { TextButton(onClick = { onDismiss(false) }) { Text("Добре") } },
            dismissButton = {
                TextButton(onClick = {
                    openUrl(context, AppInfo.RELEASES_URL)
                    onDismiss(false)
                }) { Text("Отвори GitHub") }
            },
        )
    }
}

internal fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // no browser installed — nothing sensible to do
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes <= 0) "APK" else "%.1f MB".format(bytes / 1_048_576.0).replace('.', ',')

@Composable
private fun DownloadStatus(download: MainViewModel.UpdateDownloadUi, r: ReleaseInfo) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (download) {
        MainViewModel.UpdateDownloadUi.Idle -> Text(
            if (r.apkUrl != null) {
                "Dominix ще изтегли файла (${formatSize(r.apkSizeBytes)}), ще провери, че е непокътнат, и ще отвори " +
                    "инсталатора на Android, където потвърждавате. Любимите и настройките се запазват."
            } else {
                "Тази версия няма прикачен APK файл. Отворете страницата на версията в GitHub."
            },
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
        is MainViewModel.UpdateDownloadUi.Downloading -> Column {
            Text("Изтегляне… ${download.percent?.let { "$it%" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            if (download.percent != null) {
                LinearProgressIndicator(progress = { download.percent / 100f }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text("Можете да затворите прозореца — свалянето продължава в известията.", style = MaterialTheme.typography.bodySmall, color = muted)
        }
        MainViewModel.UpdateDownloadUi.Verifying -> Text("Проверка на файла…", style = MaterialTheme.typography.bodyMedium)
        is MainViewModel.UpdateDownloadUi.Ready -> Text(
            "Файлът е изтеглен и проверен ✓. Натиснете „Инсталирай“ и потвърдете в прозореца на Android.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        MainViewModel.UpdateDownloadUi.NeedsPermission -> Text(
            "Android трябва еднократно да разреши на Dominix да инсталира обновления. Натиснете „Към настройката“, " +
                "включете „Разрешаване от този източник“, върнете се и натиснете „Инсталирай“.",
            style = MaterialTheme.typography.bodyMedium,
        )
        is MainViewModel.UpdateDownloadUi.Failed -> Text(download.message, color = MaterialTheme.colorScheme.error)
    }
}
