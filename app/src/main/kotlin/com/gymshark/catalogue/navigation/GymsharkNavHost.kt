package com.gymshark.catalogue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gymshark.catalogue.feature.products.ProductDetailScreen
import com.gymshark.catalogue.feature.products.ProductListScreen

/**
 * The app's only navigation graph — two routes (docs/ARCHITECTURE.md §11). Both entry
 * decorators are required: `rememberSaveableStateHolderNavEntryDecorator` for `rememberSaveable`
 * state inside each entry (the list screen's grid scroll position), and
 * `rememberViewModelStoreNavEntryDecorator` so each screen's ViewModel is scoped to its
 * `NavEntry` and cleared on pop rather than retained for the Activity's lifetime.
 */
@Composable
fun GymsharkNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppRoute.ProductList)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<AppRoute.ProductList> {
                    ProductListScreen(
                        onProductClick = { productId -> backStack.add(AppRoute.ProductDetail(productId)) },
                    )
                }
                entry<AppRoute.ProductDetail> { route ->
                    ProductDetailScreen(
                        productId = route.productId,
                        onBackClick = { backStack.removeLastOrNull() },
                        onProductNotFoundBackToList = { backStack.removeLastOrNull() },
                    )
                }
            },
        modifier = modifier,
    )
}
