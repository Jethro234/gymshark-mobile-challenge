package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gymshark.catalogue.core.designsystem.R
import com.gymshark.catalogue.core.designsystem.theme.GsTheme

private val CHIP_MIN_TOUCH_TARGET = 48.dp
private val CHIP_HORIZONTAL_PADDING = 16.dp
private val CHIP_BORDER_WIDTH = 1.dp

public enum class GsSizeChipState { Selected, Available, OutOfStock }

/**
 * A single size chip. [GsSizeChipState.OutOfStock] carries its state as a `stateDescription`
 * rather than relying on the strikethrough alone, which a screen reader cannot see
 * (`docs/DESIGN.md` §4).
 */
@Composable
public fun GsSizeChip(
    text: String,
    state: GsSizeChipState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val isOutOfStock = state == GsSizeChipState.OutOfStock
    val containerColor = if (state == GsSizeChipState.Selected) GsTheme.colorScheme.surfaceInk else null
    val borderColor =
        when (state) {
            GsSizeChipState.Selected -> null
            GsSizeChipState.Available -> GsTheme.colorScheme.borderStrong
            GsSizeChipState.OutOfStock -> GsTheme.colorScheme.border
        }
    val contentColor =
        when (state) {
            GsSizeChipState.Selected -> GsTheme.colorScheme.onSurfaceInk
            GsSizeChipState.Available -> GsTheme.colorScheme.textPrimary
            GsSizeChipState.OutOfStock -> GsTheme.colorScheme.textDisabled
        }
    val pillShape = GsTheme.shapes.chipPill
    val outOfStockDescription = stringResource(R.string.gs_size_out_of_stock)

    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = CHIP_MIN_TOUCH_TARGET, minHeight = CHIP_MIN_TOUCH_TARGET)
                .selectable(
                    selected = state == GsSizeChipState.Selected,
                    enabled = !isOutOfStock,
                    onClick = onClick,
                    role = Role.RadioButton,
                ).semantics { if (isOutOfStock) stateDescription = outOfStockDescription }
                .clip(pillShape)
                .let { base -> if (containerColor != null) base.background(containerColor) else base }
                .let { base ->
                    if (borderColor != null) base.border(CHIP_BORDER_WIDTH, borderColor, pillShape) else base
                }.padding(horizontal = CHIP_HORIZONTAL_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = GsTheme.typography.label,
            color = contentColor,
            textDecoration = if (isOutOfStock) TextDecoration.LineThrough else null,
        )
    }
}
