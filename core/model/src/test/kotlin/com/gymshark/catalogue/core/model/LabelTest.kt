package com.gymshark.catalogue.core.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LabelTest {
    @Test
    fun `known merchandising values parse correctly`() {
        assertEquals(Label.GoingFast, Label.parse("going-fast"))
        assertEquals(Label.New, Label.parse("new"))
        assertEquals(Label.LimitedEdition, Label.parse("limited-edition"))
        assertEquals(Label.Popular, Label.parse("popular"))
    }

    @Test
    fun `known sustainability values parse correctly`() {
        assertEquals(Label.RecycledNylon, Label.parse("recycled-nylon"))
        assertEquals(Label.RecycledPolyester, Label.parse("recycled-polyester"))
    }

    @Test
    fun `merchandising labels are categorised as merchandising`() {
        assertEquals(LabelCategory.Merchandising, Label.GoingFast.category)
        assertEquals(LabelCategory.Merchandising, Label.New.category)
        assertEquals(LabelCategory.Merchandising, Label.LimitedEdition.category)
        assertEquals(LabelCategory.Merchandising, Label.Popular.category)
    }

    @Test
    fun `sustainability labels are categorised as sustainability`() {
        assertEquals(LabelCategory.Sustainability, Label.RecycledNylon.category)
        assertEquals(LabelCategory.Sustainability, Label.RecycledPolyester.category)
    }

    @Test
    fun `parsing is case-insensitive and trims surrounding whitespace`() {
        assertEquals(Label.GoingFast, Label.parse("  GOING-FAST "))
        assertEquals(Label.RecycledNylon, Label.parse("Recycled-Nylon"))
    }

    @Test
    fun `an unseen label survives as unknown, defaulting to merchandising`() {
        val label = Label.parse("back-in-stock")

        assertTrue(label is Label.Unknown)
        assertEquals("back-in-stock", label.raw)
        assertEquals(LabelCategory.Merchandising, label.category)
    }

    @Test
    fun `null labels field yields no labels`() {
        assertEquals(emptyList(), parseLabels(null))
    }

    @Test
    fun `empty labels array yields no labels, identically to null`() {
        // A real value in the payload, not a hypothetical: one product has labels: [].
        assertEquals(emptyList(), parseLabels(emptyList()))
        assertEquals(parseLabels(null), parseLabels(emptyList()))
    }

    @Test
    fun `a product can carry labels from both categories at once`() {
        // The real payload's one three-label product: new + both recycled labels.
        val labels = parseLabels(listOf("new", "recycled-nylon", "recycled-polyester"))

        assertEquals(Label.New, labels.merchandisingBadge)
        assertEquals(listOf(Label.RecycledNylon, Label.RecycledPolyester), labels.sustainabilityChips)
    }

    @Test
    fun `at most one merchandising label is shown, first by array order`() {
        // Not observed in the real payload — every product carries at most one merchandising
        // label — but the schema doesn't exclude it, so the rule is covered explicitly.
        val labels = parseLabels(listOf("popular", "going-fast"))

        assertEquals(Label.Popular, labels.merchandisingBadge)
    }

    @Test
    fun `no merchandising label means no badge`() {
        val labels = parseLabels(listOf("recycled-nylon"))

        assertNull(labels.merchandisingBadge)
        assertEquals(listOf(Label.RecycledNylon), labels.sustainabilityChips)
    }

    @Test
    fun `no sustainability label means no chips`() {
        val labels = parseLabels(listOf("going-fast"))

        assertEquals(emptyList(), labels.sustainabilityChips)
    }
}
