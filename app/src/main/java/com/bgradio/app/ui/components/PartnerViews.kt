package com.bgradio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bgradio.app.data.partners.Partner
import com.bgradio.app.data.partners.PartnerTier
import com.bgradio.app.ui.theme.Dimens

/**
 * One partner. Premium: logo + name + link, larger. Basic: name + link.
 * Always labelled with the destination domain, opens the partner's https site in the browser.
 */
@Composable
fun PartnerCard(partner: Partner, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val premium = partner.tier == PartnerTier.PREMIUM
    Surface(
        onClick = { openUrl(context, partner.url) },
        shape = Dimens.CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (premium) 1.5.dp else 1.dp,
            if (premium) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = if (premium) 14.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (premium) {
                PartnerLogo(partner, 52)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    partner.name,
                    style = if (premium) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (premium) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    partner.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Отвори сайта на ${partner.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Partner logo on a light tile (logos are usually made for white backgrounds); initials if it fails. */
@Composable
private fun PartnerLogo(partner: Partner, sizeDp: Int) {
    var failed by remember(partner.logoUrl) { mutableStateOf(partner.logoUrl == null) }
    Box(
        Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(Dimens.RadiusSmall))
            .background(if (failed) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF7F7FA)),
        contentAlignment = Alignment.Center,
    ) {
        if (failed) {
            Text(
                partner.name.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.4f).sp,
            )
        } else {
            AsyncImage(
                model = partner.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        }
    }
}

/** Discreet, clearly labelled block at the bottom of the home screen. Hidden when there are no partners. */
@Composable
fun PartnersSection(partners: List<Partner>, onOpenAll: () -> Unit, modifier: Modifier = Modifier) {
    if (partners.isEmpty()) return
    Column(modifier) {
        SectionHeader("Партньори на DOMINIX", action = "Всички", onAction = onOpenAll)
        Column(
            Modifier.padding(horizontal = Dimens.ListPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Premium partners first; keep the home screen short.
            partners.take(4).forEach { PartnerCard(it) }
        }
    }
}

