package com.gymshark.catalogue

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.gymshark.catalogue.core.testing.FakeProductRepository
import com.gymshark.catalogue.core.testing.productFixture
import com.gymshark.catalogue.di.TestRepositoryHolder
import com.gymshark.catalogue.feature.products.PRODUCT_GRID_TEST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import com.gymshark.catalogue.feature.products.R as ProductsR

private const val WAIT_TIMEOUT_MILLIS = 5_000L

/**
 * `docs/ARCHITECTURE.md` §10.1: on refresh failure the existing content is retained **and the
 * error surfaces transiently**. `ProductListViewModelTest` covers the ViewModel half, but the
 * defect this guards against was a state change nothing rendered — only a real gesture against a
 * real `PullToRefreshBox` proves the message reaches the screen. Fixture setup follows
 * `ProductListNavigationTest`'s ordering, documented there.
 */
@HiltAndroidTest
class ProductListRefreshErrorTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    private val fakeRepository =
        FakeProductRepository()
            .apply {
                remoteProducts = Result.success(listOf(productFixture(id = "1", title = "Product 1")))
            }.also { TestRepositoryHolder.repository = it }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun failedPullToRefreshShowsTheCauseWithoutBlankingTheList() {
        waitUntilNodeWithTextExists("Product 1")

        fakeRepository.remoteProducts = Result.failure(IOException())
        composeRule.onNodeWithTag(PRODUCT_GRID_TEST_TAG).performTouchInput { swipeDown() }

        val offlineMessage = composeRule.activity.getString(ProductsR.string.product_list_error_no_connection)
        waitUntilNodeWithTextExists(offlineMessage)

        // Both halves of the contract: the failure is visible, and the content it failed to
        // replace is still on screen behind it.
        waitUntilNodeWithTextExists("Product 1")
    }

    private fun waitUntilNodeWithTextExists(text: String) {
        composeRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
