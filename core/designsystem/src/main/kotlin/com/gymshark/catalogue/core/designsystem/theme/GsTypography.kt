package com.gymshark.catalogue.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The app's single sans family in two weights, named by role. See `docs/DESIGN.md` §2.
 * `eyebrow` carries its uppercase presentation as letter-spacing/tracking only — the string
 * it is applied to must remain in its authored case; see `component.GsEyebrowText`.
 */
@Immutable
public data class GsTypography(
    public val displayScreen: TextStyle,
    public val titleProduct: TextStyle,
    public val titleCard: TextStyle,
    public val price: TextStyle,
    public val priceCard: TextStyle,
    public val body: TextStyle,
    public val label: TextStyle,
    public val eyebrow: TextStyle,
)

public val GsDefaultTypography: GsTypography =
    GsTypography(
        displayScreen =
            TextStyle(
                fontSize = 27.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.01).em,
            ),
        titleProduct =
            TextStyle(
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.005).em,
            ),
        titleCard =
            TextStyle(
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.em,
            ),
        price =
            TextStyle(
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.em,
            ),
        priceCard =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.em,
            ),
        body =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.em,
            ),
        label =
            TextStyle(
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.em,
            ),
        eyebrow =
            TextStyle(
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.13.em,
            ),
    )
