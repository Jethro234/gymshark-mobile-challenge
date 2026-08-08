package com.gymshark.catalogue.core.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductTest {
    @Test
    fun `slash-separated colours normalise to dot-separated`() {
        assertEquals(
            "Court Blue · Moonstone Blue · White",
            normaliseColour("Court Blue/Moonstone Blue/White"),
        )
    }

    @Test
    fun `pipe-separated colours normalise to dot-separated`() {
        assertEquals("Savanna · Cherry Brown", normaliseColour("Savanna | Cherry Brown"))
    }

    @Test
    fun `a single colour is unchanged, with no trailing separator`() {
        assertEquals("Navy", normaliseColour("Navy"))
    }

    @Test
    fun `product is discounted only when compareAtPrice is present and strictly greater than price`() {
        val discounted =
            productFixture(
                price = Money.fromMajorUnits(50),
                compareAtPrice = Money.fromMajorUnits(65),
            )

        assertTrue(discounted.isDiscounted)
    }

    @Test
    fun `product is not discounted when compareAtPrice is absent`() {
        val product = productFixture(price = Money.fromMajorUnits(50), compareAtPrice = null)

        assertFalse(product.isDiscounted)
    }

    @Test
    fun `product is not discounted when compareAtPrice does not exceed price`() {
        val equal =
            productFixture(
                price = Money.fromMajorUnits(50),
                compareAtPrice = Money.fromMajorUnits(50),
            )
        val lower =
            productFixture(
                price = Money.fromMajorUnits(50),
                compareAtPrice = Money.fromMajorUnits(40),
            )

        assertFalse(equal.isDiscounted)
        assertFalse(lower.isDiscounted)
    }

    @Test
    fun `discountPercentage is never recomputed, only ever displayed as supplied`() {
        // compareAtPrice and discountPercentage are null on every product in the real payload
        // (docs/ARCHITECTURE.md §15), so this is exercised entirely through constructed
        // fixtures rather than the committed payload — genuine schema handling, not invented
        // behaviour: see README.md "Assumptions" #4.
        val product =
            productFixture(
                price = Money.fromMajorUnits(50),
                compareAtPrice = Money.fromMajorUnits(65),
                discountPercentage = 30,
            )

        // The arithmetic difference between 50 and 65 is ~23%, not 30 — the supplied figure
        // is trusted regardless.
        assertEquals(30, product.discountPercentage)
    }

    private fun productFixture(
        price: Money,
        compareAtPrice: Money?,
        discountPercentage: Int? = null,
    ): Product =
        Product(
            id = "1",
            title = "Speed Leggings",
            colour = "Navy",
            type = "Leggings",
            fit = "mid-rise",
            price = price,
            compareAtPrice = compareAtPrice,
            discountPercentage = discountPercentage,
            labels = emptyList(),
            featuredMedia = null,
            media = emptyList(),
            availableSizes = emptyList(),
        )
}
