package com.bgradio.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.CategoryChips
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.SearchField
import com.bgradio.app.utils.StationSort

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
    val counts by vm.genreCounts.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val filtersActive = query.isNotBlank() || genre != null

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) runCatching { focusRequester.requestFocus() }
    }
    val gridState = rememberLazyGridState()
    // Jump to the top when the filter changes (not on first composition, to keep scroll on tab return).
    var lastFilter by rememberSaveable { mutableStateOf("$genre|$query|$sort") }
    LaunchedEffect(genre, query, sort) {
        val key = "$genre|$query|$sort"
        if (key != lastFilter) {
            lastFilter = key
            gridState.scrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Радиостанции", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (all.isEmpty()) "" else "${stations.size} от ${all.size} станции · ${sort.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (filtersActive) {
                TextButton(onClick = vm::clearFilters) { Text("Изчисти") }
            }
            SortMenu(sort, vm::setSort)
        }
        Spacer(Modifier.height(12.dp))
        SearchField(
            query = query,
            onQueryChange = vm::setQuery,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.height(12.dp))
        CategoryChips(genres, genre, vm::selectGenre, counts = counts)
        Spacer(Modifier.height(4.dp))

        if (stations.isEmpty() && all.isNotEmpty()) {
            Box(Modifier.fillMaxSize()) {
                EmptyState(
                    Icons.Rounded.SearchOff,
                    "Няма намерени радиостанции",
                    "Опитай с друго име, жанр или град — например „рок“ или „София“.",
                )
                Button(
                    onClick = vm::clearFilters,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = contentPadding.calculateBottomPadding() + 32.dp),
                ) { Text("Изчисти филтрите") }
            }
        } else {
            StationGrid(stations, player, favorites, contentPadding, vm, onOpenPlayer, gridState)
        }
    }
}

@Composable
private fun SortMenu(sort: StationSort, onSort: (StationSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Подреждане: ${sort.label}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            StationSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSort(option)
                        open = false
                    },
                    trailingIcon = { if (option == sort) Icon(Icons.Rounded.Check, null) },
                )
            }
        }
    }
}
