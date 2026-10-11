package com.bgradio.app.ui.screens

import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.ui.semantics.Role
import com.bgradio.app.ui.components.PartnersSection
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bgradio.app.ui.components.BrandSlogan
import com.bgradio.app.ui.components.TricolorLine
import com.bgradio.app.ui.theme.Dimens
import com.bgradio.app.ui.theme.DominixTheme
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
import com.bgradio.app.ui.theme.genreColor

@Composable
fun HomeScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onOpenCategory: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenPartners: () -> Unit = {},
    onOpenAdvertise: () -> Unit = {},
) {
    val partners by vm.partners.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val popular by vm.popular.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val stations by vm.stations.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    ) {
        item(key = "header") { Header(stations.size, onOpenSearch, onOpenStats) }

        item(key = "bgwarning") {
            val bg = rememberBackgroundStatus()
            val context = LocalContext.current
            if (bg.backgroundRestricted) {
                Surface(
                    onClick = { BackgroundStatus.openAppSettings(context) },
                    shape = Dimens.CardShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ListPadding),
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
        if (partners.isNotEmpty()) {
            item(key = "partners") { PartnersSection(partners, onOpenAll = onOpenPartners) }
        }
        item(key = "advertise") { AdvertiseLink(onOpenAdvertise) }
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
    val glowA = DominixTheme.colors.glowPrimary
    val glowB = DominixTheme.colors.glowSecondary
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                // One soft, low-contrast glow — atmosphere, not decoration.
                drawRect(
                    Brush.radialGradient(
                        listOf(glowA.copy(alpha = 0.22f), glowB.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f),
                        radius = size.width * 0.9f,
                    ),
                )
            }
            .statusBarsPadding()
            .padding(start = Dimens.ScreenPadding, end = 8.dp, top = 16.dp, bottom = 4.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 6.dp)) {
                    Text(
                        "DOMINIX",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(8.dp))
                    TricolorLine()
                }
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
            Spacer(Modifier.height(14.dp))
            BrandSlogan(Modifier.padding(end = 12.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                "Български радиостанции на едно място",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = onOpenSearch,
                shape = Dimens.CardShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .heightIn(min = 52.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
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
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
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
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.padding(horizontal = Dimens.ListPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            genres.forEach { g ->
                Surface(
                    onClick = { onOpen(g) },
                    shape = Dimens.CardShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(genreColor(g)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Categories.shortLabel(g), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${counts[g] ?: 0} станции",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (genres.size % 2 == 1) Spacer(Modifier.weight(1f).size(0.dp))
        }
    }
}

/**
 * Quiet footer link for potential advertisers: secondary colours, no frame, no animation — part of
 * the page footer, not a banner. Opens the existing "Рекламирай в DOMINIX" page.
 */
@Composable
private fun AdvertiseLink(onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding)
            .clip(Dimens.CardShape)
            .clickable(role = Role.Button, onClickLabel = "Отвори", onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = muted.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Рекламирай в DOMINIX", style = MaterialTheme.typography.labelLarge, color = muted)
            Text(
                "Възможности за бизнес партньори",
                style = MaterialTheme.typography.labelSmall,
                color = muted.copy(alpha = 0.75f),
            )
        }
    }
}
