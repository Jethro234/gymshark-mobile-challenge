package com.gymshark.catalogue.core.model

import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun `fromMajorUnits converts a realistic legging price`() {
        val money = Money.fromMajorUnits(65)

        assertEquals(6500L, money.minorUnits)
        assertEquals("£65.00", money.format(Locale.UK))
    }

    @Test
    fun `fromMajorUnits renders a large payload value as supplied, not special-cased`() {
        val money = Money.fromMajorUnits(1000)

        assertEquals(100_000L, money.minorUnits)
        assertEquals("£1,000.00", money.format(Locale.UK))
    }

    @Test
    fun `zero is a valid amount, not treated as absent`() {
        assertEquals("£0.00", Money.ZERO.format(Locale.UK))
        assertEquals(0L, Money.ZERO.minorUnits)
    }

    @Test
    fun `formatting follows the active locale's separators while the currency stays GBP`() {
        val money = Money.fromMajorUnits(1000)

        val ukFormatted = money.format(Locale.UK)
        val germanFormatted = money.format(Locale.GERMANY)

        // Locale determines grouping/decimal separator conventions...
        val ukMessage = "UK formatting should group thousands with a comma: $ukFormatted"
        assertTrue(ukFormatted.contains(","), ukMessage)

        val deMessage = "German formatting uses a comma as the decimal separator: $germanFormatted"
        assertTrue(germanFormatted.contains(","), deMessage)

        val noGroupingMessage = "German formatting should not group with a comma: $germanFormatted"
        assertTrue(!germanFormatted.contains("1,000"), noGroupingMessage)

        // ...but the currency itself is always GBP, regardless of locale.
        assertTrue(ukFormatted.contains("£"), "Expected a GBP symbol in UK-locale formatting: $ukFormatted")
        assertTrue(germanFormatted.contains("£"), "Expected a GBP symbol even under a German locale: $germanFormatted")
    }

    @Test
    fun `the reversed minor-units interpretation is a one-line swap, not a rewrite`() {
        // If price/compareAtPrice ever turn out to already be minor units, this is the
        // alternative entry point — no other production code changes.
        val money = Money.fromMinorUnits(1000)

        assertEquals(1000L, money.minorUnits)
        assertEquals("£10.00", money.format(Locale.UK))
    }

    @Test
    fun `money values compare by minor units`() {
        val cheaper = Money.fromMajorUnits(50)
        val pricier = Money.fromMajorUnits(65)

        assertTrue(pricier > cheaper)
        assertTrue(cheaper < pricier)
        assertEquals(cheaper, Money.fromMajorUnits(50))
    }
}
