package com.gymshark.catalogue.feature.products

import androidx.lifecycle.SavedStateHandle
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
import kotlin.test.assertIs

class ProductDetailViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductRepository()

    private fun createGatedViewModel(
        productId: String,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): Pair<ProductDetailViewModel, CompletableDeferred<Unit>> {
        val gate = CompletableDeferred<Unit>()
        repository.fetchGate = gate
        return ProductDetailViewModel(productId, savedStateHandle, repository) to gate
    }

    @Test
    fun `empty cache refetches — loading then content`() =
        runTest {
            val product = productFixture(id = "1", title = "Speed Leggings")
            repository.remoteProducts = Result.success(listOf(product))
            val (viewModel, gate) = createGatedViewModel(productId = "1")

            viewModel.uiState.test {
                assertEquals(ProductDetailUiState.Loading, awaitItem())
                gate.complete(Unit)
                val content = assertIs<ProductDetailUiState.Content>(awaitItem())
                assertEquals("Speed Leggings", content.product.title)
            }
        }

    @Test
    fun `refetch succeeding without the requested id is Error(NotFound)`() =
        runTest {
            repository.remoteProducts = Result.success(listOf(productFixture(id = "other")))
            val (viewModel, gate) = createGatedViewModel(productId = "missing")

            viewModel.uiState.test {
                assertEquals(ProductDetailUiState.Loading, awaitItem())
                gate.complete(Unit)
                val error = assertIs<ProductDetailUiState.Error>(awaitItem())
                assertEquals(ErrorCause.NotFound, error.cause)
            }
        }

    @Test
    fun `refetch failing maps to the corresponding typed cause`() =
        runTest {
            repository.remoteProducts = Result.failure(IOException())
            val (viewModel, gate) = createGatedViewModel(productId = "1")

            viewModel.uiState.test {
                assertEquals(ProductDetailUiState.Loading, awaitItem())
                gate.complete(Unit)
                val error = assertIs<ProductDetailUiState.Error>(awaitItem())
                assertEquals(ErrorCause.NoConnection, error.cause)
            }
        }

    @Test
    fun `selected size survives recreation from the same SavedStateHandle`() =
        runTest {
            val product = productFixture(id = "1")
            repository.seedCache(listOf(product))
            val savedStateHandle = SavedStateHandle()
            val firstViewModel = ProductDetailViewModel("1", savedStateHandle, repository)

            firstViewModel.uiState.test {
                assertIs<ProductDetailUiState.Content>(awaitItem())
                firstViewModel.selectSize("m")
                val selected = assertIs<ProductDetailUiState.Content>(awaitItem())
                assertEquals("m", selected.selectedSize)
            }

            // Simulates process death and restoration: a fresh ViewModel instance recreated
            // from the same (system-restored) SavedStateHandle.
            val restoredViewModel = ProductDetailViewModel("1", savedStateHandle, repository)

            restoredViewModel.uiState.test {
                val content = assertIs<ProductDetailUiState.Content>(awaitItem())
                assertEquals("m", content.selectedSize)
            }
        }
}
