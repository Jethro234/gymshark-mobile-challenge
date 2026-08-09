package com.gymshark.catalogue.feature.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymshark.catalogue.core.data.ProductRepository
import com.gymshark.catalogue.core.data.toErrorCause
import com.gymshark.catalogue.core.model.Product
import com.gymshark.catalogue.core.model.ProductMedia
import com.gymshark.catalogue.core.model.ProductSize
import com.gymshark.catalogue.core.model.merchandisingBadge
import com.gymshark.catalogue.core.model.sustainabilityChips
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val SELECTED_SIZE_KEY = "selectedSize"

/**
 * Resolves the product identified by [productId] and exposes the detail screen's state
 * (docs/ARCHITECTURE.md §5, §11.2). The selected size lives in [SavedStateHandle], not
 * `remember` — it is user intent, and losing it on a low-memory kill would be exactly the
 * small breakage that makes an app feel cheap. Everything else (loaded product, loading,
 * error) lives in the ViewModel and is lost on process death, which is correct: it is
 * re-derived from a repository refetch.
 */
@HiltViewModel(assistedFactory = ProductDetailViewModel.Factory::class)
class ProductDetailViewModel
    @AssistedInject
    constructor(
        @Assisted private val productId: String,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ProductRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(productId: String): ProductDetailViewModel
        }

        private val internalState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
        private val selectedSize = savedStateHandle.getStateFlow<String?>(SELECTED_SIZE_KEY, null)

        val uiState: StateFlow<ProductDetailUiState> =
            combine(internalState, selectedSize) { state, size ->
                if (state is ProductDetailUiState.Content) state.copy(selectedSize = size) else state
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ProductDetailUiState.Loading,
            )

        init {
            load()
        }

        fun retry() {
            internalState.value = ProductDetailUiState.Loading
            load()
        }

        /**
         * The composable only calls this for in-stock sizes — `GsSizeChip` already refuses
         * the tap otherwise.
         */
        fun selectSize(size: String) {
            savedStateHandle[SELECTED_SIZE_KEY] = size
        }

        private fun load() {
            viewModelScope.launch {
                internalState.value =
                    repository.getProduct(productId).fold(
                        onSuccess = { product ->
                            ProductDetailUiState.Content(product.toUiModel(), selectedSize = null)
                        },
                        onFailure = { throwable -> ProductDetailUiState.Error(throwable.toErrorCause()) },
                    )
            }
        }
    }

private fun Product.toUiModel(): ProductDetailUiModel {
    val badge = labels.merchandisingBadge
    return ProductDetailUiModel(
        title = title,
        subtitle = toSubtitle(),
        price = price.format(DISPLAY_LOCALE),
        compareAtPrice = if (isDiscounted) compareAtPrice?.format(DISPLAY_LOCALE) else null,
        media = media.map { it.toUiModel() }.toImmutableList(),
        badgeText = badge?.toBadgeText(),
        badgeTier = badge?.toBadgeTier(),
        sizes = availableSizes.map { it.toUiModel() }.toImmutableList(),
        materialChipTexts = labels.sustainabilityChips.map { it.toMaterialChipText() }.toImmutableList(),
        heading = heading,
        bodyHtml = bodyHtml,
    )
}

private fun Product.toSubtitle(): String = listOfNotNull(colour, type, fit?.toDisplayTitleCase()).joinToString(" · ")

private fun ProductMedia.toUiModel(): ProductMediaUiModel =
    ProductMediaUiModel(url = url, alt = alt, width = width, height = height)

private fun ProductSize.toUiModel(): ProductSizeUiModel = ProductSizeUiModel(size = size, inStock = inStock)
