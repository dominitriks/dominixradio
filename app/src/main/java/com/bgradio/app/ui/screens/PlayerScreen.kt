package com.bgradio.app.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlayerUiState
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.FavoriteButton
import com.bgradio.app.ui.components.PlayButtonState
import com.bgradio.app.ui.components.PlayPauseButton
import com.bgradio.app.ui.components.StationLogo
import com.bgradio.app.ui.components.statusLine
import com.bgradio.app.ui.theme.genreColor

@Composable
fun PlayerScreen(vm: MainViewModel, onClose: () -> Unit) {
    val state by vm.playerState.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val station = state.station

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (station == null) {
            EmptyState(Icons.Rounded.Radio, "Няма пусната радиостанция", "Избери радио от каталога, за да започнеш да слушаш.")
            IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Затвори")
            }
            return@Box
        }
        val accent = genreColor(station.genre)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent), endY = 1400f)),
        )
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Затвори", Modifier.size(32.dp)) }
                Text(
                    "Сега слушате",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(if (landscape) 8.dp else 28.dp))
            PulsingLogo(station, state.isPlaying, if (landscape) 150.dp else 250.dp)
            Spacer(Modifier.height(28.dp))

            Text(
                station.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(station.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            val (line, isError) = statusLine(state)
            AnimatedContent(line, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status") { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            Controls(
                state = state,
                isFavorite = station.id in favorites,
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = state.volume,
                    onValueChange = vm::setVolume,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Сила на звука в приложението: ${(state.volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PulsingLogo(station: Station, playing: Boolean, size: Dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val ring = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size + 24.dp)
                .scale(if (playing) pulse else 1f)
                .border(2.dp, ring.copy(alpha = if (playing) 0.5f else 0.15f), CircleShape),
        )
        StationLogo(station, size = size, corner = size / 2)
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FavoriteButton(isFavorite, onFavorite, iconSize = 28.dp)
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, "Предишно радио", Modifier.size(30.dp))
        }
        PlayPauseButton(
            state = when {
                state.isLoading -> PlayButtonState.LOADING
                state.playWhenReady -> PlayButtonState.PAUSE
                else -> PlayButtonState.PLAY
            },
            onClick = onPlayPause,
            size = 80.dp,
        )
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, "Следващо радио", Modifier.size(30.dp))
        }
        IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, "Спри", Modifier.size(28.dp)) }
    }
}
