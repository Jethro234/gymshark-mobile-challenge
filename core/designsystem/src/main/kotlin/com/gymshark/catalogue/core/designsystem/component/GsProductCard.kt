package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymshark.catalogue.core.designsystem.theme.GsTheme

private val BADGE_INSET = 10.dp
private val TITLE_TOP_SPACING = 11.dp
private val COLOURWAY_TOP_SPACING = 3.dp
private val PRICE_TOP_SPACING = 7.dp
private val PRICE_ROW_SPACING = 6.dp
private const val TITLE_MAX_LINES = 2
private const val COLOURWAY_MAX_LINES = 1

/**
 * A grid product card: image (`card` radius, ratio reserved from the payload's own
 * dimensions) → title → colourway → price. Intrinsically sized — no fixed height — so it
 * reflows rather than clips at large font scales (`docs/DESIGN.md` §2, §4). No border, no
 * background, no shadow: the photograph is the card.
 */
@Composable
public fun GsProductCard(
    imageUrl: String?,
    imageAlt: String?,
    imageWidth: Int?,
    imageHeight: Int?,
    title: String,
    colourway: String,
    price: String,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    imageMemoryCacheKey: String? = null,
    compareAtPrice: String? = null,
    badgeText: String? = null,
    badgeTier: GsLabelTier = GsLabelTier.Informational,
    onClick: () -> Unit = {},
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            GsAsyncImage(
                model = imageUrl,
                contentDescription = imageAlt ?: title,
                contentWidth = imageWidth,
                contentHeight = imageHeight,
                shape = GsTheme.shapes.card,
                // Written *and* read back under the same key: writing lets the detail hero
                // borrow this bitmap, reading lets the card redraw instantly when the list is
                // recomposed from scratch on back navigation, rather than restarting at a
                // shimmer while the shrinking hero lands on it.
                memoryCacheKey = imageMemoryCacheKey,
                placeholderMemoryCacheKey = imageMemoryCacheKey,
                modifier = Modifier.fillMaxWidth().then(imageModifier),
            )
            if (badgeText != null) {
                GsLabelBadge(
                    text = badgeText,
                    tier = badgeTier,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(BADGE_INSET),
                )
            }
        }
        Text(
            text = title,
            style = GsTheme.typography.titleCard,
            color = GsTheme.colorScheme.textPrimary,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(top = TITLE_TOP_SPACING)),
        )
        Text(
            text = colourway,
            style = GsTheme.typography.label,
            color = GsTheme.colorScheme.textMuted,
            maxLines = COLOURWAY_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(top = COLOURWAY_TOP_SPACING)),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(PRICE_ROW_SPACING),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(top = PRICE_TOP_SPACING)),
        ) {
            Text(
                text = price,
                style = GsTheme.typography.priceCard,
                color = if (compareAtPrice != null) GsTheme.colorScheme.sale else GsTheme.colorScheme.textPrimary,
            )
            if (compareAtPrice != null) {
                Text(
                    text = compareAtPrice,
                    style = GsTheme.typography.priceCard,
                    color = GsTheme.colorScheme.textMuted,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
    }
}
