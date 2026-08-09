package com.gymshark.catalogue.feature.products

import androidx.compose.runtime.Immutable
import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.model.ErrorCause
import kotlinx.collections.immutable.ImmutableList

/**
 * A closed set of mutually exclusive list-screen states (docs/ARCHITECTURE.md §5). No object
 * can be simultaneously loading, errored and populated — a flags-based data class would permit
 * that nonsense.
 */
@Immutable
sealed interface ProductListUiState {
    data object Loading : ProductListUiState

    /** [isRefreshing] lives here, not as a sibling state — pull-to-refresh must not blank the list. */
    data class Content(
        val products: ImmutableList<ProductUiModel>,
        val screenTitle: String,
        val countLabel: String,
        val isRefreshing: Boolean = false,
    ) : ProductListUiState

    data object Empty : ProductListUiState

    data class Error(
        val cause: ErrorCause,
    ) : ProductListUiState
}

/**
 * Everything a grid item needs, precomputed once in the ViewModel rather than during
 * composition (`docs/ARCHITECTURE.md` §14): currency formatting and label-to-badge mapping are
 * both done here, so scrolling never repeats that work.
 */
@Immutable
data class ProductUiModel(
    val id: String,
    val imageUrl: String?,
    val imageAlt: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val title: String,
    val colourway: String,
    val price: String,
    val compareAtPrice: String?,
    val badgeText: String?,
    val badgeTier: GsLabelTier?,
)
