package com.bgradio.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PlayButtonState { PLAY, PAUSE, LOADING }

@Composable
fun PlayPauseButton(
    state: PlayButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    container: Color = MaterialTheme.colorScheme.primary,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = Color.White),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { (scaleIn(tween(180)) + fadeIn()) togetherWith (scaleOut(tween(120)) + fadeOut()) },
            label = "playPause",
        ) { s ->
            when (s) {
                PlayButtonState.LOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.45f),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                )

                PlayButtonState.PAUSE -> Icon(Icons.Rounded.Pause, "Пауза", Modifier.size(size * 0.55f))
                PlayButtonState.PLAY -> Icon(Icons.Rounded.PlayArrow, "Пусни", Modifier.size(size * 0.6f))
            }
        }
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    val tint by animateColorAsState(
        if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "favTint",
    )
    val scale by animateFloatAsState(
        if (isFavorite) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "favScale",
    )
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Премахни от любими" else "Добави в любими",
            tint = tint,
            modifier = Modifier.size(iconSize).scale(scale),
        )
    }
}

/** Small animated "equalizer" shown next to the station that is currently playing. */
@Composable
fun EqualizerBars(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(520, 680, 440)
    Row(modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        durations.forEachIndexed { i, d ->
            val h by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(d, delayMillis = i * 90, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(if (animate) h else 0.3f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
}
