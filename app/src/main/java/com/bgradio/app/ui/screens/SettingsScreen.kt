package com.bgradio.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.BuildConfig
import com.bgradio.app.data.model.AppSettings
import com.bgradio.app.data.model.ThemeMode
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.data.repository.UpdateResult
import com.bgradio.app.utils.BackgroundStatus

@Composable
fun SettingsScreen(vm: MainViewModel, contentPadding: PaddingValues, onOpenStats: () -> Unit = {}) {
    val settings = vm.settings.collectAsStateWithLifecycle().value ?: AppSettings()
    val stations by vm.stations.collectAsStateWithLifecycle()
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showFeedback by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
        )

        SettingsGroup("Моето слушане") {
            val summary by vm.statsSummary.collectAsStateWithLifecycle()
            InfoRow(
                "Статистика",
                if (summary.isEmpty) "Време за слушане, любими станции, история"
                else "Общо ${com.bgradio.app.data.stats.StatsSummary.formatDuration(summary.totalSeconds)} · днес ${com.bgradio.app.data.stats.StatsSummary.formatDuration(summary.todaySeconds)}",
                Icons.Rounded.BarChart,
                onOpenStats,
            )
        }

        SettingsGroup("Външен вид") {
            ThemeOption("Тъмен режим", Icons.Rounded.DarkMode, settings.themeMode == ThemeMode.DARK) {
                vm.setThemeMode(ThemeMode.DARK)
            }
            ThemeOption("Светъл режим", Icons.Rounded.LightMode, settings.themeMode == ThemeMode.LIGHT) {
                vm.setThemeMode(ThemeMode.LIGHT)
            }
            ThemeOption("Автоматичен режим", Icons.Rounded.BrightnessAuto, settings.themeMode == ThemeMode.SYSTEM) {
                vm.setThemeMode(ThemeMode.SYSTEM)
            }
        }

        SettingsGroup("Възпроизвеждане") {
            SwitchRow(
                "Използване само на Wi-Fi",
                "Радиото няма да пуска през мобилни данни",
                Icons.Rounded.Wifi,
                settings.wifiOnly,
                vm::setWifiOnly,
            )
            SwitchRow(
                "Автоматично възстановяване на stream",
                "Повторно свързване при прекъсване (до ~4 минути)",
                Icons.Rounded.Refresh,
                settings.autoReconnect,
                vm::setAutoReconnect,
            )
        }

        SettingsGroup("Фоново възпроизвеждане") {
            val bg = rememberBackgroundStatus()
            InfoRow(
                "Работа на заден фон",
                when {
                    bg.backgroundRestricted -> "⚠ Ограничена от Android — радиото ще спира при заключен екран. Натиснете и разрешете работа на заден фон."
                    else -> "✓ Разрешена"
                },
                Icons.Rounded.BatteryChargingFull,
            ) { BackgroundStatus.openAppSettings(context) }
            InfoRow(
                "Оптимизация на батерията",
                if (bg.ignoringBatteryOptimizations) "✓ Без ограничения"
                else "Включена. Ако телефонът спира радиото, изберете „Без ограничения“ за Dominix.",
                Icons.Rounded.BatterySaver,
            ) { BackgroundStatus.openAppSettings(context) }
            if (bg.isXiaomi) {
                InfoRow(
                    "Автостарт (Xiaomi)",
                    "На Xiaomi/Redmi/POCO включете „Автостарт“ за Dominix, за да не се спира радиото.",
                    Icons.Rounded.PlayCircle,
                ) { BackgroundStatus.openXiaomiAutostart(context) }
            }
        }

        SettingsGroup("Информация") {
            InfoRow("За приложението", "Dominix · създадено от Dominantus", Icons.Rounded.Info) { showAbout = true }
            InfoRow("Изпрати обратна връзка", "Проблем или идея — чрез GitHub", Icons.Rounded.Feedback) { showFeedback = true }
            InfoRow("Поверителност", "Не събираме лични данни", Icons.Rounded.PrivacyTip) { showPrivacy = true }
            InfoRow("Версия", BuildConfig.VERSION_NAME, Icons.Rounded.Numbers, null)
            val update by vm.update.collectAsStateWithLifecycle()
            InfoRow(
                "Проверка за нова версия",
                when (val u = update) {
                    MainViewModel.UpdateUi.Checking -> "Проверява се…"
                    is MainViewModel.UpdateUi.Result -> when (val r = u.result) {
                        is UpdateResult.Available -> "Налична е версия ${r.release.version}"
                        is UpdateResult.UpToDate -> "Използвате най-новата версия"
                        is UpdateResult.Failed -> r.message
                    }
                    else -> "Проверява в GitHub Releases"
                },
                Icons.Rounded.SystemUpdate,
            ) { vm.checkForUpdates(manual = true) }
        }

        Text(
            "© 2026 Dominantus · Всички права запазени.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }

    if (showFeedback) FeedbackDialog(onDismiss = { showFeedback = false })
    if (showPrivacy) PrivacyDialog(onDismiss = { showPrivacy = false })
    if (showAbout) {
        AboutDialog(
            stationCount = stations.size,
            onDismiss = { showAbout = false },
            onFeedback = { showAbout = false; showFeedback = true },
            onPrivacy = { showAbout = false; showPrivacy = true },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(title: String, value: String, icon: ImageVector, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Re-reads the background status each time the screen is resumed (e.g. back from Settings). */
@Composable
internal fun rememberBackgroundStatus(): BackgroundStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(BackgroundStatus.read(context)) }
    LifecycleResumeEffect(Unit) {
        status = BackgroundStatus.read(context)
        onPauseOrDispose { }
    }
    return status
}
