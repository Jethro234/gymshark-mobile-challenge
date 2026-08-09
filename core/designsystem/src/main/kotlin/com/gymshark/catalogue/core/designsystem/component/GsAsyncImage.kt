package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gymshark.catalogue.core.designsystem.R
import com.gymshark.catalogue.core.designsystem.theme.GsTheme

private const val CROSSFADE_DURATION_MILLIS = 200
private const val HAIRLINE_WIDTH_DP = 0.5f
private const val SHIMMER_MIN_ALPHA = 0.6f
private const val SHIMMER_MAX_ALPHA = 1f
private const val SHIMMER_DURATION_MILLIS = 900
private const val GLYPH_BOX_FRACTION = 0.34f
private const val GLYPH_STROKE_FRACTION = 0.1f
private const val GLYPH_SUN_RADIUS_FRACTION = 0.12f
private const val GLYPH_SUN_CENTER_FRACTION = 0.28f
private const val GLYPH_PEAK_X_FRACTION = 0.5f
private const val GLYPH_PEAK_Y_FRACTION = 0.4f
private const val GLYPH_BASE_LEFT_Y_FRACTION = 0.8f
private const val GLYPH_BASE_RIGHT_Y_FRACTION = 0.85f

/**
 * Thin wrapper around [coil3.compose.AsyncImage] — not `SubcomposeAsyncImage`, which is
 * measurably more expensive in a scrolling grid (`docs/ARCHITECTURE.md` §12).
 *
 * All three states — loading, loaded, error — share one [shape] instance so a mismatch never
 * produces a corner pop when an image resolves mid-scroll. [aspectRatio] is reserved before
 * the image loads, so the grid never reflows once it does; Coil's own constraint-based size
 * resolution — not `Size.ORIGINAL` — keeps a large source image from decoding at full
 * resolution into a small cell.
 */
@Composable
public fun GsAsyncImage(
    model: String?,
    contentDescription: String?,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    shape: Shape = GsTheme.shapes.card,
) {
    var state by remember(model) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }

    Box(
        modifier =
            modifier
                .aspectRatio(aspectRatio)
                .clip(shape)
                .background(GsTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(model)
                    .crossfade(CROSSFADE_DURATION_MILLIS)
                    .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { state = it },
        )

        when (state) {
            is AsyncImagePainter.State.Error ->
                GsAsyncImageErrorFallback(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .border(HAIRLINE_WIDTH_DP.dp, GsTheme.colorScheme.border, shape),
                )

            is AsyncImagePainter.State.Loading, AsyncImagePainter.State.Empty ->
                GsAsyncImageShimmer(
                    modifier = Modifier.fillMaxSize(),
                )

            is AsyncImagePainter.State.Success -> Unit
        }
    }
}

@Composable
private fun GsAsyncImageShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gs-image-shimmer")
    val alpha by transition.animateFloat(
        initialValue = SHIMMER_MIN_ALPHA,
        targetValue = SHIMMER_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(SHIMMER_DURATION_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "gs-image-shimmer-alpha",
    )
    Box(
        modifier =
            modifier
                .alpha(alpha)
                .background(GsTheme.colorScheme.surface),
    )
}

@Composable
private fun GsAsyncImageErrorFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(GsTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        GsPhotoOffGlyph(modifier = Modifier.align(Alignment.Center))
        Text(
            text = stringResource(R.string.gs_image_unavailable),
            style = GsTheme.typography.label,
            color = GsTheme.colorScheme.textMuted,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clearAndSetSemantics { },
        )
    }
}

/** A hand-drawn photo-off glyph — purely decorative, no icon-library dependency. */
@Composable
private fun GsPhotoOffGlyph(modifier: Modifier = Modifier) {
    val colour = GsTheme.colorScheme.textDisabled
    Canvas(
        modifier =
            modifier
                .fillMaxSize(GLYPH_BOX_FRACTION)
                .aspectRatio(1f),
    ) {
        val strokeWidth = size.minDimension * GLYPH_STROKE_FRACTION
        drawCircle(
            color = colour,
            radius = size.minDimension * GLYPH_SUN_RADIUS_FRACTION,
            center = Offset(size.width * GLYPH_SUN_CENTER_FRACTION, size.height * GLYPH_SUN_CENTER_FRACTION),
        )
        drawLine(
            color = colour,
            start = Offset(0f, size.height * GLYPH_BASE_LEFT_Y_FRACTION),
            end = Offset(size.width * GLYPH_PEAK_X_FRACTION, size.height * GLYPH_PEAK_Y_FRACTION),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = colour,
            start = Offset(size.width * GLYPH_PEAK_X_FRACTION, size.height * GLYPH_PEAK_Y_FRACTION),
            end = Offset(size.width, size.height * GLYPH_BASE_RIGHT_Y_FRACTION),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = colour,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth,
        )
    }
}
