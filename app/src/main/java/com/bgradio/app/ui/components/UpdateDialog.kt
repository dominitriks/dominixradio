package com.bgradio.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.repository.UpdateResult

/**
 * Result of an update check. For a new version it shows the number, the release notes and a
 * button that opens the real APK link from GitHub in the browser — nothing is downloaded or
 * installed without the user doing it.
 */
@Composable
fun UpdateDialog(result: UpdateResult, installed: String, onDismiss: (rememberVersion: Boolean) -> Unit) {
    val context = LocalContext.current
    when (result) {
        is UpdateResult.Available -> {
            val r = result.release
            AlertDialog(
                onDismissRequest = { onDismiss(false) },
                title = { Text("Нова версия: ${r.version}") },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            "Инсталирана версия: $installed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (r.notes.isNotBlank()) {
                            Text("Какво ново:", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(r.notes, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(
                            if (r.apkUrl != null) {
                                "„Изтегли APK“ отваря файла (${formatSize(r.apkSizeBytes)}) в браузъра. " +
                                    "След изтеглянето го отворете и потвърдете инсталирането. " +
                                    "Любимите и настройките се запазват."
                            } else {
                                "Тази версия няма прикачен APK файл. Отворете страницата на версията в GitHub."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        openUrl(context, r.apkUrl ?: r.pageUrl)
                        onDismiss(false)
                    }) { Text(if (r.apkUrl != null) "Изтегли APK" else "Към GitHub") }
                },
                dismissButton = {
                    TextButton(onClick = { onDismiss(true) }) { Text("По-късно") }
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
