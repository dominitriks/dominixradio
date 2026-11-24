package com.bgradio.app.ui.components

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val BulbColors = listOf(Color(0xFFFF5A52), Color(0xFFFFD166), Color(0xFF4CD387), Color(0xFF6FB7FF))
private val WireColor = Color(0xFF3D5A4A)
private const val BULBS = 13
private const val SAG_SECTIONS = 3

/** Snow frame interval: ~30 fps is smooth enough and spares the battery. */
private const val SNOW_FRAME_NANOS = 30_000_000L

/** A position on the screen (0..1) plus how the flake falls and sways. */
private class Flake(random: Random) {
    val x = random.nextFloat()
    val y = random.nextFloat()
    val radius = 1f + random.nextFloat() * 1.8f
    val speed = 0.03f + random.nextFloat() * 0.05f // screen heights per second
    val phase = random.nextFloat() * 2f * PI.toFloat()
    val alpha = 0.2f + random.nextFloat() * 0.3f
}

/** "Remove animations" in the system settings also turns the snow off. */
private fun animationsEnabled(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

/**
 * Faint falling snow over the whole app. Draws only: it takes no touches and is invisible to
 * TalkBack. Runs only while the app is in the foreground.
 */
@Composable
fun Snowfall(modifier: Modifier = Modifier) {
    val flakes = remember { Random(24).let { r -> List(45) { Flake(r) } } }
    var nanos by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        running = animationsEnabled()
        onPauseOrDispose { running = false }
    }
    LaunchedEffect(running) {
        var last = 0L
        while (running) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                if (now - last >= SNOW_FRAME_NANOS) {
                    nanos += now - last
                    last = now
                }
            }
        }
    }
    if (!running) return
    Canvas(modifier.fillMaxSize()) {
        val seconds = nanos / 1e9f
        val sway = 12.dp.toPx()
        flakes.forEach { f ->
            drawCircle(
                color = Color.White,
                radius = f.radius.dp.toPx(),
                center = Offset(
                    f.x * size.width + sin(seconds * 0.8f + f.phase) * sway,
                    // Wraps just outside the screen so flakes do not pop in and out.
                    ((f.y + seconds * f.speed) % 1f) * (size.height + 2 * sway) - sway,
                ),
                alpha = f.alpha,
            )
        }
    }
}

/** String of softly twinkling lights. Purely decorative. */
@Composable
fun ChristmasLights(modifier: Modifier = Modifier) {
    val twinkle by rememberInfiniteTransition(label = "lights").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
    val wire = remember { Path() }
    Canvas(modifier.height(24.dp)) {
        val sag = 10.dp.toPx()
        val section = size.width / SAG_SECTIONS
        val top = 2.dp.toPx()
        wire.reset()
        wire.moveTo(0f, top)
        repeat(SAG_SECTIONS) { i ->
            // Control point at twice the sag: the curve itself reaches exactly `sag`.
            wire.quadraticTo((i + 0.5f) * section, top + 2 * sag, (i + 1) * section, top)
        }
        drawPath(wire, WireColor, style = Stroke(1.5.dp.toPx()))
        repeat(BULBS) { i ->
            val x = (i + 0.5f) / BULBS * size.width
            val t = (x % section) / section
            val center = Offset(x, top + 4 * sag * t * (1 - t) + 4.dp.toPx())
            val color = BulbColors[i % BulbColors.size]
            val glow = 0.7f + 0.3f * sin(twinkle + i * 1.7f)
            drawCircle(color, 7.dp.toPx(), center, alpha = 0.22f * glow)
            drawCircle(color, 3.5.dp.toPx(), center, alpha = glow)
        }
    }
}
