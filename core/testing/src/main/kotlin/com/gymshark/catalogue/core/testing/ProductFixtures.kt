package com.gymshark.catalogue.core.testing

import com.gymshark.catalogue.core.model.Label
import com.gymshark.catalogue.core.model.Money
import com.gymshark.catalogue.core.model.Product
import com.gymshark.catalogue.core.model.ProductMedia
import com.gymshark.catalogue.core.model.ProductSize

private const val DEFAULT_PRICE_MAJOR_UNITS = 50L
private const val DEFAULT_MEDIA_WIDTH = 800
private const val DEFAULT_MEDIA_HEIGHT = 1000
private const val DEFAULT_INVENTORY_QUANTITY = 5

/**
 * Sensible defaults so each test names only the field it cares about, e.g.
 * `productFixture(labels = listOf(Label.GoingFast), compareAtPrice = null)`
 * (docs/ARCHITECTURE.md §9.0). One parameter per domain field is the point of this pattern —
 * suppressed rather than restructured into a builder, which would defeat the "name only what
 * you care about" readability this exists for.
 */
@Suppress("LongParameterList")
public fun productFixture(
    id: String = "fixture-product-1",
    title: String = "Fixture Leggings",
    colour: String = "Black",
    type: String? = "Leggings",
    fit: String? = null,
    price: Money = Money.fromMajorUnits(DEFAULT_PRICE_MAJOR_UNITS),
    compareAtPrice: Money? = null,
    discountPercentage: Int? = null,
    labels: List<Label> = emptyList(),
    featuredMedia: ProductMedia? = mediaFixture(),
    media: List<ProductMedia> = listOf(mediaFixture()),
    availableSizes: List<ProductSize> = listOf(sizeFixture()),
): Product =
    Product(
        id = id,
        title = title,
        colour = colour,
        type = type,
        fit = fit,
        price = price,
        compareAtPrice = compareAtPrice,
        discountPercentage = discountPercentage,
        labels = labels,
        featuredMedia = featuredMedia,
        media = media,
        availableSizes = availableSizes,
    )

public fun mediaFixture(
    url: String = "https://cdn.develop.gymshark.com/fixture-image.jpg",
    alt: String? = null,
    width: Int? = DEFAULT_MEDIA_WIDTH,
    height: Int? = DEFAULT_MEDIA_HEIGHT,
): ProductMedia = ProductMedia(url = url, alt = alt, width = width, height = height)

public fun sizeFixture(
    size: String = "m",
    inStock: Boolean = true,
    inventoryQuantity: Int = DEFAULT_INVENTORY_QUANTITY,
): ProductSize = ProductSize(size = size, inStock = inStock, inventoryQuantity = inventoryQuantity)
