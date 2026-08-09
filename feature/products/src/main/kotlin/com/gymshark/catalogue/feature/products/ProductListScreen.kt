package com.gymshark.catalogue.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymshark.catalogue.core.designsystem.component.GsEyebrowText
import com.gymshark.catalogue.core.designsystem.component.GsLabelTier
import com.gymshark.catalogue.core.designsystem.component.GsProductCard
import com.gymshark.catalogue.core.designsystem.theme.GsTheme
import com.gymshark.catalogue.core.model.ErrorCause

/**
 * Stateful entry point — resolves the ViewModel and restores grid scroll position across
 * navigation and process death.
 */
@Composable
fun ProductListScreen(
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

    ProductListScreen(
        uiState = uiState,
        gridState = gridState,
        onProductClick = onProductClick,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
internal fun ProductListScreen(
    uiState: ProductListUiState,
    gridState: LazyGridState,
    onProductClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is ProductListUiState.Loading -> LoadingState(modifier)
        is ProductListUiState.Empty -> EmptyState(modifier)
        is ProductListUiState.Error -> ErrorState(uiState.cause, onRetry, modifier)
        is ProductListUiState.Content ->
            ContentState(
                state = uiState,
                gridState = gridState,
                onProductClick = onProductClick,
                onRefresh = onRefresh,
                modifier = modifier,
            )
    }
}

@Composable
private fun ContentState(
    state: ProductListUiState.Content,
    gridState: LazyGridState,
    onProductClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        val spacing = GsTheme.spacing
        val screenGutterInsets =
            WindowInsets(
                left = spacing.screenGutter,
                top = spacing.screenGutter,
                right = spacing.screenGutter,
                bottom = spacing.screenGutter,
            )

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMN_COUNT),
            state = gridState,
            contentPadding = WindowInsets.safeDrawing.add(screenGutterInsets).asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gridGutter),
            verticalArrangement = Arrangement.spacedBy(spacing.gridGutter),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                ProductListHeader(title = state.screenTitle, countLabel = state.countLabel)
            }

            items(
                items = state.products,
                key = { it.id },
                contentType = { "product" },
            ) { product ->
                GsProductCard(
                    imageUrl = product.imageUrl,
                    imageAlt = product.imageAlt,
                    title = product.title,
                    colourway = product.colourway,
                    price = product.price,
                    compareAtPrice = product.compareAtPrice,
                    badgeText = product.badgeText,
                    badgeTier = product.badgeTier ?: GsLabelTier.Informational,
                    onClick = { onProductClick(product.id) },
                )
            }
        }
    }
}

@Composable
private fun ProductListHeader(
    title: String,
    countLabel: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GsTheme.spacing
    Column(modifier = modifier.padding(bottom = spacing.space16)) {
        Wordmark(modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(
            text = title,
            style = GsTheme.typography.displayScreen,
            color = GsTheme.colorScheme.textPrimary,
            modifier = Modifier.padding(top = spacing.space20),
        )
        GsEyebrowText(
            text = countLabel,
            modifier = Modifier.padding(top = spacing.space8),
        )
    }
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.product_list_wordmark),
        style = MaterialTheme.typography.titleMedium,
        color = GsTheme.colorScheme.textPrimary,
        modifier = modifier,
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Wordmark(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = GsTheme.spacing.space20))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Wordmark(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = GsTheme.spacing.space20))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.product_list_empty),
                style = GsTheme.typography.body,
                color = GsTheme.colorScheme.textSecondary,
            )
        }
    }
}

@Composable
private fun ErrorState(
    cause: ErrorCause,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Wordmark(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = GsTheme.spacing.space20))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(cause.toMessageRes()),
                    style = GsTheme.typography.body,
                    color = GsTheme.colorScheme.textSecondary,
                )
                TextButton(onClick = onRetry, modifier = Modifier.padding(top = GsTheme.spacing.space8)) {
                    Text(stringResource(R.string.product_list_retry))
                }
            }
        }
    }
}

private fun ErrorCause.toMessageRes(): Int =
    when (this) {
        ErrorCause.NoConnection -> R.string.product_list_error_no_connection
        ErrorCause.Server -> R.string.product_list_error_server
        ErrorCause.Malformed, ErrorCause.NotFound, ErrorCause.Unknown -> R.string.product_list_error_generic
    }

private const val GRID_COLUMN_COUNT = 2
