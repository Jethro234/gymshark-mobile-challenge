package com.gymshark.catalogue

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gymshark.catalogue.core.testing.FakeProductRepository
import com.gymshark.catalogue.core.testing.productFixture
import com.gymshark.catalogue.di.TestRepositoryHolder
import com.gymshark.catalogue.feature.products.PRODUCT_GRID_TEST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

private const val PRODUCT_COUNT = 5
private const val WAIT_TIMEOUT_MILLIS = 5_000L

/**
 * `docs/ARCHITECTURE.md` §9.3: a double tap on one card must push a single detail entry. The
 * defect this guards against is silent — `NavEntry.contentKey` is `key.toString()` and
 * `AppRoute.ProductDetail` is a data class, so two identical pushes share one key, and popping
 * the upper one clears the saved state and `ViewModelStore` of the entry still underneath.
 * Nothing crashes and nothing is logged; back simply appears to do nothing. Only a real double
 * tap against a real `NavDisplay` proves the guard in `GymsharkNavHost` holds. Fixture setup
 * follows `ProductListNavigationTest`'s ordering, documented there.
 */
@HiltAndroidTest
class ProductListDoubleTapTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    private val fakeRepository =
        FakeProductRepository()
            .apply {
                remoteProducts =
                    Result.success((1..PRODUCT_COUNT).map { i -> productFixture(id = "$i", title = "Product $i") })
            }.also { TestRepositoryHolder.repository = it }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun doubleTappingOneCardPushesASingleDetailEntry() {
        waitUntilNodeWithTextExists("Product 1")

        // Freeze the clock so no frame lands between the taps: the list card is still composed
        // and hit-testable, which is the exact window the duplicate push happens in. With the
        // clock running, performClick's own waitForIdle would complete the navigation first and
        // the second tap would land on the detail screen instead of the card.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Product 1").performClick()
        composeRule.onNodeWithText("Product 1").performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        // One back reaches the list only if one entry was pushed. Two entries leave the second
        // detail screen on top, and the grid never composes.
        waitUntilNodeWithTagExists(PRODUCT_GRID_TEST_TAG)
        composeRule.onNodeWithTag(PRODUCT_GRID_TEST_TAG).assertIsDisplayed()
    }

    private fun waitUntilNodeWithTextExists(text: String) {
        composeRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilNodeWithTagExists(tag: String) {
        composeRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
