package com.gymshark.catalogue.core.testing

import com.gymshark.catalogue.core.data.ProductNotFoundException
import com.gymshark.catalogue.core.data.ProductRepository
import com.gymshark.catalogue.core.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Hand-written fake, not a mock (docs/ARCHITECTURE.md §9.0) — mirrors
 * [com.gymshark.catalogue.core.data.DefaultProductRepository]'s cache-or-fetch contract so
 * ViewModel tests exercise realistic state transitions, with [remoteProducts] standing in for
 * whatever a real network call would have returned.
 *
 * The cache starts empty by default — the exact "process death, cache gone" condition
 * docs/ARCHITECTURE.md §11.2 requires the detail screen to survive. Call [seedCache] to
 * simulate a screen opened with the list screen's cache already warm.
 */
public class FakeProductRepository : ProductRepository {
    public var remoteProducts: Result<List<Product>> = Result.success(emptyList())

    private var cache: List<Product>? = null

    public fun seedCache(products: List<Product>) {
        cache = products
    }

    override fun getProducts(): Flow<Result<List<Product>>> =
        flow {
            val cached = cache
            emit(if (cached != null) Result.success(cached) else fetchAndCache())
        }

    override suspend fun refresh(): Result<List<Product>> = fetchAndCache()

    override suspend fun getProduct(id: String): Result<Product> {
        val cached = cache?.find { it.id == id }
        if (cached != null) return Result.success(cached)
        return fetchAndCache().fold(
            onSuccess = { products -> productOrNotFound(products, id) },
            onFailure = { Result.failure(it) },
        )
    }

    private fun productOrNotFound(
        products: List<Product>,
        id: String,
    ): Result<Product> {
        val product = products.find { it.id == id }
        return if (product != null) Result.success(product) else Result.failure(ProductNotFoundException(id))
    }

    private fun fetchAndCache(): Result<List<Product>> = remoteProducts.onSuccess { cache = it }
}
