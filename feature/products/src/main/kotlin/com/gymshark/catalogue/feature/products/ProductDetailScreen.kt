package com.gymshark.catalogue.feature.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.gymshark.catalogue.core.designsystem.component.GsAsyncImage
import com.gymshark.catalogue.core.designsystem.component.GsEyebrowText
import com.gymshark.catalogue.core.designsystem.component.GsLabelBadge
import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.designsystem.theme.GsTheme
import com.gymshark.catalogue.core.model.ErrorCause
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long to wait for a shared-element transition to start before giving up on it — covers
 * navigation with no grid counterpart to match (process death, a deep link straight into this
 * screen). Sized just above [HERO_TRANSFORM_DURATION_MILLIS] itself, now that duration is a
 * fixed, known quantity rather than a spring with no fixed settle time.
 */
private const val SHARED_ELEMENT_MATCH_TIMEOUT_MILLIS = HERO_TRANSFORM_DURATION_MILLIS + 150L
private const val DETAILS_FADE_DURATION_MILLIS = 200

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onProductNotFoundBackToList: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    initialImageUrl: String? = null,
    initialImageWidth: Int? = null,
    initialImageHeight: Int? = null,
    viewModel: ProductDetailViewModel =
        hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(productId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProductDetailScreen(
        productId = productId,
        uiState = uiState,
        onBackClick = onBackClick,
        onProductNotFoundBackToList = onProductNotFoundBackToList,
        onRetry = viewModel::retry,
        onSelectSize = viewModel::selectSize,
        sharedTransitionScope = sharedTransitionScope,
        modifier = modifier,
        initialImageUrl = initialImageUrl,
        initialImageWidth = initialImageWidth,
        initialImageHeight = initialImageHeight,
    )
}

