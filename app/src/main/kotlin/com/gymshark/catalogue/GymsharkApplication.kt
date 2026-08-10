package com.gymshark.catalogue

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Implementing [SingletonImageLoader.Factory] here is how Coil3 finds this on Android — it
 * checks whether the Application context itself implements the interface before falling back
 * to its own unconfigured default. Without this, Coil silently builds its own bare
 * `OkHttpClient()` (docs/ARCHITECTURE.md §12 doesn't mention this because nothing wired it
 * up), giving the app two disconnected connection pools: one for the Algolia API
 * ([com.gymshark.catalogue.di.DataModule]'s `@Singleton OkHttpClient`) and one Coil owns
 * privately for every product image.
 */
@HiltAndroidApp
class GymsharkApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = okHttpClient)) }
            // Coil's default disk cache is left in place deliberately: the product photos are
            // large, immutable, and each one is fetched at two different sizes (grid
            // thumbnail, then detail hero), so without it every cold start re-downloads
            // artwork that has not changed. This is image caching only — the Algolia payload
            // itself is still not persisted, so a first launch with no connection remains an
            // error state (README.md, "next steps").
            .build()
}
