package com.bgradio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlayerUiState

/** What a given card should show, derived from the global player state. */
data class StationPlayback(val isCurrent: Boolean, val button: PlayButtonState, val isPlaying: Boolean)

fun PlayerUiState.playbackFor(station: Station): StationPlayback {
    val current = this.station?.id == station.id
    val button = when {
        !current -> PlayButtonState.PLAY
        isLoading -> PlayButtonState.LOADING
        playWhenReady -> PlayButtonState.PAUSE
        else -> PlayButtonState.PLAY
    }
    return StationPlayback(current, button, current && isPlaying)
}

/** Large list card used in the catalog and favorites. */
@Composable
fun StationCard(
    station: Station,
    playback: StationPlayback,
    isFavorite: Boolean,
    onPlayToggle: () -> Unit,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "cardScale")
    val border by animateColorAsState(
        if (playback.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Transparent,
        label = "cardBorder",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null) {
                if (playback.isCurrent) onOpen() else onPlayToggle()
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.5.dp, border),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StationLogo(station, size = 68.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    station.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedVisibility(playback.isCurrent, enter = fadeIn(), exit = fadeOut()) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        EqualizerBars(animate = playback.isPlaying)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (playback.button) {
                                PlayButtonState.LOADING -> "Зареждане…"
                                PlayButtonState.PAUSE -> "Свири сега"
                                PlayButtonState.PLAY -> "На пауза"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            FavoriteButton(isFavorite, onFavorite)
            PlayPauseButton(playback.button, onPlayToggle, size = 48.dp)
        }
    }
}

/** Compact square tile for the horizontal rows on the home screen. */
@Composable
fun StationTile(
    station: Station,
    playback: StationPlayback,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val width = if (large) 156.dp else 124.dp
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "tileScale")
    Column(
        modifier
            .width(width)
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onPlayToggle),
    ) {
        Box {
            StationLogo(station, size = width, corner = 24.dp)
            PlayPauseButton(
                playback.button,
                onPlayToggle,
                size = if (large) 44.dp else 38.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
            if (playback.isCurrent) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                ) { EqualizerBars(color = MaterialTheme.colorScheme.secondary, animate = playback.isPlaying) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            station.genre,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(6.dp),
            )
        }
    }
}
