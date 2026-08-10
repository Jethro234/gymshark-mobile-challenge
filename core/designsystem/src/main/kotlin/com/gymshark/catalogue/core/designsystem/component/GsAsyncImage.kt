package com.gymshark.catalogue.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

/** Used only when the payload supplies no usable dimensions — a sensible portrait fallback. */
private const val DEFAULT_ASPECT_RATIO = 0.75f
private const val ERROR_ICON_FRACTION = 0.5f

/**
 * Thin wrapper around [coil3.compose.AsyncImage] — not `SubcomposeAsyncImage`, which is
 * measurably more expensive in a scrolling grid (`docs/ARCHITECTURE.md` §12).
 *
 * All three states — loading, loaded, error — share one [shape] instance so a mismatch never
 * produces a corner pop when an image resolves mid-scroll. The aspect ratio is reserved from
 * [contentWidth]/[contentHeight] — the payload's own media dimensions — before the image
 * loads, so the grid never reflows once it does; when neither is supplied, a fixed portrait
 * fallback is reserved instead. Coil's own constraint-based size resolution — not
 * `Size.ORIGINAL` — keeps a large source image from decoding at full resolution into a small
 * cell.
 */
@Composable
public fun GsAsyncImage(
    model: String?,
    contentDescription: String?,
    contentWidth: Int?,
    contentHeight: Int?,
    modifier: Modifier = Modifier,
    shape: Shape = GsTheme.shapes.card,
    showErrorLabel: Boolean = true,
    isLoading: Boolean = false,
    memoryCacheKey: String? = null,
    placeholderMemoryCacheKey: String? = null,
) {
    var state by remember(model) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val aspectRatio =
        if (contentWidth != null && contentHeight != null && contentHeight > 0) {
            contentWidth.toFloat() / contentHeight.toFloat()
        } else {
            DEFAULT_ASPECT_RATIO
        }

    Box(
        modifier =
            modifier
                .aspectRatio(aspectRatio)
                .clip(shape)
                .background(GsTheme.colorScheme.surface),
    ) {
        // Coil treats a null request `data` as an immediate NullRequestDataException, not
        // "still loading" — so a caller that hasn't resolved a real url yet (as opposed to a
        // product genuinely having no image) must say so explicitly via [isLoading], or it
        // would flash the error fallback instead. Nothing is drawn in that case: the `surface`
        // fill this Box already carries *is* the placeholder.
        if (!isLoading) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(model)
                        .crossfade(CROSSFADE_DURATION_MILLIS)
                        // Coil's default memory cache key folds in the resolved display size,
                        // so the same photo cached for a small grid cell is a miss for a
                        // full-width hero. [memoryCacheKey] pins an entry under a
                        // size-independent key; [placeholderMemoryCacheKey] reads that entry
                        // back so a larger request can draw the already-decoded smaller
                        // bitmap immediately instead of an empty box while it loads.
                        .memoryCacheKey(memoryCacheKey)
                        .placeholderMemoryCacheKey(placeholderMemoryCacheKey)
                        .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state = it },
            )

            when (state) {
                is AsyncImagePainter.State.Error ->
                    GsAsyncImageErrorFallback(
                        showLabel = showErrorLabel,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .border(HAIRLINE_WIDTH_DP.dp, GsTheme.colorScheme.border, shape),
                    )

                // Nothing is drawn over the `surface` fill while loading, and Coil draws its
                // own placeholder there when [placeholderMemoryCacheKey] resolves to one.
                is AsyncImagePainter.State.Loading,
                AsyncImagePainter.State.Empty,
                is AsyncImagePainter.State.Success,
                -> Unit
            }
        }
    }
}

@Composable
private fun GsAsyncImageErrorFallback(
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(GsTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_broken_image),
            contentDescription = null,
            tint = GsTheme.colorScheme.textDisabled,
            modifier =
                Modifier
                    .fillMaxSize(ERROR_ICON_FRACTION)
                    .aspectRatio(1f),
        )
        // Too small a target (e.g. the 64dp detail-screen thumbnails) for the caption to fit
        // without wrapping over the icon — the icon alone still reads as "no image" there.
        if (showLabel) {
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
}
