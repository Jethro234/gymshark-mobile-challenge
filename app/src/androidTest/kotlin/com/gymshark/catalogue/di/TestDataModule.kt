package com.gymshark.catalogue.di

import com.gymshark.catalogue.core.data.ProductRepository
import com.gymshark.catalogue.core.testing.FakeProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces [DataModule] for every androidTest — reads whatever [TestRepositoryHolder.repository]
 * holds at the moment Dagger first resolves [ProductRepository] (lazily, when the Activity's
 * first `hiltViewModel()` call needs it). Tests set the holder in a property initialiser, which
 * runs at test-instance construction — before `HiltAndroidRule`/`createAndroidComposeRule`
 * launch anything — so the ViewModel's first load already sees the test's data.
 *
 * `@BindValue` was tried first and rejected: it adds a competing binding rather than replacing
 * [DataModule]'s, which Dagger rejects as a duplicate binding. A `@TestInstallIn` module is the
 * correct mechanism (`docs/ARCHITECTURE.md` §9.3) — this holder is what makes it configurable
 * per test despite being resolved through a plain `@Provides` function.
 */
object TestRepositoryHolder {
    var repository: ProductRepository = FakeProductRepository()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
internal object TestDataModule {
    @Provides
    @Singleton
    fun provideProductRepository(): ProductRepository = TestRepositoryHolder.repository
}
