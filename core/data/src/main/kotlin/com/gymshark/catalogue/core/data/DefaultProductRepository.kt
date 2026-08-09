package com.gymshark.catalogue.core.data

import com.gymshark.catalogue.core.data.remote.AlgoliaService
import com.gymshark.catalogue.core.data.remote.toDomain
import com.gymshark.catalogue.core.model.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * @param ioDispatcher Injected rather than hardcoded (docs/ARCHITECTURE.md §9.0), so tests can
 *   substitute a deterministic dispatcher.
 */
public class DefaultProductRepository(
    private val service: AlgoliaService,
    private val ioDispatcher: CoroutineDispatcher,
) : ProductRepository {
    @Volatile
    private var cache: List<Product>? = null

    override fun getProducts(): Flow<Result<List<Product>>> =
        flow {
            val cached = cache
            emit(if (cached != null) Result.success(cached) else fetchAndCache())
        }.flowOn(ioDispatcher)

    override suspend fun refresh(): Result<List<Product>> = withContext(ioDispatcher) { fetchAndCache() }

    override suspend fun getProduct(id: String): Result<Product> =
        withContext(ioDispatcher) {
            val cached = cache?.find { it.id == id }
            if (cached != null) {
                Result.success(cached)
            } else {
                fetchAndCache().fold(
                    onSuccess = { products -> productOrNotFound(products, id) },
                    onFailure = { Result.failure(it) },
                )
            }
        }

    private fun productOrNotFound(
        products: List<Product>,
        id: String,
    ): Result<Product> {
        val product = products.find { it.id == id }
        return if (product != null) Result.success(product) else Result.failure(ProductNotFoundException(id))
    }

    // On failure, cache is left exactly as it was — a failed refresh must not destroy content
    // already on screen (docs/ARCHITECTURE.md §10.1).
    private suspend fun fetchAndCache(): Result<List<Product>> {
        val result = runCatching { service.getProducts().toDomain() }
        result.onSuccess { cache = it }
        return result
    }
}
