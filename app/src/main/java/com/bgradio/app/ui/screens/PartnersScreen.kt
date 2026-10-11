package com.bgradio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.partners.PartnerTier
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.components.EmptyState
import com.bgradio.app.ui.components.PartnerCard
import com.bgradio.app.ui.theme.Dimens

/** "Партньори на DOMINIX" — the partner section promised in the advertising packages. */
@Composable
fun PartnersScreen(vm: MainViewModel, contentPadding: PaddingValues, onBack: () -> Unit, onAdvertise: () -> Unit) {
    val partners by vm.partners.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Text("Партньори", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
        }
        if (partners.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                EmptyState(
                    Icons.Rounded.Handshake,
                    "Все още няма партньори",
                    "Бизнесите, които подкрепят DOMINIX, ще бъдат представени тук.",
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onAdvertise,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
                ) { Text("Станете партньор — Рекламирай в DOMINIX") }
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(
                start = Dimens.ListPadding, end = Dimens.ListPadding, top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Бизнеси, които подкрепят развитието на DOMINIX. Натиснете, за да отворите сайта им в браузъра.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            val premium = partners.filter { it.tier == PartnerTier.PREMIUM }
            val basic = partners.filter { it.tier == PartnerTier.BASIC }
            if (premium.isNotEmpty()) {
                items(premium, key = { it.id }) { PartnerCard(it) }
            }
            if (basic.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Партньори", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
                }
                items(basic, key = { it.id }) { PartnerCard(it) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedButton(onClick = onAdvertise, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Станете партньор — Рекламирай в DOMINIX")
                }
            }
        }
    }
}
