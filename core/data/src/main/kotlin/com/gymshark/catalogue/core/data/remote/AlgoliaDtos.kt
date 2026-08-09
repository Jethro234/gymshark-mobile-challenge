package com.gymshark.catalogue.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Algolia search response's only top-level key — no `nbHits`/`page`/`hitsPerPage` despite
 * the "search response" framing (docs/ARCHITECTURE.md §2). `ignoreUnknownKeys` (configured on
 * the [kotlinx.serialization.json.Json] instance, not here) tolerates whatever else Algolia
 * sends that this app doesn't model.
 */
@Serializable
internal data class AlgoliaEnvelopeDto(
    val hits: List<ProductDto> = emptyList(),
)

/**
 * One Algolia hit — one product colourway. Fields not consumed anywhere in the app (`handle`,
 * `sku`, `gender`, product-level `inStock`, `sizeInStock`) are intentionally left unmodelled;
 * `ignoreUnknownKeys` discards them rather than failing to parse.
 *
 * `id` is Shopify's numeric product id — too large for `Int` (docs/ARCHITECTURE.md §2 traps).
 * `objectID` is Algolia's own string identifier and is what the domain model actually uses,
 * since it is guaranteed unique per hit while `id` is shared across a product's own variants
 * in some Shopify catalogues (verified unique in the committed payload, but `objectID` is the
 * one the endpoint's own indexing already guarantees this for).
 */
@Serializable
internal data class ProductDto(
    @SerialName("objectID") val objectId: String,
    val title: String,
    val description: String = "",
    val colour: String,
    val type: String? = null,
    val fit: String? = null,
    val price: Long,
    val compareAtPrice: Long? = null,
    val discountPercentage: Int? = null,
    val labels: List<String>? = null,
    val featuredMedia: MediaDto? = null,
    val media: List<MediaDto> = emptyList(),
    val availableSizes: List<SizeDto> = emptyList(),
)

@Serializable
internal data class MediaDto(
    val src: String,
    val alt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
internal data class SizeDto(
    val size: String,
    val inStock: Boolean,
    val inventoryQuantity: Int = 0,
)
