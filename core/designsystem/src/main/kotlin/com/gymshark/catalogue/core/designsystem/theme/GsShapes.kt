package com.gymshark.catalogue.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from `docs/DESIGN.md` §3. No elevation anywhere — separation is whitespace and hairlines. */
@Immutable
public data class GsShapes(
    public val card: Shape,
    public val hero: Shape,
    public val thumbnail: Shape,
    public val chipPill: Shape,
    public val sheet: Shape,
)

public val GsDefaultShapes: GsShapes =
    GsShapes(
        card = RoundedCornerShape(20.dp),
        hero = RoundedCornerShape(22.dp),
        thumbnail = RoundedCornerShape(10.dp),
        chipPill = RoundedCornerShape(999.dp),
        sheet = RoundedCornerShape(26.dp),
    )
