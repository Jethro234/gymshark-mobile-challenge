package com.gymshark.catalogue.core.model

/**
 * A single product colourway, exactly as one Algolia hit represents it. The payload's sixty
 * hits are twenty-one distinct products across sixty colourways, shipped flat — see
 * docs/ARCHITECTURE.md §2.
 */
public data class Product(
    public val id: String,
    public val title: String,
    public val colour: String,
    public val type: String?,
    public val fit: String?,
    public val price: Money,
    public val compareAtPrice: Money?,
    public val discountPercentage: Int?,
    public val labels: List<Label>,
    public val featuredMedia: ProductMedia?,
    public val media: List<ProductMedia>,
    public val availableSizes: List<ProductSize>,
) {
    /**
     * On sale only when [compareAtPrice] is present and strictly greater than [price].
     * [discountPercentage] is never recomputed from the two prices — see docs/ARCHITECTURE.md §7.
     */
    public val isDiscounted: Boolean
        get() = compareAtPrice != null && compareAtPrice > price
}

public data class ProductMedia(
    public val url: String,
    public val alt: String?,
    public val width: Int?,
    public val height: Int?,
)

public data class ProductSize(
    public val size: String,
    public val inStock: Boolean,
    public val inventoryQuantity: Int,
)

/**
 * Normalises the payload's multi-value colour strings — which use two different separators,
 * `/` and ` | ` — into one consistent, dot-separated presentation. Pure function, unit-tested
 * against both real separators.
 */
public fun normaliseColour(raw: String): String =
    raw
        .split("/", "|")
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString(SEPARATOR)

private const val SEPARATOR: String = " · "
