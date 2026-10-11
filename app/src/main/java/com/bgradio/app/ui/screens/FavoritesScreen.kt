package com.bgradio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.Station
import com.bgradio.app.player.PlayerUiState
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.StationCard
import com.bgradio.app.ui.components.playbackFor

/** Catalog lists use one column on phones and more on tablets / landscape. */
internal val StationGridCells = GridCells.Adaptive(minSize = 340.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: MainViewModel, contentPadding: PaddingValues, onOpenPlayer: () -> Unit) {
    val player by vm.playerState.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val recent by vm.recentAll.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            if (tab == 0) "Любими" else "Последно слушани",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        )
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            listOf("Любими (${favorites.size})", "Последно слушани").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = tab == i,
                    onClick = { tab = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(label, maxLines = 1) },
                )
            }
        }

        if (tab == 0) {
            if (favorites.isEmpty()) {
                EmptyState(
                    Icons.Rounded.FavoriteBorder,
                    "Все още нямате добавени любими радиостанции.",
                    "Натиснете ❤ до радиостанция, за да я добавите тук.",
                )
            } else {
                StationGrid(favorites, player, favoriteIds, contentPadding, vm, onOpenPlayer)
            }
        } else {
            if (recent.isEmpty()) {
                EmptyState(
                    Icons.Rounded.History,
                    "Все още не сте слушали радио.",
                    "Последните 10 станции, които слушате, ще се появят тук.",
                )
            } else {
                StationGrid(recent, player, favoriteIds, contentPadding, vm, onOpenPlayer)
            }
        }
    }
}

@Composable
internal fun StationGrid(
    stations: List<Station>,
    player: PlayerUiState,
    favoriteIds: Set<String>,
    contentPadding: PaddingValues,
    vm: MainViewModel,
    onOpenPlayer: () -> Unit,
    state: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
) {
    LazyVerticalGrid(
        columns = StationGridCells,
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(stations, key = { it.id }) { s ->
            StationCard(
                station = s,
                playback = player.playbackFor(s),
                isFavorite = s.id in favoriteIds,
                onPlayToggle = { vm.togglePlay(s, stations) },
                onFavorite = { vm.toggleFavorite(s.id) },
                onOpen = onOpenPlayer,
                modifier = Modifier.animateItem(),
                issue = player.issues[s.id],
                onLongPress = { vm.openStationMenu(s) },
            )
        }
    }
}
