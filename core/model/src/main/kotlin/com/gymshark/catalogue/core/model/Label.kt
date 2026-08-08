package com.gymshark.catalogue.core.model

/** Which slot a label competes for — see docs/ARCHITECTURE.md §8. */
public enum class LabelCategory {
    /** Urgency and novelty. Renders as the single image badge. */
    Merchandising,

    /** Material provenance. Renders as a chip near the description. */
    Sustainability,
}

/**
 * A product label. The known six values split into two categories that never compete for the
 * same display slot — see docs/ARCHITECTURE.md §8 and docs/DESIGN.md §4.
 */
public sealed interface Label {
    public val raw: String
    public val category: LabelCategory

    public data object GoingFast : Label {
        override val raw: String = "going-fast"
        override val category: LabelCategory = LabelCategory.Merchandising
    }

    public data object New : Label {
        override val raw: String = "new"
        override val category: LabelCategory = LabelCategory.Merchandising
    }

    public data object LimitedEdition : Label {
        override val raw: String = "limited-edition"
        override val category: LabelCategory = LabelCategory.Merchandising
    }

    public data object Popular : Label {
        override val raw: String = "popular"
        override val category: LabelCategory = LabelCategory.Merchandising
    }

    public data object RecycledNylon : Label {
        override val raw: String = "recycled-nylon"
        override val category: LabelCategory = LabelCategory.Sustainability
    }

    public data object RecycledPolyester : Label {
        override val raw: String = "recycled-polyester"
        override val category: LabelCategory = LabelCategory.Sustainability
    }

    /**
     * A label the CMS supplied that isn't one of the six known values. Defaults to
     * [LabelCategory.Merchandising] — the badge already has a quiet treatment for this; a
     * material chip does not, and guessing at unknown material provenance would be worse than
     * showing nothing.
     */
    public data class Unknown(
        override val raw: String,
    ) : Label {
        override val category: LabelCategory = LabelCategory.Merchandising
    }

    public companion object {
        private val knownByRaw: Map<String, Label> =
            listOf(GoingFast, New, LimitedEdition, Popular, RecycledNylon, RecycledPolyester)
                .associateBy { it.raw }

        /** Null-safe, case-insensitive, trims surrounding whitespace. */
        public fun parse(raw: String): Label {
            val trimmed = raw.trim()
            return knownByRaw[trimmed.lowercase()] ?: Unknown(trimmed)
        }
    }
}

/**
 * Parses a product's `labels` field. `null` and `[]` are semantically identical — both are
 * real values in the payload and both mean "no labels".
 */
public fun parseLabels(raw: List<String>?): List<Label> = raw?.map(Label::parse) ?: emptyList()

/**
 * The single label shown as the image badge, if any. At most one merchandising label is ever
 * shown — no product in the real payload carries more than one, but if it ever did, the first
 * by array order wins rather than stacking a second badge.
 */
public val List<Label>.merchandisingBadge: Label?
    get() = firstOrNull { it.category == LabelCategory.Merchandising }

/** Every sustainability label, rendered as material chips independently of the badge slot. */
public val List<Label>.sustainabilityChips: List<Label>
    get() = filter { it.category == LabelCategory.Sustainability }
