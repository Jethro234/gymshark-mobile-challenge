package com.gymshark.catalogue.core.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create

/**
 * Builds an [AlgoliaService]. A plain factory function, not a Hilt module, so tests can point
 * it at a local `MockWebServer` (docs/ARCHITECTURE.md §9.1) without any DI graph — the same
 * factory is what [com.gymshark.catalogue.core.data.ProductRepositoryFactory] calls for the
 * real base URL.
 */
internal object AlgoliaServiceFactory {
    const val BASE_URL: String = "https://cdn.develop.gymshark.com/"

    private val json = Json { ignoreUnknownKeys = true }

    fun create(
        baseUrl: String = BASE_URL,
        okHttpClient: OkHttpClient = OkHttpClient(),
    ): AlgoliaService =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
}
