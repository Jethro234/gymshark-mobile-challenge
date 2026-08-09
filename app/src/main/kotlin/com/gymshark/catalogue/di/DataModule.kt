package com.gymshark.catalogue.di

import com.gymshark.catalogue.core.data.ProductRepository
import com.gymshark.catalogue.core.data.ProductRepositoryFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Wires the data layer into Hilt's graph (`docs/ARCHITECTURE.md` §4): `@Singleton` for
 * [OkHttpClient] and [ProductRepository] so the repository's in-memory cache outlives the
 * list screen and survives into the detail screen (§10).
 *
 * [ProductRepositoryFactory] keeps `:core:data` framework-agnostic — this module is the only
 * place that knows Hilt exists — and is also why this module never names `AlgoliaService` or
 * `DefaultProductRepository` directly: both are `internal` to `:core:data`, along with every
 * wire-format DTO, since the factory is the only construction path either of them needs.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideProductRepository(
        okHttpClient: OkHttpClient,
        ioDispatcher: CoroutineDispatcher,
    ): ProductRepository = ProductRepositoryFactory.create(okHttpClient, ioDispatcher)
}
