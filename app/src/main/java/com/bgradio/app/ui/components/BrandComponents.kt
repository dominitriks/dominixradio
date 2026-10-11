package com.bgradio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bgradio.app.ui.theme.Brand
import com.bgradio.app.ui.theme.DominixTheme
import com.bgradio.app.ui.theme.SloganStyle

/**
 * The DOMINIX slogan as part of the brand system: serif italic in the accent colour with a thin
 * purple→cyan rule. Wraps on small screens instead of being cut.
 */
@Composable
fun BrandSlogan(modifier: Modifier = Modifier, centered: Boolean = false) {
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) { contentDescription = Brand.SLOGAN },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!centered) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, DominixTheme.colors.playing))),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            Brand.SLOGAN,
            style = SloganStyle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Thin Bulgarian tricolour line — the same identity mark as in the app icon. */
@Composable
fun TricolorLine(modifier: Modifier = Modifier, segment: Dp = 16.dp) {
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(Brand.FlagWhite, Brand.FlagGreen, Brand.FlagRed).forEach { c ->
            Box(
                Modifier
                    .width(segment)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c),
            )
        }
    }
}
