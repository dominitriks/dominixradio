package com.bgradio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bgradio.app.data.model.Station
import com.bgradio.app.ui.theme.genreColor

/**
 * Station logo loaded from the network (cached by Coil and downsampled to the view size).
 * Falls back to a coloured tile with the station initials when there is no logo.
 */
@Composable
fun StationLogo(station: Station, size: Dp, modifier: Modifier = Modifier, corner: Dp = size / 4) {
    val shape = RoundedCornerShape(corner)
    var failed by remember(station.logoUrl) { mutableStateOf(station.logoUrl.isBlank()) }
    var loaded by remember(station.logoUrl) { mutableStateOf(false) }
    Box(modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
        InitialsTile(station, size)
        if (!failed) {
            val context = LocalContext.current
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (loaded) Modifier.background(Color(0xFFF7F7FA)) else Modifier)
                    .padding(size / 10),
            ) {
                AsyncImage(
                    model = remember(station.logoUrl) {
                        ImageRequest.Builder(context)
                            .data(station.logoUrl)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = station.name,
                    contentScale = ContentScale.Fit,
                    onError = { failed = true },
                    onSuccess = { loaded = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun InitialsTile(station: Station, size: Dp) {
    val base = genreColor(station.genre)
    Box(
        Modifier
            .fillMaxSize()
            // Genre tint blended into the dark base so white initials keep ≥ 4.5:1 contrast.
            .background(
                Brush.linearGradient(
                    listOf(base.copy(alpha = 0.7f).compositeOver(Color(0xFF101322)), base.copy(alpha = 0.4f).compositeOver(Color(0xFF101322))),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(station.name),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.34f).sp,
            maxLines = 1,
        )
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}

private val skipWords = setOf("радио", "radio", "бнр", "fm", "the")

internal fun initials(name: String): String {
    val words = name.split(' ', '-', '!', '(', ')').filter { it.isNotBlank() }
    val meaningful = words.filter { it.lowercase() !in skipWords }.ifEmpty { words }
    return meaningful.take(2).joinToString("") { it.take(1).uppercase() }
}
