package com.bgradio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.CategoryChips
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.SearchField
import com.bgradio.app.ui.components.StationCard
import com.bgradio.app.ui.components.playbackFor

@Composable
fun StationsScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    focusSearch: Boolean,
    onOpenPlayer: () -> Unit,
) {
    val player by vm.playerState.collectAsStateWithLifecycle()
    val stations by vm.filtered.collectAsStateWithLifecycle()
    val all by vm.stations.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val genre by vm.selectedGenre.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) runCatching { focusRequester.requestFocus() }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(genre, query) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Радиостанции",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
        Text(
            if (all.isEmpty()) "" else "${stations.size} от ${all.size} станции",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))
        SearchField(
            query = query,
            onQueryChange = vm::setQuery,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.height(12.dp))
        CategoryChips(genres, genre, vm::selectGenre)
        Spacer(Modifier.height(8.dp))

        if (stations.isEmpty() && all.isNotEmpty()) {
            EmptyState(
                Icons.Rounded.SearchOff,
                "Няма намерени радиостанции",
                "Опитай с друго име, жанр или град — например „рок“ или „София“.",
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(stations, key = { it.id }) { s ->
                    StationCard(
                        station = s,
                        playback = player.playbackFor(s),
                        isFavorite = s.id in favorites,
                        onPlayToggle = { vm.togglePlay(s, stations) },
                        onFavorite = { vm.toggleFavorite(s.id) },
                        onOpen = onOpenPlayer,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
