package com.gymshark.catalogue

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gymshark.catalogue.core.testing.FakeProductRepository
import com.gymshark.catalogue.core.testing.productFixture
import com.gymshark.catalogue.di.TestRepositoryHolder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import com.gymshark.catalogue.feature.products.R as ProductsR

private const val WAIT_TIMEOUT_MILLIS = 5_000L

/** `docs/ARCHITECTURE.md` §9.3, second instrumented test: the error state's Retry action recovers to `Content`. */
@HiltAndroidTest
class ProductListRetryTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    private val fakeRepository =
        FakeProductRepository()
            .apply {
                remoteProducts = Result.failure(IOException())
            }.also { TestRepositoryHolder.repository = it }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun retryRecoversFromErrorToContent() {
        val offlineMessage = composeRule.activity.getString(ProductsR.string.product_list_error_no_connection)
        waitUntilNodeWithTextExists(offlineMessage)

        fakeRepository.remoteProducts = Result.success(listOf(productFixture(id = "1", title = "Product 1")))

        val retryLabel = composeRule.activity.getString(ProductsR.string.product_list_retry)
        composeRule.onNodeWithText(retryLabel).performClick()

        waitUntilNodeWithTextExists("Product 1")
    }

    private fun waitUntilNodeWithTextExists(text: String) {
        composeRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