/**
 * The hero image is hoisted above `when (uiState)` and rendered for every state except
 * [ProductDetailUiState.Error] — it must exist on the very first frame, before the product
 * fetch resolves, or there is nothing for the grid's outgoing image to match against
 * (`docs/DESIGN.md` §6). It's pinned above the scrolling content rather than scrolling away
 * with it, since it now needs to survive `Loading` → `Content` without being torn down.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ProductDetailScreen(
    productId: String,
    uiState: ProductDetailUiState,
    onBackClick: () -> Unit,
    onProductNotFoundBackToList: () -> Unit,
    onRetry: () -> Unit,
    onSelectSize: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    initialImageUrl: String? = null,
    initialImageWidth: Int? = null,
    initialImageHeight: Int? = null,
) {
    val content = uiState as? ProductDetailUiState.Content
    var selectedMediaIndex by rememberSaveable(content?.product?.media) { mutableIntStateOf(0) }
    val selectedMedia = content?.product?.media?.getOrNull(selectedMediaIndex)

    // Until the fetch resolves, fall back to the thumbnail the grid card was already showing
    // (passed through the route — see AppRoute.ProductDetail). It's the same asset media[0]
    // resolves to, so Coil serves it from memory and the expanding hero draws the real photo
    // rather than an empty box for the length of the transition.
    val heroUrl = selectedMedia?.url ?: initialImageUrl
    val heroWidth = selectedMedia?.width ?: initialImageWidth
    val heroHeight = selectedMedia?.height ?: initialImageHeight

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        BackRow(onBackClick = onBackClick)

        if (uiState !is ProductDetailUiState.Error) {
            HeroImage(
                productId = productId,
                imageUrl = heroUrl,
                imageWidth = heroWidth,
                imageHeight = heroHeight,
                // Only a genuine "nothing to draw yet" — a resolved product with no image at
                // all must fall through to the error fallback, not sit on a blank fill forever.
                isLoading = heroUrl == null && content == null,
                contentDescription = selectedMedia?.alt ?: content?.product?.title,
                badgeText = content?.product?.badgeText,
                badgeTier = content?.product?.badgeTier,
                sharedTransitionScope = sharedTransitionScope,
            )
        }

        when (uiState) {
            is ProductDetailUiState.Loading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is ProductDetailUiState.Error ->
                ErrorState(
                    cause = uiState.cause,
                    onRetry = onRetry,
                    onBackToList = onProductNotFoundBackToList,
                )

            is ProductDetailUiState.Content ->
                ContentState(
                    state = uiState,
                    selectedMediaIndex = selectedMediaIndex,
                    onSelectMediaIndex = { selectedMediaIndex = it },
                    onSelectSize = onSelectSize,
                    sharedTransitionScope = sharedTransitionScope,
                )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HeroImage(
    productId: String,
    imageUrl: String?,
    imageWidth: Int?,
    imageHeight: Int?,
    isLoading: Boolean,
    contentDescription: String?,
    badgeText: String?,
    badgeTier: GsLabelTier?,
    sharedTransitionScope: SharedTransitionScope,
) {
    val spacing = GsTheme.spacing
    with(sharedTransitionScope) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenGutter),
        ) {
            GsAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentWidth = imageWidth,
                contentHeight = imageHeight,
                shape = GsTheme.shapes.hero,
                isLoading = isLoading,
                // The grid card cached this same photo under its url; drawing it while the
                // full-width request loads is what keeps the expanding hero from being empty
                // on a product's first visit (docs/DESIGN.md §6).
                placeholderMemoryCacheKey = imageUrl,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            rememberSharedContentState(key = productId),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            boundsTransform = HeroBoundsTransform,
                        ),
            )
            if (badgeText != null) {
                GsLabelBadge(
                    text = badgeText,
                    tier = badgeTier ?: GsLabelTier.Informational,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(spacing.space8 + 2.dp),
                )
            }
        }
    }
}

@Composable
private fun BackRow(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.product_detail_back),
            tint = GsTheme.colorScheme.textPrimary,
        )
    }
}

@Composable
private fun ErrorState(
    cause: ErrorCause,
    onRetry: () -> Unit,
    onBackToList: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(cause.toDetailMessageRes()),
                style = GsTheme.typography.body,
                color = GsTheme.colorScheme.textSecondary,
            )
            if (cause == ErrorCause.NotFound) {
                TextButton(onClick = onBackToList, modifier = Modifier.padding(top = GsTheme.spacing.space8)) {
                    Text(stringResource(R.string.product_detail_back_to_list))
                }
            } else {
                TextButton(onClick = onRetry, modifier = Modifier.padding(top = GsTheme.spacing.space8)) {
                    Text(stringResource(R.string.product_list_retry))
                }
            }
        }
    }
}

private fun ErrorCause.toDetailMessageRes(): Int =
    when (this) {
        ErrorCause.NoConnection -> R.string.product_list_error_no_connection
        ErrorCause.Server -> R.string.product_list_error_server
        ErrorCause.NotFound -> R.string.product_detail_not_found
        ErrorCause.Malformed, ErrorCause.Unknown -> R.string.product_list_error_generic
    }

/**
 * "Remaining details" — everything but the hero image, gated behind [detailsVisible] so it
 * fades in once the shared-element transform lands rather than popping in underneath it.
 * [detailsVisible] must be `rememberSaveable` and the effect must short-circuit once already
 * `true`: a rotation recreates this composition without ever starting a shared-element
 * transition (nothing to match against), so without the short-circuit the already-visible
 * content would blank out and only reappear after [SHARED_ELEMENT_MATCH_TIMEOUT_MILLIS]
 * (`docs/ARCHITECTURE.md` §11.2 — the class of config-change bug this project already
 * guards against).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ContentState(
    state: ProductDetailUiState.Content,
    selectedMediaIndex: Int,
    onSelectMediaIndex: (Int) -> Unit,
    onSelectSize: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
) {
    val product = state.product
    val spacing = GsTheme.spacing
    var detailsVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!detailsVisible) {
            withTimeoutOrNull(SHARED_ELEMENT_MATCH_TIMEOUT_MILLIS) {
                snapshotFlow { sharedTransitionScope.isTransitionActive }.first { it }
                snapshotFlow { sharedTransitionScope.isTransitionActive }.first { !it }
            }
            detailsVisible = true
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(PRODUCT_DETAIL_ROOT_TAG),
    ) {
        if (product.media.size > 1) {
            ThumbnailStrip(
                media = product.media,
                selectedIndex = selectedMediaIndex,
                onSelect = onSelectMediaIndex,
                fallbackContentDescription = product.title,
                modifier = Modifier.padding(top = spacing.space8 + 2.dp, start = spacing.screenGutter),
            )
        }

        AnimatedVisibility(
            visible = detailsVisible,
            enter = fadeIn(tween(DETAILS_FADE_DURATION_MILLIS)),
            exit = ExitTransition.None,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = spacing.screenGutter),
            ) {
                Text(
                    text = product.title,
                    style = GsTheme.typography.titleProduct,
                    color = GsTheme.colorScheme.textPrimary,
                    modifier = Modifier.padding(top = spacing.heroToContentGap),
                )
                Text(
                    text = product.subtitle,
                    style = GsTheme.typography.label,
                    color = GsTheme.colorScheme.textMuted,
                    modifier = Modifier.padding(top = spacing.space4),
                )
                PriceRow(
                    price = product.price,
                    compareAtPrice = product.compareAtPrice,
                    modifier = Modifier.padding(top = spacing.space8),
                )

                GsEyebrowText(
                    text = stringResource(R.string.product_detail_size_label),
                    modifier = Modifier.padding(top = spacing.space20),
                )
                SizeChipRow(
                    sizes = product.sizes,
                    selectedSize = state.selectedSize,
                    onSelectSize = onSelectSize,
                    modifier = Modifier.padding(top = spacing.space8),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = spacing.space24),
                    color = GsTheme.colorScheme.border,
                )

                if (product.materialChipTexts.isNotEmpty()) {
                    MaterialChipRow(
                        texts = product.materialChipTexts,
                        modifier = Modifier.padding(top = spacing.space16),
                    )
                }

                DescriptionSection(heading = product.heading, bodyHtml = product.bodyHtml)
            }
        }
    }
}

/**
 * `remember(bodyHtml)` runs the HTML-to-styled-text conversion once per description, not on
 * every recomposition (`docs/ARCHITECTURE.md` §6). `heading` renders with the `eyebrow` token,
 * never an HTML heading tag, so it can't override the type scale.
 */
@Composable
private fun DescriptionSection(
    heading: String?,
    bodyHtml: String,
) {
    val body = remember(bodyHtml) { AnnotatedString.fromHtml(bodyHtml) }
    Column {
        if (heading != null) {
            GsEyebrowText(
                text = heading,
                modifier = Modifier.padding(top = GsTheme.spacing.space24),
            )
        }
        Text(
            text = body,
            style = GsTheme.typography.body,
            color = GsTheme.colorScheme.textSecondary,
            modifier = Modifier.padding(top = GsTheme.spacing.space8, bottom = GsTheme.spacing.space32),
        )
    }
}

const val PRODUCT_DETAIL_ROOT_TAG = "productDetailRootTag"
