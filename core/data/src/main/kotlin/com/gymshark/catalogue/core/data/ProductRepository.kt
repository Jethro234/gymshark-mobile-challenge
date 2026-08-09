package com.gymshark.catalogue.core.data

import com.gymshark.catalogue.core.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Single fetch plus in-memory cache (docs/ARCHITECTURE.md §10). The instance is expected to be
 * `@Singleton`-scoped in whatever DI graph wires it up, so its cache outlives the list screen
 * and the detail screen can read from it.
 */
public interface ProductRepository {
    /** Cache when populated, else network. */
    public fun getProducts(): Flow<Result<List<Product>>>

    /** Always network; replaces the cache on success. On failure the existing cache is kept. */
    public suspend fun refresh(): Result<List<Product>>

    /** Cache hit returns directly; a miss refetches and looks up again. */
    public suspend fun getProduct(id: String): Result<Product>
}

/**
 * Thrown by [ProductRepository.getProduct] when a refetch succeeds but [id] is absent from the
 * response — the product genuinely no longer exists in the answer, not a network failure.
 * Public rather than internal so [com.gymshark.catalogue.core.testing.FakeProductRepository]
 * can throw the identical type, keeping [toErrorCause] consistent for both.
 */
public class ProductNotFoundException(
    id: String,
) : Exception("Product not found: $id")
