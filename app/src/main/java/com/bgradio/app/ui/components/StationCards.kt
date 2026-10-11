package com.bgradio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import com.bgradio.app.ui.theme.Dimens
import com.bgradio.app.ui.theme.DominixTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlayerUiState
import com.bgradio.app.player.StationIssue

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

/** List card used in the catalog and favorites. Long press opens the station actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCard(
    station: Station,
    playback: StationPlayback,
    isFavorite: Boolean,
    onPlayToggle: () -> Unit,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    issue: StationIssue? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cardScale")
    val playing = DominixTheme.colors.playing
    val border by animateColorAsState(
        if (playback.isCurrent) playing.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        label = "cardBorder",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = if (playback.isCurrent) "Отвори плейъра" else "Пусни",
                onLongClickLabel = "Още действия",
                onLongClick = onLongPress,
            ) {
                if (playback.isCurrent) onOpen() else onPlayToggle()
            }
            .semantics {
                stateDescription = when {
                    !playback.isCurrent -> ""
                    playback.button == PlayButtonState.LOADING -> "Свързване"
                    playback.isPlaying -> "Свири сега"
                    else -> "На пауза"
                }
            },
        shape = Dimens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (playback.isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, border),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            // Accent bar marks the selected station (together with the text label below).
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(if (playback.isCurrent) playing else Color.Transparent),
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 9.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationLogo(station, size = 56.dp, corner = Dimens.RadiusSmall)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        station.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        station.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (issue != null && !playback.isPlaying) {
                        val issueColor = if (issue == StationIssue.BROKEN) MaterialTheme.colorScheme.error else DominixTheme.colors.warning
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = issueColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(issue.label, style = MaterialTheme.typography.labelMedium, color = issueColor)
                        }
                    }
                    AnimatedVisibility(playback.isCurrent, enter = fadeIn(), exit = fadeOut()) {
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            EqualizerBars(animate = playback.isPlaying, height = 12.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (playback.button) {
                                    PlayButtonState.LOADING -> "Свързване…"
                                    PlayButtonState.PAUSE -> "Свири сега"
                                    PlayButtonState.PLAY -> "На пауза"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = playing,
                            )
                        }
                    }
                }
                FavoriteButton(isFavorite, onFavorite)
                PlayPauseButton(playback.button, onPlayToggle, size = 44.dp, prominent = playback.isCurrent)
            }
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
    val width = if (large) 148.dp else 120.dp
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tileScale")
    Column(
        modifier
            .width(width)
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = "Пусни ${station.name}",
                onClick = onPlayToggle,
            )
            .semantics { if (playback.isCurrent) stateDescription = if (playback.isPlaying) "Свири сега" else "Избрана" },
    ) {
        Box {
            StationLogo(
                station,
                size = width,
                corner = Dimens.RadiusLarge,
                modifier = if (playback.isCurrent) {
                    Modifier.border(2.dp, DominixTheme.colors.playing.copy(alpha = 0.7f), RoundedCornerShape(Dimens.RadiusLarge))
                } else {
                    Modifier
                },
            )
            PlayPauseButton(
                playback.button,
                onPlayToggle,
                size = if (large) 42.dp else 36.dp,
                prominent = playback.isCurrent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            )
            if (playback.isCurrent) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) { EqualizerBars(animate = playback.isPlaying, height = 12.dp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            station.genre,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelLarge) }
        }
    }
}
