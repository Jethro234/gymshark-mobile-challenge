package com.gymshark.catalogue.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour tokens. Both [LightGsColorScheme] and [DarkGsColorScheme] are complete,
 * separately authored sets — dark is never derived by inverting light. See `docs/DESIGN.md`
 * §1 for the exact values and the contrast rationale.
 */
@Immutable
public data class GsColorScheme(
    public val background: Color,
    public val surface: Color,
    public val surfaceInk: Color,
    public val onSurfaceInk: Color,
    public val textPrimary: Color,
    public val textSecondary: Color,
    public val textMuted: Color,
    public val textDisabled: Color,
    public val border: Color,
    public val borderStrong: Color,
    public val sale: Color,
    public val success: Color,
    public val onSuccess: Color,
)

public val LightGsColorScheme: GsColorScheme =
    GsColorScheme(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF5F5F3),
        surfaceInk = Color(0xFF0B0B0B),
        onSurfaceInk = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF0B0B0B),
        textSecondary = Color(0xFF5F5F5B),
        textMuted = Color(0xFF656561),
        textDisabled = Color(0xFFB4B4AE),
        border = Color(0xFFE6E6E2),
        borderStrong = Color(0xFFC8C8C2),
        sale = Color(0xFFA32D2D),
        success = Color(0xFFE1F5EE),
        onSuccess = Color(0xFF0F6E56),
    )

public val DarkGsColorScheme: GsColorScheme =
    GsColorScheme(
        background = Color(0xFF0B0B0B),
        surface = Color(0xFF1F1F1D),
        surfaceInk = Color(0xFFFFFFFF),
        onSurfaceInk = Color(0xFF0B0B0B),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA5A5A2),
        textMuted = Color(0xFF8A8A84),
        textDisabled = Color(0xFF4A4A48),
        border = Color(0xFF2A2A28),
        borderStrong = Color(0xFF4A4A48),
        sale = Color(0xFFF09595),
        success = Color(0xFF0F3D31),
        onSuccess = Color(0xFF9FE1CB),
    )
