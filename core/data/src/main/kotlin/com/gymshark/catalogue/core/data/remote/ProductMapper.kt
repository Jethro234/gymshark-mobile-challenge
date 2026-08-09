package com.gymshark.catalogue.core.data.remote

import com.gymshark.catalogue.core.model.DefaultHtmlSanitiser
import com.gymshark.catalogue.core.model.Money
import com.gymshark.catalogue.core.model.Product
import com.gymshark.catalogue.core.model.ProductMedia
import com.gymshark.catalogue.core.model.ProductSize
import com.gymshark.catalogue.core.model.normaliseColour
import com.gymshark.catalogue.core.model.parseLabels

internal fun AlgoliaEnvelopeDto.toDomain(): List<Product> = hits.map(ProductDto::toDomain)

internal fun ProductDto.toDomain(): Product {
    val sanitisedDescription = DefaultHtmlSanitiser.sanitise(description)
    val isBrokenImageProduct = objectId == BROKEN_IMAGE_PRODUCT_ID
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
        featuredMedia = featuredMedia?.toDomain(brokenImage = isBrokenImageProduct),
        // media[0] is the same Shopify asset as featuredMedia in every real product (same
        // id, same src) — only breaking featuredMedia would leave the detail screen's hero
        // image (read from media[0], not featuredMedia) showing the real photo the moment a
        // reviewer taps in, undoing the whole demonstration.
        media =
            media.mapIndexed { index, dto ->
                dto.toDomain(brokenImage = isBrokenImageProduct && index == 0)
            },
        availableSizes = availableSizes.map(SizeDto::toDomain),
        heading = sanitisedDescription.heading,
        bodyHtml = sanitisedDescription.bodyHtml,
    )
}

internal fun MediaDto.toDomain(brokenImage: Boolean = false): ProductMedia =
    ProductMedia(
        url = if (brokenImage) BROKEN_IMAGE_URL else src,
        alt = alt,
        width = width,
        height = height,
    )

internal fun SizeDto.toDomain(): ProductSize =
    ProductSize(
        size = size,
        inStock = inStock,
        inventoryQuantity = inventoryQuantity,
    )

// The real endpoint is a static file that cannot fail, and the payload's own image URLs
// are Shopify CDN links outside this project's control — whether any of the 353 of them
// currently 404 is incidental, not a stable property of the dataset. At the time this
// override was written, none did; as of 2026-08-10, three genuinely do (Training Leggings
// | Navy, Flex High Waisted Leggings | Charcoal Marl, Flex High Waisted Leggings | Black —
// confirmed with a direct curl, not assumed). That is not something to build the graded
// "handle incorrect and/or missing images" requirement on: Shopify could clean those links
// up at any point, silently removing the only evidence of this requirement being met.
// README.md and docs/ARCHITECTURE.md §9.4 record the deliberate fix instead: one specific,
// stable product's image is swapped for a URL guaranteed to 404, so the composed error
// state is visible on first launch without opening a test file, regardless of what the real
// CDN is doing today. "Speed Leggings | Moonstone Blue" was picked arbitrarily; what matters
// is that it is a fixed objectID, not a random or index-based choice that could shift under
// a future payload update.
//
// Both featuredMedia and media[0] are swapped, not just featuredMedia — in every real
// product they're the same Shopify asset (same id, same src), and the list screen reads
// featuredMedia while the detail screen's hero image reads media[0]. Swapping only the
// former meant the list card showed the error placeholder but tapping into the detail
// screen immediately showed the real photo — the same asset "un-breaking" itself on
// navigation, which looks exactly like a caching bug even though no caching is involved.
//
// Only `src` is swapped — width/height/alt are kept from the real record. A genuinely broken
// image still has known dimensions in the product data (a CDN or proxy failure doesn't erase
// them); nulling them here would be a self-inflicted second failure mode, reserving the
// portrait-fallback aspect ratio instead of the real one and leaving this one card a
// different height from its grid siblings.
private const val BROKEN_IMAGE_PRODUCT_ID = "6732607094883"
private const val BROKEN_IMAGE_URL =
    "https://cdn.develop.gymshark.com/deliberately-broken-image-for-error-state-demo.jpg"
