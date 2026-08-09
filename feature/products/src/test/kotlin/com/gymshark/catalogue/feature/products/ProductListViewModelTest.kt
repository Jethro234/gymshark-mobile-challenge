package com.gymshark.catalogue.feature.products

import app.cash.turbine.test
import com.gymshark.catalogue.core.model.ErrorCause
import com.gymshark.catalogue.core.testing.FakeProductRepository
import com.gymshark.catalogue.core.testing.MainDispatcherRule
import com.gymshark.catalogue.core.testing.productFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductListViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()

    /** Gates the first fetch so Loading is observable before it resolves. */
    private fun createGatedViewModel(): Pair<ProductListViewModel, CompletableDeferred<Unit>> {
        val gate = CompletableDeferred<Unit>()
        repository.fetchGate = gate
        return ProductListViewModel(repository) to gate
    }

    @Test
    fun `loading transitions to content when products load`() =
        runTest {
            repository.remoteProducts = Result.success(listOf(productFixture(id = "1")))
            val (viewModel, gate) = createGatedViewModel()

            viewModel.uiState.test {
                assertEquals(ProductListUiState.Loading, awaitItem())
                gate.complete(Unit)
                val content = assertIs<ProductListUiState.Content>(awaitItem())
                assertEquals(1, content.products.size)
                assertFalse(content.isRefreshing)
            }
        }

    @Test
    fun `loading transitions to empty when the response has no products`() =
        runTest {
            repository.remoteProducts = Result.success(emptyList())
            val (viewModel, gate) = createGatedViewModel()

            viewModel.uiState.test {
                assertEquals(ProductListUiState.Loading, awaitItem())
                gate.complete(Unit)
                assertEquals(ProductListUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `loading transitions to error carrying the mapped cause`() =
        runTest {
            repository.remoteProducts = Result.failure(IOException())
            val (viewModel, gate) = createGatedViewModel()

            viewModel.uiState.test {
                assertEquals(ProductListUiState.Loading, awaitItem())
                gate.complete(Unit)
                val error = assertIs<ProductListUiState.Error>(awaitItem())
                assertEquals(ErrorCause.NoConnection, error.cause)
            }
        }

    @Test
    fun `retry recovers into content after an error`() =
        runTest {
            repository.remoteProducts = Result.failure(IOException())
            val (viewModel, firstGate) = createGatedViewModel()

            viewModel.uiState.test {
                assertEquals(ProductListUiState.Loading, awaitItem())
                firstGate.complete(Unit)
                assertIs<ProductListUiState.Error>(awaitItem())

                repository.remoteProducts = Result.success(listOf(productFixture()))
                val retryGate = CompletableDeferred<Unit>()
                repository.fetchGate = retryGate
                viewModel.retry()

                assertEquals(ProductListUiState.Loading, awaitItem())
                retryGate.complete(Unit)
                assertIs<ProductListUiState.Content>(awaitItem())
            }
        }

    @Test
    fun `refresh keeps content visible while in flight and once resolved`() =
        runTest {
            val original = productFixture(id = "1", title = "Original")
            repository.seedCache(listOf(original))
            val viewModel = ProductListViewModel(repository)

            viewModel.uiState.test {
                val initial = assertIs<ProductListUiState.Content>(awaitItem())
                assertFalse(initial.isRefreshing)

                repository.remoteProducts = Result.success(listOf(productFixture(id = "2", title = "Refreshed")))
                val refreshGate = CompletableDeferred<Unit>()
                repository.fetchGate = refreshGate
                viewModel.refresh()

                val refreshing = assertIs<ProductListUiState.Content>(awaitItem())
                assertTrue(refreshing.isRefreshing)
                assertEquals(initial.products, refreshing.products)

                refreshGate.complete(Unit)

                val resolved = assertIs<ProductListUiState.Content>(awaitItem())
                assertFalse(resolved.isRefreshing)
                assertEquals("Refreshed", resolved.products.single().title)
            }
        }

    @Test
    fun `refresh failure keeps the previous content on screen`() =
        runTest {
            val original = productFixture(id = "1", title = "Original")
            repository.seedCache(listOf(original))
            val viewModel = ProductListViewModel(repository)

            viewModel.uiState.test {
                val initial = assertIs<ProductListUiState.Content>(awaitItem())

                repository.remoteProducts = Result.failure(IOException())
                val refreshGate = CompletableDeferred<Unit>()
                repository.fetchGate = refreshGate
                viewModel.refresh()

                val refreshing = assertIs<ProductListUiState.Content>(awaitItem())
                assertTrue(refreshing.isRefreshing)

                refreshGate.complete(Unit)

                val afterFailure = assertIs<ProductListUiState.Content>(awaitItem())
                assertFalse(afterFailure.isRefreshing)
                assertEquals(initial.products, afterFailure.products)
            }
        }
}
