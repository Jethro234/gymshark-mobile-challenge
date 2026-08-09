package com.gymshark.catalogue.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymshark.catalogue.core.data.ProductRepository
import com.gymshark.catalogue.core.data.toErrorCause
import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.model.ErrorCause
import com.gymshark.catalogue.core.model.Label
import com.gymshark.catalogue.core.model.Product
import com.gymshark.catalogue.core.model.merchandisingBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Exposes the product grid's state (docs/ARCHITECTURE.md §5). [ProductRepository.getProducts]
 * is a single cache-or-fetch emission, not a hot stream, so [refresh] and [retry] drive new
 * emissions explicitly rather than the ViewModel merely observing an upstream flow.
 */
@HiltViewModel
class ProductListViewModel
    @Inject
    constructor(
        private val repository: ProductRepository,
    ) : ViewModel() {
        private val internalState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)

        val uiState: StateFlow<ProductListUiState> =
            internalState.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ProductListUiState.Loading,
            )

        init {
            load()
        }

        /** Re-attempts a load from an error state — the same request an initial load makes. */
        fun retry() {
            internalState.value = ProductListUiState.Loading
            load()
        }

        /** Keeps whatever is currently on screen visible — and visible again on failure. */
        fun refresh() {
            val current = internalState.value
            if (current is ProductListUiState.Content) {
                internalState.value = current.copy(isRefreshing = true)
            }
            viewModelScope.launch {
                val result = repository.refresh()
                internalState.value =
                    result.fold(
                        onSuccess = { products -> products.toUiState() },
                        onFailure = { throwable -> refreshFailureState(throwable.toErrorCause()) },
                    )
            }
        }

        private fun refreshFailureState(cause: ErrorCause): ProductListUiState {
            val current = internalState.value
            return if (current is ProductListUiState.Content) {
                current.copy(isRefreshing = false)
            } else {
                ProductListUiState.Error(cause)
            }
        }

        private fun load() {
            viewModelScope.launch {
                repository.getProducts().collect { result ->
                    internalState.value =
                        result.fold(
                            onSuccess = { products -> products.toUiState() },
                            onFailure = { throwable -> ProductListUiState.Error(throwable.toErrorCause()) },
                        )
                }
            }
        }
    }

private const val SINGULAR_PRODUCT_COUNT = 1

private fun List<Product>.toUiState(): ProductListUiState =
    if (isEmpty()) {
        ProductListUiState.Empty
    } else {
        ProductListUiState.Content(
            products = toUiModels(),
            screenTitle = toScreenTitle(),
            countLabel = toCountLabel(),
        )
    }

private fun List<Product>.toUiModels(): ImmutableList<ProductUiModel> = map { it.toUiModel() }.toImmutableList()

/** Derived from the payload's own `type` field rather than hardcoded, so it can't drift from what's shown. */
private fun List<Product>.toScreenTitle(): String =
    firstNotNullOfOrNull { it.type }
        ?.lowercase()
        ?.replaceFirstChar(Char::uppercase)
        ?: "Products"

private fun List<Product>.toCountLabel(): String =
    if (size == SINGULAR_PRODUCT_COUNT) "$size product" else "$size products"

private fun Product.toUiModel(): ProductUiModel {
    val badge = labels.merchandisingBadge
    return ProductUiModel(
        id = id,
        imageUrl = featuredMedia?.url,
        imageAlt = featuredMedia?.alt,
        title = title,
        colourway = colour,
        price = price.format(DISPLAY_LOCALE),
        compareAtPrice = if (isDiscounted) compareAtPrice?.format(DISPLAY_LOCALE) else null,
        badgeText = badge?.toBadgeText(),
        badgeTier = badge?.toBadgeTier(),
    )
}

/** GBP, always — the payload has no locale field and the brand this catalogue represents is UK-only. */
private val DISPLAY_LOCALE: Locale = Locale.UK

private fun Label.toBadgeText(): String =
    when (this) {
        Label.GoingFast -> "Going fast"
        Label.LimitedEdition -> "Limited edition"
        Label.New -> "New"
        Label.Popular -> "Popular"
        Label.RecycledNylon, Label.RecycledPolyester ->
            error("Sustainability label reached the merchandising badge slot: $raw")
        is Label.Unknown -> raw.toDisplayTitleCase()
    }

private fun Label.toBadgeTier(): GsLabelTier =
    when (this) {
        Label.GoingFast, Label.LimitedEdition -> GsLabelTier.Urgency
        Label.New, Label.Popular -> GsLabelTier.Informational
        Label.RecycledNylon, Label.RecycledPolyester ->
            error("Sustainability label reached the merchandising badge slot: $raw")
        is Label.Unknown -> GsLabelTier.Unknown
    }

private fun String.toDisplayTitleCase(): String =
    split('-', ' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
