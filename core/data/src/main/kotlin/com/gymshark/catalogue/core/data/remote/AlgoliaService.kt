package com.gymshark.catalogue.core.data.remote

import retrofit2.http.GET

/**
 * The endpoint is one static CDN file — no pagination, no auth, no query parameters
 * (docs/ARCHITECTURE.md §2). See [AlgoliaServiceFactory] for the base URL.
 */
public interface AlgoliaService {
    @GET("training/mock-product-responses/algolia-example-payload.json")
    public suspend fun getProducts(): AlgoliaEnvelopeDto
}
