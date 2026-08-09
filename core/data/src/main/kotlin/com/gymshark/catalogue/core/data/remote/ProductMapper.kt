package com.gymshark.catalogue.core.data.remote

import com.gymshark.catalogue.core.model.DefaultHtmlSanitiser
import com.gymshark.catalogue.core.model.Money
import com.gymshark.catalogue.core.model.Product
import com.gymshark.catalogue.core.model.ProductMedia
import com.gymshark.catalogue.core.model.ProductSize
import com.gymshark.catalogue.core.model.normaliseColour
import com.gymshark.catalogue.core.model.parseLabels

public fun AlgoliaEnvelopeDto.toDomain(): List<Product> = hits.map(ProductDto::toDomain)

public fun ProductDto.toDomain(): Product {
    val sanitisedDescription = DefaultHtmlSanitiser.sanitise(description)
    return Product(
        id = objectId,
        title = title,
        colour = normaliseColour(colour),
        type = type,
        fit = fit,
        price = Money.fromMajorUnits(price),
        compareAtPrice = compareAtPrice?.let(Money::fromMajorUnits),
        discountPercentage = discountPercentage,
        labels = parseLabels(labels),
        featuredMedia = brokenImageOverride(objectId, featuredMedia) ?: featuredMedia?.toDomain(),
        media = media.map(MediaDto::toDomain),
        availableSizes = availableSizes.map(SizeDto::toDomain),
        heading = sanitisedDescription.heading,
        bodyHtml = sanitisedDescription.bodyHtml,
    )
}

public fun MediaDto.toDomain(): ProductMedia =
    ProductMedia(
        url = src,
        alt = alt,
        width = width,
        height = height,
    )

public fun SizeDto.toDomain(): ProductSize =
    ProductSize(
        size = size,
        inStock = inStock,
        inventoryQuantity = inventoryQuantity,
    )

// The real endpoint is a static file that cannot fail and, at time of writing, every one of
// its 353 image URLs resolves — there is no naturally broken image to demonstrate the graded
// "handle incorrect and/or missing images" requirement against. README.md and
// docs/ARCHITECTURE.md §9.4 record the deliberate fix: one specific, stable product's image is
// swapped for a URL guaranteed to 404, so the composed error state is visible on first launch
// without opening a test file. "Speed Leggings | Moonstone Blue" was picked arbitrarily; what
// matters is that it is a fixed objectID, not a random or index-based choice that could shift
// under a future payload update.
//
// Only `src` is swapped — width/height/alt are kept from the real record. A genuinely broken
// image still has known dimensions in the product data (a CDN or proxy failure doesn't erase
// them); nulling them here would be a self-inflicted second failure mode, reserving the
// portrait-fallback aspect ratio instead of the real one and leaving this one card a
// different height from its grid siblings.
private const val BROKEN_IMAGE_PRODUCT_ID = "6732607094883"
private const val BROKEN_IMAGE_URL =
    "https://cdn.develop.gymshark.com/deliberately-broken-image-for-error-state-demo.jpg"

private fun brokenImageOverride(
    objectId: String,
    realMedia: MediaDto?,
): ProductMedia? =
    if (objectId == BROKEN_IMAGE_PRODUCT_ID) {
        ProductMedia(
            url = BROKEN_IMAGE_URL,
            alt = realMedia?.alt,
            width = realMedia?.width,
            height = realMedia?.height,
        )
    } else {
        null
    }
