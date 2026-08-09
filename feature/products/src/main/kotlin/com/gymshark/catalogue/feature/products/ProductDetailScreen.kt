package com.gymshark.catalogue.feature.products

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymshark.catalogue.core.designsystem.component.GsAsyncImage
import com.gymshark.catalogue.core.designsystem.component.GsEyebrowText
import com.gymshark.catalogue.core.designsystem.component.GsLabelBadge
import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.designsystem.theme.GsTheme
import com.gymshark.catalogue.core.model.ErrorCause

@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onProductNotFoundBackToList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel =
        hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(productId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProductDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onProductNotFoundBackToList = onProductNotFoundBackToList,
        onRetry = viewModel::retry,
        onSelectSize = viewModel::selectSize,
        modifier = modifier,
    )
}

@Composable
internal fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    onBackClick: () -> Unit,
    onProductNotFoundBackToList: () -> Unit,
    onRetry: () -> Unit,
    onSelectSize: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackRow(onBackClick = onBackClick)

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
                ContentState(state = uiState, onSelectSize = onSelectSize)
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

@Composable
private fun ContentState(
    state: ProductDetailUiState.Content,
    onSelectSize: (String) -> Unit,
) {
    val product = state.product
    var selectedMediaIndex by rememberSaveable(product.media) { mutableIntStateOf(0) }
    val selectedMedia = product.media.getOrNull(selectedMediaIndex)
    val spacing = GsTheme.spacing

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenGutter)) {
            GsAsyncImage(
                model = selectedMedia?.url,
                contentDescription = selectedMedia?.alt ?: product.title,
                contentWidth = selectedMedia?.width,
                contentHeight = selectedMedia?.height,
                shape = GsTheme.shapes.hero,
                modifier = Modifier.fillMaxWidth(),
            )
            if (product.badgeText != null) {
                GsLabelBadge(
                    text = product.badgeText,
                    tier = product.badgeTier ?: GsLabelTier.Informational,
                    modifier = Modifier.align(Alignment.TopStart).padding(spacing.space8 + 2.dp),
                )
            }
        }

        if (product.media.size > 1) {
            ThumbnailStrip(
                media = product.media,
                selectedIndex = selectedMediaIndex,
                onSelect = { selectedMediaIndex = it },
                fallbackContentDescription = product.title,
                modifier = Modifier.padding(top = spacing.space8 + 2.dp, start = spacing.screenGutter),
            )
        }

        Column(modifier = Modifier.padding(horizontal = spacing.screenGutter)) {
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
