package com.gymshark.catalogue.core.data

import com.gymshark.catalogue.core.data.remote.AlgoliaServiceFactory
import com.gymshark.catalogue.core.model.ErrorCause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultProductRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: DefaultProductRepository

    @BeforeEach
    fun setUp() {
        server.start()
        val service = AlgoliaServiceFactory.create(baseUrl = server.url("/").toString())
        repository = DefaultProductRepository(service, ioDispatcher = Dispatchers.Unconfined)
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun realPayloadBody(): String {
        val stream = javaClass.classLoader?.getResourceAsStream("algolia-example-payload.json")
        return checkNotNull(stream) { "Missing committed payload resource" }.bufferedReader().readText()
    }

    // ---- Cache behaviour (task 4.5) ----

    @Test
    fun `first read fetches from the network`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )

            val result = repository.getProducts().first()

            assertTrue(result.isSuccess)
            assertEquals(60, result.getOrThrow().size)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `second read serves from cache without a second network call`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )

            repository.getProducts().first()
            repository.getProducts().first()

            assertEquals(1, server.requestCount)
        }

    @Test
    fun `getProduct reads a cached product without a network call`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )
            val products = repository.getProducts().first().getOrThrow()
            val target = products.first()

            val result = repository.getProduct(target.id)

            assertEquals(target, result.getOrThrow())
            assertEquals(1, server.requestCount)
        }

    // ---- Refresh (task 4.5) ----

    @Test
    fun `refresh always hits the network even when the cache is populated`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )

            repository.getProducts().first()
            repository.refresh()

            assertEquals(2, server.requestCount)
        }

    @Test
    fun `refresh failure keeps the existing cached content`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )
            server.enqueue(MockResponse.Builder().code(500).build())

            repository.getProducts().first()
            val refreshResult = repository.refresh()
            val subsequentRead = repository.getProducts().first()

            assertTrue(refreshResult.isFailure)
            assertTrue(subsequentRead.isSuccess)
            assertEquals(60, subsequentRead.getOrThrow().size)
        }

    // ---- getProduct: cache miss and not-found (task 4.5, product-detail-screen spec) ----

    @Test
    fun `getProduct refetches on a cache miss`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )

            val result = repository.getProduct("6732609257571")

            assertTrue(result.isSuccess)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `getProduct resolves to NotFound when a refetch succeeds without the id`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .build(),
            )

            val result = repository.getProduct("this-id-does-not-exist")

            assertTrue(result.isFailure)
            assertEquals(ErrorCause.NotFound, result.exceptionOrNull()?.toErrorCause())
        }

    // ---- MockWebServer failure suite (task 4.7) ----

    @Test
    fun `HTTP 500 maps to Server`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(500).build())

            val result = repository.getProducts().first()

            assertTrue(result.isFailure)
            assertEquals(ErrorCause.Server, result.exceptionOrNull()?.toErrorCause())
        }

    @Test
    fun `socket timeout maps to NoConnection`() =
        runTest {
            server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())
            val service =
                AlgoliaServiceFactory.create(
                    baseUrl = server.url("/").toString(),
                    okHttpClient =
                        okhttp3.OkHttpClient
                            .Builder()
                            .callTimeout(java.time.Duration.ofMillis(200))
                            .build(),
                )
            val timeoutRepository = DefaultProductRepository(service, ioDispatcher = Dispatchers.Unconfined)

            val result = timeoutRepository.getProducts().first()

            assertTrue(result.isFailure)
            assertEquals(ErrorCause.NoConnection, result.exceptionOrNull()?.toErrorCause())
        }

    @Test
    fun `malformed JSON body maps to Malformed`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("{ this is not valid json")
                    .build(),
            )

            val result = repository.getProducts().first()

            assertTrue(result.isFailure)
            assertEquals(ErrorCause.Malformed, result.exceptionOrNull()?.toErrorCause())
        }

    @Test
    fun `truncated body maps to Malformed`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(realPayloadBody())
                    .onResponseBody(SocketEffect.CloseSocket())
                    .build(),
            )

            val result = repository.getProducts().first()

            assertTrue(result.isFailure)
        }

    @Test
    fun `empty hits array is a successful empty list, not an error`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"hits": []}""")
                    .build(),
            )

            val result = repository.getProducts().first()

            assertTrue(result.isSuccess)
            assertEquals(emptyList(), result.getOrThrow())
        }

    @Test
    fun `an unclassifiable failure maps to Unknown`() {
        val cause = RuntimeException("something unexpected").toErrorCause()

        assertIs<ErrorCause.Unknown>(cause)
    }
}
