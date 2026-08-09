package com.gymshark.catalogue.feature.products

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gymshark.catalogue.core.designsystem.component.GsAsyncImage
import com.gymshark.catalogue.core.designsystem.component.GsMaterialChip
import com.gymshark.catalogue.core.designsystem.component.GsSizeChip
import com.gymshark.catalogue.core.designsystem.component.GsSizeChipState
import com.gymshark.catalogue.core.designsystem.theme.GsTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PriceRow(
    price: String,
    compareAtPrice: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GsTheme.spacing.space8)) {
        Text(
            text = price,
            style = GsTheme.typography.price,
            color = if (compareAtPrice != null) GsTheme.colorScheme.sale else GsTheme.colorScheme.textPrimary,
        )
        if (compareAtPrice != null) {
            Text(
                text = compareAtPrice,
                style = GsTheme.typography.price,
                color = GsTheme.colorScheme.textMuted,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

private const val THUMBNAIL_SIZE_DP = 64
private const val THUMBNAIL_SELECTION_BORDER_DP = 1.5f

@Composable
internal fun ThumbnailStrip(
    media: ImmutableList<ProductMediaUiModel>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    fallbackContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GsTheme.spacing.space8),
    ) {
        media.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Box(
                modifier =
                    Modifier
                        .size(THUMBNAIL_SIZE_DP.dp)
                        .clip(GsTheme.shapes.thumbnail)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    THUMBNAIL_SELECTION_BORDER_DP.dp,
                                    GsTheme.colorScheme.textPrimary,
                                    GsTheme.shapes.thumbnail,
                                )
                            } else {
                                Modifier
                            },
                        ).clickable { onSelect(index) },
            ) {
                GsAsyncImage(
                    model = item.url,
                    contentDescription = item.alt ?: fallbackContentDescription,
                    contentWidth = item.width,
                    contentHeight = item.height,
                    shape = GsTheme.shapes.thumbnail,
                    showErrorLabel = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
internal fun SizeChipRow(
    sizes: ImmutableList<ProductSizeUiModel>,
    selectedSize: String?,
    onSelectSize: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GsTheme.spacing.space8),
    ) {
        sizes.forEach { size ->
            val state =
                when {
                    !size.inStock -> GsSizeChipState.OutOfStock
                    size.size == selectedSize -> GsSizeChipState.Selected
                    else -> GsSizeChipState.Available
                }
            GsSizeChip(
                text = size.size,
                state = state,
                onClick = { onSelectSize(size.size) },
            )
        }
    }
}

@Composable
internal fun MaterialChipRow(
    texts: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GsTheme.spacing.space8)) {
        texts.forEach { text -> GsMaterialChip(text = text) }
    }
}
