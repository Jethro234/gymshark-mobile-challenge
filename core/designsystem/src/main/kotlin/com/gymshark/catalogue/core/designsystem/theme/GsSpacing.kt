package com.gymshark.catalogue.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale from `docs/DESIGN.md` §3: 4/8/12/16/20/24/32dp. Nothing off-scale except
 * two named exceptions called out explicitly in the design document: [gridGutter] (§5) and
 * [heroToContentGap] (§5, the detail screen's thumbnail-to-title gap).
 */
@Immutable
public data class GsSpacing(
    public val space4: Dp,
    public val space8: Dp,
    public val space12: Dp,
    public val space16: Dp,
    public val space20: Dp,
    public val space24: Dp,
    public val space32: Dp,
    public val screenGutter: Dp,
    public val gridGutter: Dp,
    /** Detail screen only: the gap between the thumbnail strip and the title block. */
    public val heroToContentGap: Dp,
)

public val GsDefaultSpacing: GsSpacing =
    GsSpacing(
        space4 = 4.dp,
        space8 = 8.dp,
        space12 = 12.dp,
        space16 = 16.dp,
        space20 = 20.dp,
        space24 = 24.dp,
        space32 = 32.dp,
        screenGutter = 20.dp,
        gridGutter = 14.dp,
        heroToContentGap = 18.dp,
    )
