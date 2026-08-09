package com.gymshark.catalogue.di

import com.gymshark.catalogue.core.data.DefaultProductRepository
import com.gymshark.catalogue.core.data.ProductRepository
import com.gymshark.catalogue.core.data.remote.AlgoliaService
import com.gymshark.catalogue.core.data.remote.AlgoliaServiceFactory
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
 * list screen and survives into the detail screen (§10). [AlgoliaService] stands in for the
 * doc's separately-named "Retrofit instance" — [AlgoliaServiceFactory] already encapsulates
 * Retrofit construction as an implementation detail, and nothing else needs the raw
 * `Retrofit` object, so exposing a second binding for it would be pure ceremony.
 *
 * [DefaultProductRepository] keeps a plain constructor rather than `@Inject` so `:core:data`
 * stays framework-agnostic — this module is the only place that knows Hilt exists.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideAlgoliaService(okHttpClient: OkHttpClient): AlgoliaService =
        AlgoliaServiceFactory.create(okHttpClient = okHttpClient)

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideProductRepository(
        service: AlgoliaService,
        ioDispatcher: CoroutineDispatcher,
    ): ProductRepository = DefaultProductRepository(service, ioDispatcher)
}
