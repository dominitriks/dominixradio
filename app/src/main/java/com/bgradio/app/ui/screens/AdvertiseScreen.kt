package com.bgradio.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bgradio.app.data.model.AdPackage
import com.bgradio.app.data.model.Advertising
import com.bgradio.app.share.StationShare
import com.bgradio.app.ui.components.copy
import com.bgradio.app.ui.theme.Dimens

/** "Рекламирай в DOMINIX" — information for potential advertisers. No payments, no tracking. */
@Composable
fun AdvertiseScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Text(
                Advertising.TITLE,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }

        LazyVerticalGrid(
            // One column on phones, packages side by side on tablets / landscape.
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(
                start = Dimens.ListPadding,
                end = Dimens.ListPadding,
                top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    Advertising.INTRO,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(Advertising.PACKAGES, key = { it.name }) { pkg -> PackageCard(pkg) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    Advertising.NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) { ContactCard() }
        }
    }
}

@Composable
private fun PackageCard(pkg: AdPackage) {
    val index = Advertising.PACKAGES.indexOf(pkg)
    // Distinguished by border colour AND a text badge (not colour alone).
    val (accent, badge) = when (index) {
        1 -> MaterialTheme.colorScheme.primary to "С лого"
        2 -> MaterialTheme.colorScheme.secondary to "12 месеца"
        else -> MaterialTheme.colorScheme.outlineVariant to null
    }
    Card(
        shape = Dimens.PanelShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(if (badge != null) 1.5.dp else 1.dp, accent.copy(alpha = if (badge != null) 0.7f else 1f)),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pkg.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(pkg.price, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            pkg.features.forEach { feature ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (badge != null) accent else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ContactCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // message, isError
    Card(
        shape = Dimens.PanelShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Интересувате ли се?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Пишете ни — ще отговорим с подробности за избрания пакет.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val mail = Intent(Intent.ACTION_SENDTO, Uri.parse(Advertising.mailtoUri())).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(Advertising.EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, Advertising.SUBJECT)
                        putExtra(Intent.EXTRA_TEXT, Advertising.BODY)
                    }
                    status = if (StationShare.start(context, mail)) {
                        null
                    } else {
                        "Не е намерено приложение за имейл. Копирайте адреса по-долу и ни пишете от удобно за вас място." to true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Rounded.Email, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Свържи се с нас", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Имейл", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectionContainer {
                        Text(Advertising.EMAIL, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                TextButton(onClick = {
                    status = if (copy(context, Advertising.EMAIL, "Имейл за реклама в DOMINIX")) {
                        "Адресът е копиран." to false
                    } else {
                        "Копирането не успя." to true
                    }
                }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Копирай")
                }
            }
            status?.let { (msg, isError) ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

