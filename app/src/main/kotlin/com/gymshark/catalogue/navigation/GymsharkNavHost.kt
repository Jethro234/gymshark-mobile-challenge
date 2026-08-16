package com.gymshark.catalogue.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gymshark.catalogue.feature.products.PRODUCT_DETAIL_EXIT_DURATION_MILLIS
import com.gymshark.catalogue.feature.products.ProductDetailScreen
import com.gymshark.catalogue.feature.products.ProductListScreen

/**
 * Both directions of the pop use one spec, so a gesture-driven back and a button-driven back
 * sequence against the hero transform identically.
 */
private val PRODUCT_DETAIL_POP_TRANSITION =
    ContentTransform(
        fadeIn(tween(PRODUCT_DETAIL_EXIT_DURATION_MILLIS)),
        fadeOut(tween(PRODUCT_DETAIL_EXIT_DURATION_MILLIS)),
    )

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
            // NavDisplay's default pop is a 700ms crossfade, more than twice the hero's own
            // transform — so on the way back the detail content was still fading long after
            // the image had finished shrinking into its grid cell. Popping quickly instead
            // lets HeroBoundsTransform hold the image still until that fade is done and then
            // shrink it (docs/DESIGN.md §6). The predictive spec is overridden for the same
            // reason and because its default scales the whole outgoing scene, which fights
            // the shared element rather than complementing it.
            popTransitionSpec = { PRODUCT_DETAIL_POP_TRANSITION },
            predictivePopTransitionSpec = { PRODUCT_DETAIL_POP_TRANSITION },
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
                                val route =
                                    AppRoute.ProductDetail(
                                        productId = productId,
                                        imageUrl = imageUrl,
                                        imageWidth = imageWidth,
                                        imageHeight = imageHeight,
                                    )
                                // Not a cosmetic dedupe. `NavEntry` derives `contentKey` as
                                // `key.toString()` and `AppRoute.ProductDetail` is a data class, so
                                // a double tap pushes two entries sharing one `contentKey`. Popping
                                // the upper one then runs both decorators' `onPop` against that
                                // shared key — `removeState` and `clearKey` — destroying the saved
                                // state and `ViewModelStore` of the duplicate still underneath. It
                                // fails silently: back appears to do nothing, the selected size is
                                // gone and the product refetches. Covered by
                                // `ProductListDoubleTapTest`.
                                if (backStack.last() == route) return@ProductListScreen
                                backStack.add(route)
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
