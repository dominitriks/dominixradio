package com.bgradio.app.ui.screens

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlayerUiState
import com.bgradio.app.player.SleepTimer
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.FavoriteButton
import com.bgradio.app.ui.components.PlayButtonState
import com.bgradio.app.ui.components.PlayPauseButton
import com.bgradio.app.ui.components.StationLogo
import com.bgradio.app.ui.components.statusLine
import com.bgradio.app.ui.theme.genreColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: MainViewModel, onClose: () -> Unit) {
    val state by vm.playerState.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val station = state.station
    var showSleepSheet by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (station == null) {
            EmptyState(Icons.Rounded.Radio, "Няма избрана радиостанция", "Избери радио от каталога, за да започнеш да слушаш.")
            IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Затвори")
            }
            return@Box
        }

        // Soft genre-coloured backdrop.
        val accent by animateColorAsState(genreColor(station.genre), tween(600), label = "accent")
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.55f),
                        0.55f to accent.copy(alpha = 0.12f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isFavorite = station.id in favorites
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            TopBar(state, onClose = onClose, onSleep = { showSleepSheet = true })
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Artwork(station, state.isPlaying, 200.dp)
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Details(station, state, isFavorite, vm, onClose)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))
                    val maxArt = (LocalConfiguration.current.screenWidthDp.dp - 72.dp).coerceIn(180.dp, 320.dp)
                    Artwork(station, state.isPlaying, maxArt)
                    Spacer(Modifier.height(28.dp))
                    Details(station, state, isFavorite, vm, onClose)
                }
            }
        }
    }

    if (showSleepSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SleepTimerSheet(
                state = state,
                onSelect = { minutes ->
                    vm.setSleepTimer(minutes)
                    showSleepSheet = false
                },
            )
        }
    }
}

@Composable
private fun TopBar(state: PlayerUiState, onClose: () -> Unit, onSleep: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Затвори плейъра", Modifier.size(32.dp)) }
        Text(
            "Сега слушате",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        SleepTimerButton(state.sleepEndsAt, onSleep)
    }
}

/** Moon button; shows the countdown while the sleep timer runs. */
@Composable
private fun SleepTimerButton(endsAt: Long?, onClick: () -> Unit) {
    val remaining = rememberRemaining(endsAt)
    val active = remaining != null
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        modifier = Modifier
            .height(48.dp)
            .semantics {
                contentDescription = if (active) "Таймер за заспиване, остават ${SleepTimer.format(remaining!!)}" else "Таймер за заспиване"
            },
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (remaining != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    SleepTimer.format(remaining),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Remaining sleep time, ticking once per second; null when the timer is off. */
@Composable
internal fun rememberRemaining(endsAt: Long?): Long? {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(endsAt) {
        while (endsAt != null) {
            now = SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(1_000)
        }
    }
    return endsAt?.let { (it - now).coerceAtLeast(0) }
}

@Composable
private fun Artwork(station: Station, playing: Boolean, size: Dp) {
    val transition = rememberInfiniteTransition(label = "glow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    val ring = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size + 16.dp)
                .border(
                    width = 2.dp,
                    color = ring.copy(alpha = if (playing) glow else 0.12f),
                    shape = RoundedCornerShape(size / 6 + 8.dp),
                ),
        )
        StationLogo(
            station,
            size = size,
            corner = size / 6,
            modifier = Modifier.shadow(24.dp, RoundedCornerShape(size / 6)),
        )
    }
}

@Composable
private fun ColumnScope.Details(
    station: Station,
    state: PlayerUiState,
    isFavorite: Boolean,
    vm: MainViewModel,
    onClose: () -> Unit,
) {
    Text(
        station.name,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        station.subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(14.dp))
    StatusPill(state)
    Spacer(Modifier.height(8.dp))

    val (line, isError) = statusLine(state)
    AnimatedContent(line, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status") { text ->
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .height(42.dp)
                .widthIn(max = 420.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (state.errorMessage != null) {
        OutlinedButton(onClick = vm::retry, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Опитай отново")
        }
    }
    Spacer(Modifier.height(8.dp))

    Controls(
        state = state,
        isFavorite = isFavorite,
        onFavorite = { vm.toggleFavorite(station.id) },
        onPrevious = vm::previous,
        onPlayPause = { if (state.playWhenReady) vm.pause() else vm.resume() },
        onNext = vm::next,
        onStop = {
            vm.stop()
            onClose()
        },
    )

    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth().widthIn(max = 480.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = state.volume,
            onValueChange = vm::setVolume,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "Сила на звука" },
        )
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(24.dp))
}

/** "НА ЖИВО" / "СВЪРЗВАНЕ" / "ПАУЗА" / "ГРЕШКА" badge. */
@Composable
private fun StatusPill(state: PlayerUiState) {
    val (label, color) = when {
        state.errorMessage != null -> "ГРЕШКА" to MaterialTheme.colorScheme.error
        state.reconnectMessage != null || state.isLoading -> "СВЪРЗВАНЕ" to MaterialTheme.colorScheme.tertiary
        state.isPlaying -> "НА ЖИВО" to MaterialTheme.colorScheme.secondary
        else -> "ПАУЗА" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (state.isPlaying) pulse else 1f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Controls(
    state: PlayerUiState,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().widthIn(max = 480.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FavoriteButton(isFavorite, onFavorite, iconSize = 28.dp)
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, "Предишно радио", Modifier.size(30.dp))
        }
        val button = when {
            state.isLoading -> PlayButtonState.LOADING
            state.playWhenReady -> PlayButtonState.PAUSE
            else -> PlayButtonState.PLAY
        }
        PlayPauseButton(
            state = button,
            onClick = onPlayPause,
            size = 80.dp,
            modifier = Modifier.semantics {
                stateDescription = when (button) {
                    PlayButtonState.LOADING -> "Свързване"
                    PlayButtonState.PAUSE -> "Свири"
                    PlayButtonState.PLAY -> "На пауза"
                }
            },
        )
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, "Следващо радио", Modifier.size(30.dp))
        }
        IconButton(onClick = onStop, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Stop, "Спри", Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SleepTimerSheet(state: PlayerUiState, onSelect: (Int) -> Unit) {
    val remaining = rememberRemaining(state.sleepEndsAt)
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            "Таймер за заспиване",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            if (remaining != null) "Радиото ще спре след ${SleepTimer.format(remaining)}"
            else "Радиото ще затихне плавно и ще спре след избраното време.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SleepTimer.OPTIONS.forEach { minutes ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onSelect(minutes) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(sleepLabel(minutes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        if (remaining != null) {
            TextButton(
                onClick = { onSelect(0) },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Изключи таймера")
            }
        }
    }
}

private fun sleepLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes минути"
    minutes == 60 -> "1 час"
    minutes % 60 == 0 -> "${minutes / 60} часа"
    else -> "${minutes / 60} ч. ${minutes % 60} мин."
}
