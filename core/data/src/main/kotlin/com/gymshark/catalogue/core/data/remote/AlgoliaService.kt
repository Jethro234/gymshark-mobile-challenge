package com.gymshark.catalogue.core.data.remote

import retrofit2.http.GET

/**
 * The endpoint is one static CDN file — no pagination, no auth, no query parameters
 * (docs/ARCHITECTURE.md §2). See [AlgoliaServiceFactory] for the base URL.
 *
 * Internal: [com.gymshark.catalogue.core.data.ProductRepositoryFactory] is the module's only
 * public entry point for constructing a repository, so nothing outside `:core:data` ever
 * needs to name this type or the DTOs it returns.
 */
internal interface AlgoliaService {
    @GET("training/mock-product-responses/algolia-example-payload.json")
    suspend fun getProducts(): AlgoliaEnvelopeDto
}
