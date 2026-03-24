package io.engst.launcher.ui.grid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import io.engst.core.apps.launchActivity
import io.engst.launcher.core.launchAppDetails
import io.engst.launcher.core.launchAppRemovalRequest
import io.engst.launcher.core.launchShortcut
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.rememberScreenInfo
import org.koin.androidx.compose.koinViewModel

private val ICON_SIZE_DP = 60.dp
private val SPACING_DP = 12.dp

@Composable
fun AppGrid(
    modifier: Modifier = Modifier,
    isDefaultLauncher: Boolean = false,
    viewModel: GridViewModel = koinViewModel(),
    onNavigateToAppManager: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isInDragMode) {
        viewModel.onIntent(GridIntent.DragDropped)
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val screenInfo = rememberScreenInfo()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            handleEffect(
                effect = effect,
                context = context,
                onNavigateToAppManager = onNavigateToAppManager,
                onSetDefaultLauncher = onSetDefaultLauncher,
            )
        }
    }

    val displayGrid = state.displayGrid
    if (displayGrid == null || displayGrid.grid.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(108.dp),
                color = LocalWallpaperState.current.suggestedForegroundColor,
            )
        }
        return
    }

    val spacingPx = with(density) { SPACING_DP.roundToPx() }
    val pagerState = rememberPagerState { displayGrid.grid.size }
    val pagerContainerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { position ->
                        if (state.draggingAppId == null) {
                            val offset = with(density) { DpOffset(position.x.toDp(), position.y.toDp()) }
                            viewModel.onIntent(GridIntent.GridMenuRequested(offset))
                        }
                    },
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            val pageSize = remember(state.isInDragMode, spacingPx) {
                buildPageSize(isDragMode = state.isInDragMode, spacingPx = spacingPx)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onPlaced { coords -> pagerContainerCoordinates.value = coords },
            ) {
                val pagerContentPadding = remember(state.isInDragMode) {
                    if (state.isInDragMode) {
                        PaddingValues(horizontal = SPACING_DP, vertical = SPACING_DP)
                    } else {
                        PaddingValues(vertical = SPACING_DP)
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    contentPadding = pagerContentPadding,
                    beyondViewportPageCount = 1,
                    pageSize = pageSize,
                    snapPosition = if (state.isInDragMode) SnapPosition.Center else SnapPosition.Start,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    AppGridPage(
                        pageIndex = pageIndex,
                        page = displayGrid.grid[pageIndex],
                        columns = displayGrid.spec.cols,
                        iconSizeDp = ICON_SIZE_DP,
                        spacing = SPACING_DP,
                        draggingAppId = state.draggingAppId,
                        activeAppMenuIdentifier = state.activeAppMenuIdentifier,
                        density = density,
                        viewConfiguration = viewConfiguration,
                        screenInfo = screenInfo,
                        pagerContainerCoordinates = pagerContainerCoordinates.value,
                        pagerState = pagerState,
                        coroutineScope = coroutineScope,
                        onDragStarted = { appId -> viewModel.onIntent(GridIntent.DragStarted(appId)) },
                        onDragMovedToCell = { page, cell -> viewModel.onIntent(GridIntent.DragMovedToGridCell(page, cell)) },
                        onDragDropped = { viewModel.onIntent(GridIntent.DragDropped) },
                        onDragCancelled = { viewModel.onIntent(GridIntent.DragCancelled) },
                        onAppTapped = { app -> viewModel.onIntent(GridIntent.AppTapped(app)) },
                        onAppMenuRequested = { appId -> viewModel.onIntent(GridIntent.AppMenuRequested(appId)) },
                        onAppMenuDismissed = { viewModel.onIntent(GridIntent.AppMenuDismissed) },
                        onAppDetails = { app -> viewModel.onIntent(GridIntent.AppDetailsRequested(app)) },
                        onAppRemoval = { app -> viewModel.onIntent(GridIntent.AppRemovalRequested(app)) },
                        onShortcutLaunch = { shortcut -> viewModel.onIntent(GridIntent.ShortcutLaunchRequested(shortcut)) },
                    )
                }
            }

            if (pagerState.pageCount > 0) {
                PagerPageIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.fillMaxWidth().height(SPACING_DP),
                )
            }

            Spacer(Modifier.size(SPACING_DP))

            AppGridQuickBar(
                apps = displayGrid.bar,
                iconSizeDp = ICON_SIZE_DP,
                spacing = SPACING_DP,
                draggingAppId = state.draggingAppId,
                activeAppMenuIdentifier = state.activeAppMenuIdentifier,
                density = density,
                viewConfiguration = viewConfiguration,
                onDragStarted = { appId -> viewModel.onIntent(GridIntent.DragStarted(appId)) },
                onDragMovedToSlot = { index -> viewModel.onIntent(GridIntent.DragMovedToQuickBarSlot(index)) },
                onDragDropped = { viewModel.onIntent(GridIntent.DragDropped) },
                onDragCancelled = { viewModel.onIntent(GridIntent.DragCancelled) },
                onAppTapped = { app -> viewModel.onIntent(GridIntent.AppTapped(app)) },
                onAppMenuRequested = { appId -> viewModel.onIntent(GridIntent.AppMenuRequested(appId)) },
                onAppMenuDismissed = { viewModel.onIntent(GridIntent.AppMenuDismissed) },
                onAppDetails = { app -> viewModel.onIntent(GridIntent.AppDetailsRequested(app)) },
                onAppRemoval = { app -> viewModel.onIntent(GridIntent.AppRemovalRequested(app)) },
                onShortcutLaunch = { shortcut -> viewModel.onIntent(GridIntent.ShortcutLaunchRequested(shortcut)) },
            )
        }

        AppGridMenu(
            isVisible = state.isGridMenuVisible,
            offset = state.gridMenuOffset,
            isDefaultLauncher = isDefaultLauncher,
            currentGridSpec = displayGrid?.spec ?: GridSpec(4, 4),
            onDismissRequest = { viewModel.onIntent(GridIntent.GridMenuDismissed) },
            onAppsListRequested = { viewModel.onIntent(GridIntent.AppManagerOpenRequested) },
            onSetDefaultLauncherRequested = { viewModel.onIntent(GridIntent.DefaultLauncherSettingsOpenRequested) },
            onGridSpecSelected = { spec -> viewModel.onIntent(GridIntent.GridSpecChangeRequested(spec)) },
        )
    }
}

