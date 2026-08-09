package com.gymshark.catalogue.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

private const val NORMAL_TEXT_MINIMUM_RATIO = 4.5

/**
 * Every real foreground-text/background pairing used by the app, asserted against WCAG AA's
 * 4.5:1 normal-text threshold. `border`/`borderStrong` (decorative separators, WCAG 1.4.11
 * exempt) and `textDisabled` (inactive-control text, WCAG 1.4.3 exempt) are deliberately
 * absent — see `docs/DESIGN.md` §1.
 */
private class Pairing(
    val description: String,
    val foreground: Color,
    val background: Color,
)

private fun pairings(scheme: GsColorScheme): List<Pairing> =
    listOf(
        Pairing("textPrimary on background (titles, prices)", scheme.textPrimary, scheme.background),
        Pairing("textPrimary on surface (informational label badge)", scheme.textPrimary, scheme.surface),
        Pairing("textSecondary on background (body copy)", scheme.textSecondary, scheme.background),
        Pairing("textMuted on background (colourway, eyebrow)", scheme.textMuted, scheme.background),
        Pairing("textMuted on surface (image error caption)", scheme.textMuted, scheme.surface),
        Pairing("onSurfaceInk on surfaceInk (urgency label badge)", scheme.onSurfaceInk, scheme.surfaceInk),
        Pairing("sale on background (discounted price)", scheme.sale, scheme.background),
        Pairing("onSuccess on success (material chip)", scheme.onSuccess, scheme.success),
    )

internal class TokenContrastTest {
    @Test
    fun `every light theme text pairing meets WCAG AA`() {
        assertAll(
            pairings(LightGsColorScheme).map { pairing ->
                Executable {
                    val ratio = contrastRatio(pairing.foreground, pairing.background)
                    assertTrue(
                        ratio >= NORMAL_TEXT_MINIMUM_RATIO,
                        "${pairing.description}: $ratio:1, below $NORMAL_TEXT_MINIMUM_RATIO:1",
                    )
                }
            },
        )
    }

    @Test
    fun `every dark theme text pairing meets WCAG AA`() {
        assertAll(
            pairings(DarkGsColorScheme).map { pairing ->
                Executable {
                    val ratio = contrastRatio(pairing.foreground, pairing.background)
                    assertTrue(
                        ratio >= NORMAL_TEXT_MINIMUM_RATIO,
                        "${pairing.description}: $ratio:1, below $NORMAL_TEXT_MINIMUM_RATIO:1",
                    )
                }
            },
        )
    }
}
