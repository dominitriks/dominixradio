package com.bgradio.app.ui.screens

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import com.bgradio.app.ui.components.EqualizerBars
import com.bgradio.app.ui.theme.DominixTheme
import com.bgradio.app.ui.theme.SmallCaps
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
import androidx.compose.material.icons.rounded.MoreVert
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
import com.bgradio.app.ui.components.PlayButtonState
import com.bgradio.app.ui.components.PlayPauseButton
import com.bgradio.app.ui.components.StationLogo
import com.bgradio.app.ui.components.statusLine

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

        // One soft brand glow behind the artwork; slightly stronger while playing.
        val glowA = DominixTheme.colors.glowPrimary
        val glowB = DominixTheme.colors.glowSecondary
        val glowStrength by animateFloatAsState(if (state.isPlaying) 0.34f else 0.2f, tween(800), label = "glow")
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(glowA.copy(alpha = glowStrength), glowB.copy(alpha = glowStrength * 0.35f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.28f),
                            radius = size.maxDimension * 0.6f,
                        ),
                    )
                },
        )

        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isFavorite = station.id in favorites
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        ) {
            TopBar(state, onClose = onClose, onSleep = { showSleepSheet = true }, onMore = { vm.openStationMenu(station) })
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
private fun TopBar(state: PlayerUiState, onClose: () -> Unit, onSleep: () -> Unit, onMore: () -> Unit) {
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
        IconButton(onClick = onMore) {
            Icon(Icons.Rounded.MoreVert, "Още: сподели, QR код, провери, докладвай")
        }
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
            .heightIn(min = 48.dp)
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
    val ringShape = RoundedCornerShape(size / 6 + 10.dp)
    val ringColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center) {
        // Animated ring only while audio plays — nothing runs when paused.
        val ringAlpha = if (playing) {
            val transition = rememberInfiniteTransition(label = "ring")
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
                label = "ringAlpha",
            ).value
        } else {
            0.12f
        }
        Box(
            Modifier
                .size(size + 20.dp)
                .border(1.5.dp, ringColor.copy(alpha = ringAlpha), ringShape),
        )
        StationLogo(
            station,
            size = size,
            corner = size / 6,
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(size / 6), ambientColor = DominixTheme.colors.glowPrimary, spotColor = DominixTheme.colors.glowPrimary)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(size / 6)),
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
    StatusPill(state)
    Spacer(Modifier.height(14.dp))
    Text(
        station.name,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(4.dp))
    Text(
        station.subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))

    val (line, isError) = statusLine(state)
    AnimatedContent(line, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status") { text ->
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .heightIn(min = 40.dp)
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
    Spacer(Modifier.height(12.dp))

    Controls(
        state = state,
        onPrevious = vm::previous,
        onPlayPause = { if (state.playWhenReady) vm.pause() else vm.resume() },
        onNext = vm::next,
    )
    Spacer(Modifier.height(20.dp))
    SecondaryActions(
        isFavorite = isFavorite,
        onFavorite = { vm.toggleFavorite(station.id) },
        onStop = {
            vm.stop()
            onClose()
        },
    )

    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().widthIn(max = 480.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = state.volume,
            onValueChange = vm::setVolume,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "Сила на звука" },
        )
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(24.dp))
}

/** "НА ЖИВО" / "СВЪРЗВАНЕ" / "ПАУЗА" / "ГРЕШКА" badge — text + icon, never colour alone. */
@Composable
private fun StatusPill(state: PlayerUiState) {
    val (label, color) = when {
        state.errorMessage != null -> "ГРЕШКА" to MaterialTheme.colorScheme.error
        state.reconnectMessage != null || state.isLoading -> "СВЪРЗВАНЕ" to DominixTheme.colors.warning
        state.isPlaying -> "НА ЖИВО" to DominixTheme.colors.playing
        else -> "ПАУЗА" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isPlaying) {
            // Light playback animation (not an audio analyser).
            EqualizerBars(color = color, height = 12.dp)
        } else {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = SmallCaps, color = color)
    }
}

@Composable
private fun Controls(
    state: PlayerUiState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(60.dp)) {
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
            size = 84.dp,
            modifier = Modifier.semantics {
                stateDescription = when (button) {
                    PlayButtonState.LOADING -> "Свързване"
                    PlayButtonState.PAUSE -> "Свири"
                    PlayButtonState.PLAY -> "На пауза"
                }
            },
        )
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(60.dp)) {
            Icon(Icons.Rounded.SkipNext, "Следващо радио", Modifier.size(30.dp))
        }
    }
}

/** Favourite and Stop with visible labels, below the main controls. */
@Composable
private fun SecondaryActions(isFavorite: Boolean, onFavorite: () -> Unit, onStop: () -> Unit) {
    Row(
        Modifier.widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onFavorite,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) DominixTheme.colors.favorite else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isFavorite) "В любими" else "Любима", color = MaterialTheme.colorScheme.onSurface)
        }
        OutlinedButton(
            onClick = onStop,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("Спри", color = MaterialTheme.colorScheme.onSurface)
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
