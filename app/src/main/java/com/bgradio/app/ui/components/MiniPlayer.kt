package com.bgradio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bgradio.app.player.PlayerUiState

/** Persistent player bar shown above the bottom navigation while a station is selected. */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val station = state.station ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StationLogo(station, size = 48.dp, corner = 14.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        station.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (state.sleepEndsAt != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Bedtime,
                            contentDescription = "Таймер за заспиване е включен",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                val (line, isError) = statusLine(state)
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlayPauseButton(
                state = when {
                    state.isLoading -> PlayButtonState.LOADING
                    state.playWhenReady -> PlayButtonState.PAUSE
                    else -> PlayButtonState.PLAY
                },
                onClick = onPlayPause,
                size = 42.dp,
            )
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "Следващо радио") }
            IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, "Спри") }
        }
        }
    }
}

/** Text shown under the station name; second value = whether it's an error. */
fun statusLine(state: PlayerUiState): Pair<String, Boolean> = when {
    state.errorMessage != null -> state.errorMessage to true
    state.reconnectMessage != null -> state.reconnectMessage to false
    state.isLoading -> "Свързване…" to false
    state.isPlaying -> (state.nowPlaying ?: "На живо") to false
    state.playWhenReady -> "Буфериране…" to false
    else -> "На пауза" to false
}
