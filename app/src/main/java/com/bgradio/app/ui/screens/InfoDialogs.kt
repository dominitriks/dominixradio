package com.bgradio.app.ui.screens

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bgradio.app.BuildConfig
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.ui.components.openUrl

/** Technical details attached to feedback — shown to the user before anything is sent. */
internal fun feedbackDetails(): String =
    "Dominix ${BuildConfig.VERSION_NAME}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
        "Устройство: ${Build.MANUFACTURER} ${Build.MODEL}"

/** GitHub "new issue" link with the details pre-filled; the user reviews it in the browser. */
internal fun feedbackUrl(): String {
    val body = "**Какво се случи / какво предлагате?**\n\n\n\n" +
        "**Радиостанция (ако е свързано):**\n\n" +
        "---\n" + feedbackDetails().replace("\n", "  \n")
    return Uri.parse(AppInfo.NEW_ISSUE_URL).buildUpon()
        .appendQueryParameter("title", "Обратна връзка: ")
        .appendQueryParameter("body", body)
        .build()
        .toString()
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Обратна връзка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ще се отвори страница в GitHub за нов сигнал (issue), където да опишете проблем или идея.")
                Text("Ще бъдат попълнени само тези данни — можете да ги промените или изтриете преди изпращане:")
                Text(feedbackDetails(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Сигналите в GitHub са публични и изискват безплатен GitHub акаунт. Не пишете лични данни.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                openUrl(context, feedbackUrl())
                onDismiss()
            }) { Text("Към GitHub") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отказ") } },
    )
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поверителност") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Dominix не събира, не изпраща и не продава лични данни. Няма регистрация, реклами, " +
                        "анализи, проследяване или отчети за сривове. Авторът не получава информация за вас.",
                )
                Section(
                    "Пази се само на телефона",
                    "Любими, последно слушани (до 10), настройки, датата на последната проверка за версия и " +
                        "кеширани логота. Изтриват се при деинсталиране. Ако е включено системното архивиране " +
                        "на Android, системата може да ги архивира в Google акаунта ви.",
                )
                Section(
                    "Мрежови заявки",
                    "• Сървърите на радиостанцията — когато пуснете радио.\n" +
                        "• Сайтовете с логата — при показване на станциите.\n" +
                        "• GitHub — проверка за нова версия (до веднъж дневно и ръчно).\n" +
                        "Те виждат IP адреса ви, както при всяка връзка. За тях важат техните правила.",
                )
                Section(
                    "Разрешения",
                    "Интернет, фонова услуга за възпроизвеждане и известия. Без местоположение, контакти, " +
                        "камера, микрофон или файлове.",
                )
                Section(
                    "Обратна връзка",
                    "Изпраща се само ако вие го направите, чрез публичен сигнал в GitHub.",
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Затвори") } },
        dismissButton = {
            TextButton(onClick = { openUrl(context, AppInfo.PRIVACY_URL) }) { Text("Пълен текст") }
        },
    )
}

@Composable
private fun Section(title: String, text: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
