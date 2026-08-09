package com.gymshark.catalogue.feature.products

import androidx.compose.runtime.Immutable
import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.model.ErrorCause
import kotlinx.collections.immutable.ImmutableList

/**
 * A closed set of mutually exclusive detail-screen states (docs/ARCHITECTURE.md §5), matching
 * the list screen's shape. [Content] carries [selectedSize] alongside the loaded product,
 * since selection only means anything once the product's sizes are known.
 */
@Immutable
sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState

    data class Content(
        val product: ProductDetailUiModel,
        val selectedSize: String?,
    ) : ProductDetailUiState

    data class Error(
        val cause: ErrorCause,
    ) : ProductDetailUiState
}

/**
 * Everything the detail screen needs, precomputed once in the ViewModel: currency formatting,
 * the colourway/type/fit subtitle, and label-to-text mapping are all done here, not during
 * composition. [heading]/[bodyHtml] are plain strings — the HTML-to-styled-text conversion
 * itself happens in the composable, memoised against [bodyHtml] (`docs/ARCHITECTURE.md` §6).
 */
@Immutable
data class ProductDetailUiModel(
    val title: String,
    val subtitle: String,
    val price: String,
    val compareAtPrice: String?,
    val media: ImmutableList<ProductMediaUiModel>,
    val badgeText: String?,
    val badgeTier: GsLabelTier?,
    val sizes: ImmutableList<ProductSizeUiModel>,
    val materialChipTexts: ImmutableList<String>,
    val heading: String?,
    val bodyHtml: String,
)

@Immutable
data class ProductMediaUiModel(
    val url: String,
    val alt: String?,
    val width: Int?,
    val height: Int?,
)

@Immutable
data class ProductSizeUiModel(
    val size: String,
    val inStock: Boolean,
)
