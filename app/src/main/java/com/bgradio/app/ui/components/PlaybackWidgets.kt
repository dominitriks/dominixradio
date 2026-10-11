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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bgradio.app.ui.theme.Brand
import com.bgradio.app.ui.theme.DominixTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PlayButtonState { PLAY, PAUSE, LOADING }

/** Gradient used for the main accent actions (deep purple → deep blue, white content ≥ 4.5:1). */
val AccentGradient: Brush
    get() = Brush.linearGradient(listOf(Brand.PurpleDeep, Brand.BlueDeep))

/**
 * Play / Pause / loading button.
 * [prominent] = filled brand gradient (main action, or the station that is playing);
 * otherwise a quiet tonal circle so long lists don't turn into a wall of colour.
 */
@Composable
fun PlayPauseButton(
    state: PlayButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    prominent: Boolean = true,
) {
    val content = if (prominent) Color.White else MaterialTheme.colorScheme.primary
    val label = when (state) {
        PlayButtonState.LOADING -> "Свързване, натиснете за пауза"
        PlayButtonState.PAUSE -> "Пауза"
        PlayButtonState.PLAY -> "Пусни"
    }
    Box(
        modifier
            .minimumInteractiveComponentSize() // ≥ 48dp touch area even for the small mini-player button
            .size(size.coerceAtLeast(40.dp))
            .clip(CircleShape)
            .then(
                if (prominent) Modifier.background(AccentGradient)
                else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { (scaleIn(tween(160)) + fadeIn()) togetherWith (scaleOut(tween(100)) + fadeOut()) },
            label = "playPause",
        ) { s ->
            when (s) {
                PlayButtonState.LOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.42f),
                    color = content,
                    strokeWidth = 2.5.dp,
                )
                PlayButtonState.PAUSE -> Icon(Icons.Rounded.Pause, null, Modifier.size(size * 0.52f), tint = content)
                PlayButtonState.PLAY -> Icon(Icons.Rounded.PlayArrow, null, Modifier.size(size * 0.58f), tint = content)
            }
        }
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    val tint by animateColorAsState(
        if (isFavorite) DominixTheme.colors.favorite else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "favTint",
    )
    val scale by animateFloatAsState(
        if (isFavorite) 1.12f else 1f,
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

/**
 * Small animated bars next to the station that is playing. A light status animation only — it
 * does not analyse the audio. Bars rest when [animate] is false (paused / connecting).
 */
@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = DominixTheme.colors.playing,
    animate: Boolean = true,
    height: Dp = 14.dp,
) {
    val durations = listOf(520, 680, 440, 600)
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        if (animate) {
            val transition = rememberInfiniteTransition(label = "eq")
            durations.forEachIndexed { i, d ->
                val h by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(d, delayMillis = i * 90, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "bar$i",
                )
                Bar(h, color)
            }
        } else {
            durations.forEach { _ -> Bar(0.3f, color) }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(3.dp)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(DominixTheme.colors.playing))
}
