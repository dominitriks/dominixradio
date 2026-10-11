package com.bgradio.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.Categories
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlayerUiState
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.utils.BackgroundStatus
import com.bgradio.app.share.StationShare
import com.bgradio.app.ui.components.SectionHeader
import com.bgradio.app.ui.components.StationTile
import com.bgradio.app.ui.components.playbackFor
import com.bgradio.app.ui.theme.BgGreen
import com.bgradio.app.ui.theme.BgRed
import com.bgradio.app.ui.theme.genreColor

@Composable
fun HomeScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onOpenCategory: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenStats: () -> Unit = {},
) {
    val player by vm.playerState.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val popular by vm.popular.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val stations by vm.stations.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "header") { Header(stations.size, onOpenSearch, onOpenStats) }

        item(key = "bgwarning") {
            val bg = rememberBackgroundStatus()
            val context = LocalContext.current
            if (bg.backgroundRestricted) {
                Surface(
                    onClick = { BackgroundStatus.openAppSettings(context) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(
                        "Android ограничава Dominix на заден фон и радиото ще спира при заключен екран. " +
                            "Натиснете тук и разрешете работа на заден фон / „Без ограничения“.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (recent.isNotEmpty()) {
            item(key = "recent") {
                StationRow("Последно слушани", recent, player) { vm.togglePlay(it, recent) }
            }
        }
        if (favorites.isNotEmpty()) {
            item(key = "favorites") {
                StationRow("Любими", favorites, player, action = "Всички", onAction = onOpenFavorites) {
                    vm.togglePlay(it, favorites)
                }
            }
        }
        if (popular.isNotEmpty()) {
            item(key = "popular") {
                StationRow("Популярни радиостанции", popular, player, large = true, action = "Още", onAction = { onOpenCategory(null) }) {
                    vm.togglePlay(it, popular)
                }
            }
        }
        if (genres.isNotEmpty()) {
            item(key = "categories") {
                CategoryGrid(genres, stations, onOpenCategory)
            }
        }
        item(key = "credit") {
            Text(
                "Created by Dominantus",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun Header(stationCount: Int, onOpenSearch: () -> Unit, onOpenStats: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(BgGreen.copy(alpha = 0.28f), BgRed.copy(alpha = 0.06f), Color.Transparent),
                ),
            )
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DOMINIX", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                val context = LocalContext.current
                IconButton(onClick = {
                    if (!StationShare.shareApp(context)) {
                        Toast.makeText(context, "Няма приложение, с което да се сподели.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Сподели DOMINIX", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenStats) {
                    Icon(Icons.Rounded.BarChart, contentDescription = "Статистика", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Български радиостанции на едно място",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = onOpenSearch,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Търси сред $stationCount радиостанции",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    title: String,
    stations: List<Station>,
    player: PlayerUiState,
    large: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    onPlay: (Station) -> Unit,
) {
    Column {
        SectionHeader(title, action = action, onAction = onAction)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(stations, key = { it.id }) { s ->
                StationTile(s, player.playbackFor(s), onPlayToggle = { onPlay(s) }, large = large)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(genres: List<String>, stations: List<Station>, onOpen: (String?) -> Unit) {
    val counts = stations.groupingBy { it.genre }.eachCount()
    Column {
        SectionHeader("Категории")
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            genres.forEach { g ->
                val color = genreColor(g)
                Box(
                    Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.45f))))
                        .clickable { onOpen(g) }
                        .padding(14.dp),
                ) {
                    Text(
                        Categories.shortLabel(g),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    Text(
                        "${counts[g] ?: 0} станции",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
            if (genres.size % 2 == 1) Spacer(Modifier.weight(1f).size(0.dp))
        }
    }
}
