package com.gymshark.catalogue.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe route keys (docs/ARCHITECTURE.md §11). `@Serializable` + [NavKey] is what lets
 * `rememberNavBackStack` persist the back stack across process death — route arguments are
 * one of the three things (docs/ARCHITECTURE.md §11.2) that must survive a low-memory kill.
 * [ProductDetail] carries only the product identifier — never the product itself, its media
 * list or its description — so nothing large is ever serialised into the saved state bundle.
 */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object ProductList : AppRoute

    @Serializable
    data class ProductDetail(
        val productId: String,
    ) : AppRoute
}
