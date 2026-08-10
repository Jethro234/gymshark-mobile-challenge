package com.gymshark.catalogue.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe route keys (docs/ARCHITECTURE.md §11). `@Serializable` + [NavKey] is what lets
 * `rememberNavBackStack` persist the back stack across process death — route arguments are
 * one of the three things (docs/ARCHITECTURE.md §11.2) that must survive a low-memory kill.
 * [ProductDetail] carries the product identifier plus the grid card's own thumbnail — never
 * the product itself, its media list or its description — so nothing large is ever serialised
 * into the saved state bundle.
 *
 * The thumbnail fields exist for the shared-element transition (`docs/DESIGN.md` §6): the
 * detail screen renders `Loading` on its first frame, so without them the expanding hero has
 * no image to draw and the transition plays out over a blank box. They are the same Shopify
 * asset the hero resolves to once loaded — the list reads `featuredMedia`, the detail reads
 * `media[0]`, and `ProductMapper` documents those as identical in every real product — so
 * Coil serves the expansion straight from its memory cache and nothing visibly swaps when the
 * fetch lands. All three are nullable: a product with no image is a real case.
 */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object ProductList : AppRoute

    @Serializable
    data class ProductDetail(
        val productId: String,
        val imageUrl: String? = null,
        val imageWidth: Int? = null,
        val imageHeight: Int? = null,
    ) : AppRoute
}
