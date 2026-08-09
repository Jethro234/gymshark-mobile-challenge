package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gymshark.catalogue.core.designsystem.theme.GsTheme

private val CHIP_HORIZONTAL_PADDING = 11.dp
private val CHIP_VERTICAL_PADDING = 4.dp

/**
 * A sustainability-material chip (`docs/DESIGN.md` §4) — genuine material-provenance data,
 * rendered near the description rather than competing with the image's merchandising badge.
 * Uses the `success` token pair, freed up now that "back in stock" isn't a real label.
 */
@Composable
public fun GsMaterialChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(GsTheme.shapes.chipPill)
                .background(GsTheme.colorScheme.success)
                .padding(horizontal = CHIP_HORIZONTAL_PADDING, vertical = CHIP_VERTICAL_PADDING),
    ) {
        Text(
            text = text,
            style = GsTheme.typography.label,
            color = GsTheme.colorScheme.onSuccess,
        )
    }
}
