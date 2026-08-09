package com.gymshark.catalogue.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalGsColorScheme = staticCompositionLocalOf { LightGsColorScheme }
private val LocalGsTypography = staticCompositionLocalOf { GsDefaultTypography }
private val LocalGsSpacing = staticCompositionLocalOf { GsDefaultSpacing }
private val LocalGsShapes = staticCompositionLocalOf { GsDefaultShapes }

/**
 * Root theme composable. Dynamic colour is never applied — [isDarkTheme] is the only input,
 * and both palettes are complete, separately authored token sets (`docs/DESIGN.md` §1).
 * Wraps a real M3 [MaterialTheme] so Material components used underneath (ripple, default
 * content colour) inherit the brand palette rather than M3's own defaults.
 */
@Composable
public fun GymsharkTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val gsColorScheme = if (isDarkTheme) DarkGsColorScheme else LightGsColorScheme
    val materialColorScheme =
        if (isDarkTheme) {
            darkColorScheme(
                primary = gsColorScheme.surfaceInk,
                onPrimary = gsColorScheme.onSurfaceInk,
                background = gsColorScheme.background,
                onBackground = gsColorScheme.textPrimary,
                surface = gsColorScheme.surface,
                onSurface = gsColorScheme.textPrimary,
                surfaceVariant = gsColorScheme.surface,
                onSurfaceVariant = gsColorScheme.textSecondary,
                outline = gsColorScheme.border,
                outlineVariant = gsColorScheme.borderStrong,
                error = gsColorScheme.sale,
                onError = gsColorScheme.background,
            )
        } else {
            lightColorScheme(
                primary = gsColorScheme.surfaceInk,
                onPrimary = gsColorScheme.onSurfaceInk,
                background = gsColorScheme.background,
                onBackground = gsColorScheme.textPrimary,
                surface = gsColorScheme.surface,
                onSurface = gsColorScheme.textPrimary,
                surfaceVariant = gsColorScheme.surface,
                onSurfaceVariant = gsColorScheme.textSecondary,
                outline = gsColorScheme.border,
                outlineVariant = gsColorScheme.borderStrong,
                error = gsColorScheme.sale,
                onError = gsColorScheme.background,
            )
        }

    CompositionLocalProvider(
        LocalGsColorScheme provides gsColorScheme,
        LocalGsTypography provides GsDefaultTypography,
        LocalGsSpacing provides GsDefaultSpacing,
        LocalGsShapes provides GsDefaultShapes,
    ) {
        MaterialTheme(colorScheme = materialColorScheme, content = content)
    }
}

/** Mirrors [MaterialTheme]'s `.colorScheme` / `.typography` access pattern for the brand tokens. */
public object GsTheme {
    public val colorScheme: GsColorScheme
        @Composable
        get() = LocalGsColorScheme.current

    public val typography: GsTypography
        @Composable
        get() = LocalGsTypography.current

    public val spacing: GsSpacing
        @Composable
        get() = LocalGsSpacing.current

    public val shapes: GsShapes
        @Composable
        get() = LocalGsShapes.current
}
