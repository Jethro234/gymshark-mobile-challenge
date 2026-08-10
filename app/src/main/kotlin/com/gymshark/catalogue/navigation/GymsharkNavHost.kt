package com.gymshark.catalogue.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
 *
 * `SharedTransitionLayout` wraps `NavDisplay` — not the other way round, and no
 * `sharedTransitionScope` passed to `NavDisplay` itself — because this app is single-pane
 * (`ARCHITECTURE.md` §11: no adaptive layout), so there's only ever one `Scene` on screen at
 * a time. That's the shape `androidx.navigation3.ui.samples.SceneNavSharedElementSample`
 * uses; `NavDisplay`'s own `sharedTransitionScope` parameter exists for coordinating
 * `sharedBounds` continuity across simultaneously-rendered `Scene`s, which doesn't apply here.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GymsharkNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppRoute.ProductList)

    SharedTransitionLayout {
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
                            onProductClick = { productId, imageUrl, imageWidth, imageHeight ->
                                backStack.add(
                                    AppRoute.ProductDetail(
                                        productId = productId,
                                        imageUrl = imageUrl,
                                        imageWidth = imageWidth,
                                        imageHeight = imageHeight,
                                    ),
                                )
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                        )
                    }
                    entry<AppRoute.ProductDetail> { route ->
                        ProductDetailScreen(
                            productId = route.productId,
                            initialImageUrl = route.imageUrl,
                            initialImageWidth = route.imageWidth,
                            initialImageHeight = route.imageHeight,
                            onBackClick = { backStack.removeLastOrNull() },
                            onProductNotFoundBackToList = { backStack.removeLastOrNull() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                        )
                    }
                },
            modifier = modifier,
        )
    }
}