/** Isolates [PagerState.currentPage] reads so the parent does not recompose on page changes. */
@Composable
private fun PagerPageIndicator(
    pagerState: androidx.compose.foundation.pager.PagerState,
    modifier: Modifier = Modifier,
) {
    PageIndicator(
        count = pagerState.pageCount,
        currentIndex = pagerState.currentPage,
        modifier = modifier,
    )
}

private fun buildPageSize(isDragMode: Boolean, spacingPx: Int): PageSize =
    object : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int =
            if (isDragMode) availableSpace - spacingPx * 2 else availableSpace
    }

private fun handleEffect(
    effect: GridEffect,
    context: android.content.Context,
    onNavigateToAppManager: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
) {
    when (effect) {
        is GridEffect.LaunchApp -> context.launchActivity(effect.app.componentName)
        is GridEffect.OpenAppDetails -> context.launchAppDetails(effect.app.componentName.packageName)
        is GridEffect.RemoveApp -> context.launchAppRemovalRequest(effect.app.componentName.packageName)
        is GridEffect.LaunchShortcut -> context.launchShortcut(effect.shortcut)
        is GridEffect.OpenAppManager -> onNavigateToAppManager()
        is GridEffect.OpenDefaultLauncherSettings -> onSetDefaultLauncher()
        is GridEffect.NavigateToPage -> { /* handled by pager directly in AppGridPage */ }
    }
}
