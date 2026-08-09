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
public data class AlgoliaEnvelopeDto(
    public val hits: List<ProductDto> = emptyList(),
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
public data class ProductDto(
    @SerialName("objectID") public val objectId: String,
    public val title: String,
    public val description: String = "",
    public val colour: String,
    public val type: String? = null,
    public val fit: String? = null,
    public val price: Long,
    public val compareAtPrice: Long? = null,
    public val discountPercentage: Int? = null,
    public val labels: List<String>? = null,
    public val featuredMedia: MediaDto? = null,
    public val media: List<MediaDto> = emptyList(),
    public val availableSizes: List<SizeDto> = emptyList(),
)

@Serializable
public data class MediaDto(
    public val src: String,
    public val alt: String? = null,
    public val width: Int? = null,
    public val height: Int? = null,
)

@Serializable
public data class SizeDto(
    public val size: String,
    public val inStock: Boolean,
    public val inventoryQuantity: Int = 0,
)
