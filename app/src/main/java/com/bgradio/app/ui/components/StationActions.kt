package com.bgradio.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.AppInfo
import com.bgradio.app.data.model.Station
import com.bgradio.app.data.repository.CheckResult
import com.bgradio.app.share.ReportReason
import com.bgradio.app.share.ShareContent
import com.bgradio.app.share.StationReport
import com.bgradio.app.share.StationShare
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.theme.DominixTheme
import com.bgradio.app.ui.MainViewModel.StationDialog
import kotlinx.coroutines.delay

/** Renders whichever station dialog is open. Placed once, at the app root. */
@Composable
fun StationActionsHost(vm: MainViewModel) {
    val dialog by vm.stationDialog.collectAsStateWithLifecycle()
    when (val d = dialog) {
        is StationDialog.Menu -> StationMenuSheet(d.station, vm)
        is StationDialog.Qr -> QrDialog(d.station, onDismiss = vm::closeStationDialog)
        is StationDialog.Report -> ReportDialog(d.station, vm, onDismiss = vm::closeStationDialog)
        is StationDialog.Check -> CheckDialog(d.station, vm, onDismiss = vm::closeStationDialog)
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationMenuSheet(station: Station, vm: MainViewModel) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = vm::closeStationDialog,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                StationLogo(station, size = 44.dp, corner = 12.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(station.name, style = MaterialTheme.typography.titleMedium)
                    Text(station.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            MenuRow(Icons.Rounded.Share, "Сподели") {
                if (StationShare.shareText(context, station)) vm.closeStationDialog()
                else error = "Няма приложение, с което да се сподели."
            }
            MenuRow(Icons.Rounded.QrCode2, "Покажи QR код") { vm.showStationDialog(StationDialog.Qr(station)) }
            MenuRow(Icons.Rounded.NetworkCheck, "Провери радиостанцията") { vm.showStationDialog(StationDialog.Check(station)) }
            MenuRow(Icons.Rounded.Flag, "Докладвай проблем") { vm.showStationDialog(StationDialog.Report(station)) }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---- QR ------------------------------------------------------------------------------------

@Composable
private fun QrDialog(station: Station, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val content = ShareContent.qrContent()
    // Generated locally; failure is reported instead of crashing.
    val card: Bitmap? = remember(station.id) { runCatching { StationShare.qrCard(station, content) }.getOrNull() }
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // message, isError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR код") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                if (card == null) {
                    Text("QR кодът не можа да бъде създаден.", color = MaterialTheme.colorScheme.error)
                } else {
                    Box(
                        Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                    ) {
                        Image(
                            card.asImageBitmap(),
                            contentDescription = "QR код към страницата за изтегляне на DOMINIX, станция ${station.name}",
                            modifier = Modifier.size(240.dp, 300.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Кодът води към официалната страница за изтегляне на DOMINIX. След като инсталира приложението, " +
                        "приятелят ви може да намери „${station.name}“ в търсачката.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(content, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                status?.let { (msg, isError) ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                }
                if (card != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (!StationShare.shareQrImage(context, station, card)) status = "Няма приложение за споделяне на изображение." to true
                        }) {
                            Icon(Icons.Rounded.Share, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Сподели")
                        }
                        if (StationShare.canSaveToGallery) {
                            OutlinedButton(onClick = {
                                status = if (StationShare.saveToGallery(context, station, card)) {
                                    "Запазено в Галерия › Pictures/Dominix" to false
                                } else {
                                    "Изображението не можа да бъде запазено." to true
                                }
                            }) {
                                Icon(Icons.Rounded.Save, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Запази")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Затвори") } },
    )
}

// ---- Report --------------------------------------------------------------------------------

@Composable
private fun ReportDialog(station: Station, vm: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var reason by rememberSaveable { mutableStateOf(ReportReason.WONT_START) }
    var description by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    // Short lock after each action so double taps don't open the same app twice.
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(busy) {
        if (busy) {
            delay(1_500)
            busy = false
        }
    }
    fun report(): StationReport = vm.buildReport(station, reason, description)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Докладвай проблем") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(station.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup()) {
                    ReportReason.entries.forEach { r ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .selectable(selected = r == reason, onClick = { reason = r }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = r == reason, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(r.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(StationReport.MAX_DESCRIPTION) },
                    label = { Text("Описание (по желание)") },
                    supportingText = { Text("${description.length}/${StationReport.MAX_DESCRIPTION}") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "Докладът съдържа само станцията, причината, версията на DOMINIX, датата и вашето описание. " +
                        "Изпраща се едва когато го потвърдите в избраното приложение.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ActionButton(Icons.Rounded.OpenInBrowser, "Изпрати чрез GitHub", enabled = !busy) {
                    busy = true
                    status = if (StationShare.start(context, Intent(Intent.ACTION_VIEW, Uri.parse(report().githubIssueUrl())))) {
                        "Отворихме GitHub. Докладът ще бъде изпратен едва след като натиснете „Submit“ там (нужен е GitHub акаунт)." to false
                    } else {
                        "Няма браузър, който да отвори GitHub. Копирайте текста и го изпратете по удобен за вас начин." to true
                    }
                }
                ActionButton(Icons.Rounded.Share, "Сподели доклада", enabled = !busy) {
                    busy = true
                    val r = report()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, r.subject)
                        putExtra(Intent.EXTRA_TEXT, r.text)
                    }
                    status = if (StationShare.start(context, Intent.createChooser(send, "Изпрати доклада чрез"))) {
                        "Избраното приложение ще изпрати доклада едва когато го потвърдите в него." to false
                    } else {
                        "Няма подходящо приложение. Копирайте текста и го изпратете по удобен за вас начин." to true
                    }
                }
                AppInfo.SUPPORT_EMAIL?.let { email ->
                    ActionButton(Icons.Rounded.Email, "Изпрати по имейл", enabled = !busy) {
                        busy = true
                        val r = report()
                        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                            putExtra(Intent.EXTRA_SUBJECT, r.subject)
                            putExtra(Intent.EXTRA_TEXT, r.text)
                        }
                        status = if (StationShare.start(context, mail)) {
                            "Отворихме имейл приложението. Докладът ще бъде изпратен, когато натиснете „Изпрати“ там." to false
                        } else {
                            "Няма имейл приложение. Копирайте текста." to true
                        }
                    }
                }
                ActionButton(Icons.Rounded.ContentCopy, "Копирай текста", enabled = true) {
                    status = if (copy(context, report().text)) "Текстът на доклада е копиран." to false
                    else "Копирането не успя." to true
                }
                status?.let { (msg, isError) ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Затвори") } },
    )
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

/** Copies [text] to the clipboard; false if the system refused. */
internal fun copy(context: Context, text: String, label: String = "Доклад DOMINIX"): Boolean = try {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    true
} catch (_: Exception) {
    false
}

// ---- Stream check --------------------------------------------------------------------------

@Composable
private fun CheckDialog(station: Station, vm: MainViewModel, onDismiss: () -> Unit) {
    val result by vm.checkResult.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Проверка на радиостанцията") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(station.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                when (val r = result) {
                    null -> {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Свързване с потока… (до ${CheckResult.ATTEMPTS} опита на адрес). Текущото радио продължава да свири.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        val good = r == CheckResult.Works || r == CheckResult.WorksViaFallback
                        Icon(
                            if (good) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = if (good) DominixTheme.colors.playing else DominixTheme.colors.warning,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(r.message, textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (result == null) "Отказ" else "Затвори") } },
        dismissButton = {
            if (result is CheckResult.NotResponding) {
                TextButton(onClick = { vm.showStationDialog(StationDialog.Report(station)) }) { Text("Докладвай") }
            }
        },
    )
}
