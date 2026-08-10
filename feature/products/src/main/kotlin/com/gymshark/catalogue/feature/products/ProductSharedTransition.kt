package com.gymshark.catalogue.feature.products

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween

// Motion shared by both halves of the grid-to-detail hero transition (docs/DESIGN.md §6). It
// lives in one place because a `sharedElement` match has two call sites — the grid card and the
// detail hero — and they must agree: if each supplied its own `boundsTransform`, which one won
// would depend on which side the API happened to read.

/**
 * Replaces the shared-element default (`spring(stiffness = StiffnessMediumLow)`), whose settle
 * time is neither fixed nor fast.
 */
internal const val HERO_TRANSFORM_DURATION_MILLIS = 350

/**
 * How long the detail screen's content takes to fade out when popped. Public because the
 * `NavDisplay` pop transition that performs that fade is configured in `:app`, while the
 * matching delay below is applied here — one constant rather than the same number written
 * twice in two modules.
 */
const val PRODUCT_DETAIL_EXIT_DURATION_MILLIS: Int = 120

/**
 * Sequences the two directions differently, which is the whole point of it being a
 * [BoundsTransform] rather than a plain spec: the transform receives the bounds it is moving
 * between, so "am I growing or shrinking" — and therefore "is this a forward navigation or a
 * back one" — is answerable without any navigation state being threaded down here.
 *
 * Growing runs immediately: the image leads, and the detail content fades in behind it once it
 * lands. Shrinking waits [PRODUCT_DETAIL_EXIT_DURATION_MILLIS] first, so the detail content is
 * already gone before the image starts moving, rather than the two overlapping. The hero is
 * rendered in the shared-element overlay throughout, so it stays fully opaque while the screen
 * beneath it fades.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val HeroBoundsTransform: BoundsTransform =
    BoundsTransform { initialBounds, targetBounds ->
        val shrinking = targetBounds.width < initialBounds.width
        tween(
            durationMillis = HERO_TRANSFORM_DURATION_MILLIS,
            delayMillis = if (shrinking) PRODUCT_DETAIL_EXIT_DURATION_MILLIS else 0,
        )
    }
