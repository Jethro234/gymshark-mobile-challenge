package com.gymshark.catalogue.feature.products

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymshark.catalogue.core.designsystem.component.GsEyebrowText
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
        modifier = modifier,
    )
}

@Composable
internal fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    onBackClick: () -> Unit,
    onProductNotFoundBackToList: () -> Unit,
    onRetry: () -> Unit,
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

            is ProductDetailUiState.Content -> ContentState(state = uiState)
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
private fun ContentState(state: ProductDetailUiState.Content) {
    val product = state.product
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = GsTheme.spacing.screenGutter),
    ) {
        DescriptionSection(heading = product.heading, bodyHtml = product.bodyHtml)
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
                modifier = Modifier.padding(top = GsTheme.spacing.space20),
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
