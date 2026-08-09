package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.gymshark.catalogue.core.designsystem.theme.GsTheme

/**
 * Renders [text] uppercase in the `eyebrow` style without ever upper-casing the string itself:
 * uppercasing content breaks screen readers (which spell out short tokens) and breaks languages
 * without case. The authored [text] is exposed as the accessibility content description; only
 * the drawn glyphs are transformed.
 */
@Composable
public fun GsEyebrowText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GsTheme.colorScheme.textMuted,
    style: TextStyle = GsTheme.typography.eyebrow,
) {
    BasicText(
        text = text.uppercase(),
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
        style = style.copy(color = color),
    )
}
