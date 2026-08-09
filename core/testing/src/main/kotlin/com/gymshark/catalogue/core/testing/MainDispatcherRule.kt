package com.gymshark.catalogue.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Swaps [Dispatchers.Main] for a deterministic test dispatcher around each test, so
 * `viewModelScope`-launched work runs synchronously. JUnit 5's equivalent of the classic JUnit 4
 * `MainDispatcherRule` — register with `@JvmField @RegisterExtension` in a test class, not
 * `@get:Rule` (docs/ARCHITECTURE.md §9.0: ViewModel tests are JUnit 5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
