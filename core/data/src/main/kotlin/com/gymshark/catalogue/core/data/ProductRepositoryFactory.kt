package com.gymshark.catalogue.core.data

import com.gymshark.catalogue.core.data.remote.AlgoliaServiceFactory
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient

/**
 * The module's sole entry point for constructing a [ProductRepository]. `:core:data` stays
 * framework-agnostic — `:app`'s Hilt module is the only place that knows Hilt exists
 * (docs/ARCHITECTURE.md §4) — so `:app` must construct this by hand rather than injecting it
 * directly. Routing that construction through one factory function, instead of `:app` wiring
 * [com.gymshark.catalogue.core.data.remote.AlgoliaService] and
 * [DefaultProductRepository] together itself, means `:app` never needs to name either —
 * `AlgoliaService` and every wire-format DTO stay `internal`.
 */
public object ProductRepositoryFactory {
    public fun create(
        okHttpClient: OkHttpClient,
        ioDispatcher: CoroutineDispatcher,
    ): ProductRepository =
        DefaultProductRepository(
            service = AlgoliaServiceFactory.create(okHttpClient = okHttpClient),
            ioDispatcher = ioDispatcher,
        )
}
