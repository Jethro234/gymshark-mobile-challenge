package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymshark.catalogue.core.designsystem.theme.GsTheme

private val BADGE_HORIZONTAL_PADDING = 11.dp
private val BADGE_VERTICAL_PADDING = 4.dp
private val UNKNOWN_BORDER_WIDTH = 0.5.dp

/**
 * Merchandising-label tiers (`docs/DESIGN.md` §4). Meaning is carried by tier and text, not
 * by fill colour alone — [GsLabelBadge] always renders its [text].
 */
public enum class GsLabelTier {
    /** Going fast, Limited edition — `surfaceInk` fill. */
    Urgency,

    /** New, Popular — `surface` fill. */
    Informational,

    /** An unrecognised label — the quietest of the three, deliberately. */
    Unknown,
}

/**
 * A single merchandising-label pill positioned by the caller (image top-left inset, per
 * `docs/DESIGN.md` §4). At most one is ever shown per product.
 */
@Composable
public fun GsLabelBadge(
    text: String,
    tier: GsLabelTier,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when (tier) {
            GsLabelTier.Urgency -> GsTheme.colorScheme.surfaceInk
            GsLabelTier.Informational -> GsTheme.colorScheme.surface
            GsLabelTier.Unknown -> Color.Transparent
        }
    val contentColor =
        when (tier) {
            GsLabelTier.Urgency -> GsTheme.colorScheme.onSurfaceInk
            GsLabelTier.Informational -> GsTheme.colorScheme.textPrimary
            GsLabelTier.Unknown -> GsTheme.colorScheme.textSecondary
        }
    val pillShape = GsTheme.shapes.chipPill

    Box(
        modifier =
            modifier
                .clip(pillShape)
                .background(containerColor)
                .let { base ->
                    if (tier == GsLabelTier.Unknown) {
                        base.border(UNKNOWN_BORDER_WIDTH, GsTheme.colorScheme.borderStrong, pillShape)
                    } else {
                        base
                    }
                }.padding(horizontal = BADGE_HORIZONTAL_PADDING, vertical = BADGE_VERTICAL_PADDING),
    ) {
        Text(
            text = text,
            style = GsTheme.typography.label,
            color = contentColor,
        )
    }
}
