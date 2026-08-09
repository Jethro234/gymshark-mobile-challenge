package com.gymshark.catalogue.core.data.remote

import com.gymshark.catalogue.core.model.Label
import com.gymshark.catalogue.core.model.LabelCategory
import com.gymshark.catalogue.core.model.Money
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadRealPayload(): AlgoliaEnvelopeDto {
        val stream = javaClass.classLoader?.getResourceAsStream("algolia-example-payload.json")
        val text =
            checkNotNull(stream) { "Missing committed payload resource — see AGENTS.md §4a" }
                .bufferedReader()
                .readText()
        return json.decodeFromString(text)
    }

    @Test
    fun `all sixty hits in the real payload map to products`() {
        val products = loadRealPayload().toDomain()

        assertEquals(60, products.size)
    }

    @Test
    fun `every hit maps to a distinct product id`() {
        val products = loadRealPayload().toDomain()

        assertEquals(60, products.map { it.id }.toSet().size)
    }

    @Test
    fun `null fit maps to a null fit`() {
        val products = loadRealPayload().toDomain()

        assertTrue(products.any { it.fit == null })
    }

    @Test
    fun `present fit maps through unchanged`() {
        val products = loadRealPayload().toDomain()

        assertTrue(products.any { it.fit == "mid-rise" })
    }

    @Test
    fun `null compareAtPrice and discountPercentage map to null on every real product`() {
        // Genuine payload characteristic (docs/ARCHITECTURE.md §15), not a mapper bug.
        val products = loadRealPayload().toDomain()

        assertTrue(products.all { it.compareAtPrice == null })
        assertTrue(products.all { it.discountPercentage == null })
    }

    @Test
    fun `null labels map to an empty label list`() {
        val products = loadRealPayload().toDomain()

        val withoutLabels = products.filter { it.labels.isEmpty() }
        assertTrue(withoutLabels.isNotEmpty())
    }

    @Test
    fun `slash-separated colour is normalised`() {
        val products = loadRealPayload().toDomain()

        val product = products.first { it.colour == "Court Blue · Moonstone Blue · White" }
        assertEquals("Court Blue · Moonstone Blue · White", product.colour)
    }

    @Test
    fun `pipe-separated colour is normalised`() {
        val products = loadRealPayload().toDomain()

        val product = products.first { it.colour == "Savanna · Cherry Brown" }
        assertEquals("Savanna · Cherry Brown", product.colour)
    }

    @Test
    fun `the three-label product maps to one merchandising label and two sustainability labels`() {
        val products = loadRealPayload().toDomain()

        val product =
            products.first {
                it.title == "Adapt Ombre Seamless Leggings" && it.colour == "Rose Pink · Light Blue"
            }
        assertEquals(Label.New, product.labels.first { it.category == LabelCategory.Merchandising })
        assertEquals(
            setOf(Label.RecycledNylon, Label.RecycledPolyester),
            product.labels.filter { it.category == LabelCategory.Sustainability }.toSet(),
        )
    }

    @Test
    fun `price maps as major units`() {
        val products = loadRealPayload().toDomain()

        val product = products.first { it.title == "Energy Seamless Leggings" && it.colour == "Black" }
        assertEquals(Money.fromMajorUnits(65), product.price)
    }

    @Test
    fun `a product with no media maps to an empty media list without throwing`() {
        // Every real product happens to carry media, so this exercises the mapper's own
        // handling of the DTO's default rather than a genuine payload case.
        val dto =
            ProductDto(
                objectId = "1",
                title = "No Media Product",
                colour = "Black",
                price = 50,
                media = emptyList(),
            )

        val product = dto.toDomain()

        assertTrue(product.media.isEmpty())
        assertNull(product.featuredMedia)
    }

    @Test
    fun `a product with no featuredMedia maps to a null featuredMedia`() {
        val dto =
            ProductDto(
                objectId = "999999",
                title = "No Featured Media",
                colour = "Black",
                price = 50,
                featuredMedia = null,
            )

        val product = dto.toDomain()

        assertNull(product.featuredMedia)
    }

    @Test
    fun `every real image URL resolves except the one deliberately broken product`() {
        val products = loadRealPayload().toDomain()

        val broken = products.first { it.title == "Speed Leggings" && it.colour == "Moonstone Blue" }
        assertNotNull(broken.featuredMedia)
        assertTrue(
            broken.featuredMedia!!.url.contains("deliberately-broken"),
            "Expected the fixed broken-image product to carry the deliberately broken URL",
        )

        val others = products.filterNot { it === broken }
        assertFalse(others.any { it.featuredMedia?.url?.contains("deliberately-broken") == true })
    }
}
