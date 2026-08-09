package com.gymshark.catalogue.feature.products

import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.model.Label
import java.util.Locale

/** GBP, always — the payload has no locale field and the brand this catalogue represents is UK-only. */
internal val DISPLAY_LOCALE: Locale = Locale.UK

/** Shared between the list and detail ViewModels — both map a merchandising badge to text and tier. */
internal fun Label.toBadgeText(): String =
    when (this) {
        Label.GoingFast -> "Going fast"
        Label.LimitedEdition -> "Limited edition"
        Label.New -> "New"
        Label.Popular -> "Popular"
        Label.RecycledNylon, Label.RecycledPolyester ->
            error("Sustainability label reached the merchandising badge slot: $raw")
        is Label.Unknown -> raw.toDisplayTitleCase()
    }

internal fun Label.toBadgeTier(): GsLabelTier =
    when (this) {
        Label.GoingFast, Label.LimitedEdition -> GsLabelTier.Urgency
        Label.New, Label.Popular -> GsLabelTier.Informational
        Label.RecycledNylon, Label.RecycledPolyester ->
            error("Sustainability label reached the merchandising badge slot: $raw")
        is Label.Unknown -> GsLabelTier.Unknown
    }

/** Detail screen only — sustainability labels never reach the list screen's badge slot. */
internal fun Label.toMaterialChipText(): String =
    when (this) {
        Label.RecycledNylon -> "Recycled nylon"
        Label.RecycledPolyester -> "Recycled polyester"
        else -> error("Merchandising label reached the sustainability chip slot: $raw")
    }

internal fun String.toDisplayTitleCase(): String =
    split('-', ' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
