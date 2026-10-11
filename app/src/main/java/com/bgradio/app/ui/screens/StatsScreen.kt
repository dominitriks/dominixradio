package com.bgradio.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.Station
import com.bgradio.app.data.stats.ListeningSession
import com.bgradio.app.data.stats.StatsSummary
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.StationLogo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fmt = StatsSummary.Companion::formatDuration

@Composable
fun StatsScreen(vm: MainViewModel, contentPadding: PaddingValues, onBack: () -> Unit) {
    val summary by vm.statsSummary.collectAsStateWithLifecycle()
    val stations by vm.stations.collectAsStateWithLifecycle()
    val byId = remember(stations) { stations.associateBy { it.id } }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Text("Статистика", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (!summary.isEmpty) {
                IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteOutline, "Изтрий статистиката") }
            }
        }

        if (summary.isEmpty) {
            EmptyState(
                Icons.Rounded.BarChart,
                "Все още няма статистика",
                "Пуснете радио — времето за слушане се отчита автоматично, само докато звукът реално върви.",
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { StatCard("Общо", fmt(summary.totalSeconds)) }
                item { StatCard("Днес", fmt(summary.todaySeconds)) }
                item { StatCard("Последните 7 дни", fmt(summary.last7DaysSeconds)) }
                item { StatCard("Този месец", fmt(summary.thisMonthSeconds)) }
                item { StatCard("Сесии на слушане", summary.sessionCount.toString()) }
                summary.topStations.firstOrNull()?.let { (id, secs) ->
                    item { StatCard("Най-слушана", byId[id]?.name ?: id, sub = fmt(secs)) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { ChartCard(summary.chart) }
                if (summary.topStations.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { TopStationsCard(summary.topStations, byId) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Последни сесии", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                }
                items(summary.recentSessions.take(30), span = { GridItemSpan(maxLineSpan) }, key = { "${it.stationId}@${it.startMs}" }) { s ->
                    SessionRow(s, byId[s.stationId])
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Отчита се само времето, в което звукът реално върви. Данните са само на този телефон.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Изтриване на статистиката") },
            text = { Text("Цялото време за слушане и историята на сесиите ще бъдат изтрити. Действието не може да се отмени.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearStats()
                    confirmClear = false
                }) { Text("Изтрий", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отказ") } },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, sub: String? = null) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Bar chart: minutes per day for the last 14 days. */
@Composable
private fun ChartCard(chart: List<Pair<String, Long>>) {
    val bar = MaterialTheme.colorScheme.primary
    val today = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val max = (chart.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(60L)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(600)) }
    val description = "Слушане по дни: " + chart.joinToString { (day, s) -> "${day.takeLast(2)}.${day.substring(5, 7)} ${fmt(s)}" }

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Последните ${chart.size} дни", style = MaterialTheme.typography.titleMedium)
            Text("Най-много: ${fmt(chart.maxOfOrNull { it.second } ?: 0)} на ден", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .semantics { contentDescription = description },
            ) {
                val n = chart.size.coerceAtLeast(1)
                val slot = size.width / n
                val w = slot * 0.6f
                chart.forEachIndexed { i, (_, secs) ->
                    val x = i * slot + (slot - w) / 2
                    drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 3))
                    val h = size.height * (secs.toFloat() / max) * grow.value
                    if (h > 0f) {
                        drawRoundRect(
                            if (i == chart.lastIndex) today else bar,
                            Offset(x, size.height - h), Size(w, h), CornerRadius(w / 3),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                chart.forEachIndexed { i, (day, _) ->
                    Text(
                        if (i % 2 == chart.lastIndex % 2) day.takeLast(2) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopStationsCard(top: List<Pair<String, Long>>, byId: Map<String, Station>) {
    val max = top.first().second.coerceAtLeast(1)
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Топ 5 радиостанции", style = MaterialTheme.typography.titleMedium)
            top.forEachIndexed { i, (id, secs) ->
                val station = byId[id]
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text("${i + 1}.", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(24.dp))
                    if (station != null) {
                        StationLogo(station, size = 36.dp, corner = 10.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(station?.name ?: id, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(fmt(secs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { secs.toFloat() / max },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private val sessionDate = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("bg", "BG"))

@Composable
private fun SessionRow(s: ListeningSession, station: Station?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (station != null) {
            StationLogo(station, size = 40.dp, corner = 12.dp)
        } else {
            Box(Modifier.width(40.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(station?.name ?: s.stationId, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sessionDate.format(Date(s.startMs)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(fmt(s.seconds), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
