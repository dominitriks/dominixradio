package com.bgradio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.StationCard
import com.bgradio.app.ui.components.playbackFor

@Composable
fun FavoritesScreen(vm: MainViewModel, contentPadding: PaddingValues, onOpenPlayer: () -> Unit) {
    val player by vm.playerState.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Любими",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        if (favorites.isEmpty()) {
            EmptyState(
                Icons.Rounded.FavoriteBorder,
                "Все още нямате добавени любими радиостанции.",
                "Натиснете ❤ до радиостанция, за да я добавите тук.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(favorites, key = { it.id }) { s ->
                    StationCard(
                        station = s,
                        playback = player.playbackFor(s),
                        isFavorite = true,
                        onPlayToggle = { vm.togglePlay(s, favorites) },
                        onFavorite = { vm.toggleFavorite(s.id) },
                        onOpen = onOpenPlayer,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
