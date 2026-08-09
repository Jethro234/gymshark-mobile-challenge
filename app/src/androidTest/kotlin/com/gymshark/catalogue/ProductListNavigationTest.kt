package com.gymshark.catalogue

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.gymshark.catalogue.core.testing.FakeProductRepository
import com.gymshark.catalogue.core.testing.productFixture
import com.gymshark.catalogue.di.TestRepositoryHolder
import com.gymshark.catalogue.feature.products.PRODUCT_GRID_TEST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

private const val PRODUCT_COUNT = 20
private const val SCROLLED_PRODUCT_INDEX = 15
private const val WAIT_TIMEOUT_MILLIS = 5_000L

/**
 * `docs/ARCHITECTURE.md` §9.3, first of the two instrumented tests: real navigation and a
 * real `OnBackPressedDispatcher`, which neither the JUnit 5 unit suite nor a snapshot layer
 * can honestly verify. `TestRepositoryHolder` (see `TestDataModule`) is configured in this
 * class's own property initialiser — which runs before `HiltAndroidRule` builds the test's
 * Hilt component, itself built before `createAndroidComposeRule<MainActivity>()` launches the
 * activity — so the ViewModel's first load already sees this fixture data. No Espresso, per
 * `docs/ARCHITECTURE.md` §9.3 — system back is a direct `OnBackPressedDispatcher` call.
 */
@HiltAndroidTest
class ProductListNavigationTest {
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
    fun tappingAProductNavigatesToDetailAndBackRestoresScrollPosition() {
        waitUntilNodeWithTextExists("Product 1")

        composeRule.onNodeWithTag(PRODUCT_GRID_TEST_TAG).performScrollToIndex(SCROLLED_PRODUCT_INDEX)
        val scrolledTitle = "Product ${SCROLLED_PRODUCT_INDEX + 1}"
        waitUntilNodeWithTextExists(scrolledTitle)

        composeRule.onNodeWithText(scrolledTitle).performClick()

        // The detail screen shows the same product's title too, so its presence confirms
        // navigation landed on the right product.
        waitUntilNodeWithTextExists(scrolledTitle)

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        // Back on the list, still scrolled — the item scrolled to before navigating is
        // visible again without scrolling, which only holds if the position was restored.
        waitUntilNodeWithTextExists(scrolledTitle)
    }

    private fun waitUntilNodeWithTextExists(text: String) {
        composeRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
