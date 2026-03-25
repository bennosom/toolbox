package io.engst.launcher.ui.grid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.engst.core.scopedLogger
import io.engst.launcher.model.Grid
import io.engst.launcher.ui.grid.drag.GestureResult
import io.engst.launcher.ui.grid.drag.awaitLongPressOrSwipeOrTap
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.previewApps
import io.engst.launcher.ui.shared.previewPage
import io.engst.launcher.ui.shared.previewSpec
import io.engst.launcher.ui.shared.spacing
import org.koin.androidx.compose.koinViewModel

private val logger = scopedLogger("AppGrid")
val ICON_SIZE = 60.dp

@Composable
fun AppGrid(
    modifier: Modifier = Modifier,
    isDefaultLauncher: Boolean = false,
    viewModel: GridViewModel = koinViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  BackHandler(enabled = state.isInDragMode) { viewModel.onIntent(GridIntent.DragDropped) }
  AppGridContent(
      state = state,
      isDefaultLauncher = isDefaultLauncher,
      onIntent = viewModel::onIntent,
      modifier = modifier,
  )
}

@Composable
private fun AppGridContent(
    state: GridScreenState,
    isDefaultLauncher: Boolean,
    onIntent: (GridIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val viewConfiguration = LocalViewConfiguration.current
  val spacing = MaterialTheme.spacing
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

  val spacingPx = with(density) { spacing.medium.roundToPx() }
  val pagerState = rememberPagerState { displayGrid.grid.size }
  val pagerContainerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
  val gestureClaimedByTile = remember { mutableStateOf(false) }
  val pendingPageNavigationTarget = state.pendingPageNavigationTarget

  LaunchedEffect(pendingPageNavigationTarget) {
    val target = pendingPageNavigationTarget ?: return@LaunchedEffect
    if (target !in 0 until pagerState.pageCount || target == pagerState.currentPage) {
      onIntent(GridIntent.PageNavigationHandled)
      return@LaunchedEffect
    }
    pagerState.animateScrollToPage(target)
    onIntent(GridIntent.PageNavigationHandled)
  }
  Box(
      modifier =
          modifier
              .fillMaxSize()
              .testTag("app_grid")
              .pointerInput(Unit) {
                awaitEachGesture {
                  gestureClaimedByTile.value = false
                  val down =
                      awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                  val result =
                      awaitLongPressOrSwipeOrTap(down, viewConfiguration, PointerEventPass.Final)
                  if (result is GestureResult.LongPress) {
                    if (gestureClaimedByTile.value) {
                      logger.logDebug { "long press on background ignored — tile claimed gesture" }
                    } else if (state.draggingAppId == null) {
                      val position = down.position
                      val offset = with(density) { DpOffset(position.x.toDp(), position.y.toDp()) }
                      logger.logDebug { "long press on empty space — opening grid menu" }
                      onIntent(GridIntent.GridMenuRequested(offset))
                    }
                  }
                }
              }
              .homeScreenSwipeGestures(
                  onSwipeDown = { onIntent(GridIntent.SwipeDownDetected) },
                  onSwipeUp = { onIntent(GridIntent.SwipeUpDetected) },
              )
  ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val cellHeight =
          calculateCellHeight(
              totalHeight = maxHeight,
              rows = displayGrid.spec.rows,
              spacing = spacing.medium,
              barVisible = state.isBarVisible,
          )

      Column(Modifier.fillMaxSize()) {
        val pageSize =
            remember(state.isInDragMode, spacingPx) {
              buildPageSize(isDragMode = state.isInDragMode, spacingPx = spacingPx)
            }

        Box(
            modifier =
                Modifier.weight(1f).fillMaxWidth().onPlaced { coords ->
                  pagerContainerCoordinates.value = coords
                }
        ) {
          HorizontalPager(
              state = pagerState,
              beyondViewportPageCount = 1,
              pageSize = pageSize,
              snapPosition = if (state.isInDragMode) SnapPosition.Center else SnapPosition.Start,
              modifier = Modifier.fillMaxSize(),
          ) { pageIndex ->
            AppGridPage(
                modifier = Modifier.padding(horizontal = spacing.medium),
                pageIndex = pageIndex,
                page = displayGrid.grid[pageIndex],
                columns = displayGrid.spec.cols,
                cellHeight = cellHeight,
                iconSizeDp = ICON_SIZE,
                draggingAppId = state.draggingAppId,
                activeAppMenuIdentifier = state.activeAppMenuIdentifier,
                pagerContainerCoordinates = pagerContainerCoordinates.value,
                currentPagerPage = pagerState.currentPage,
                pagerPageCount = pagerState.pageCount,
                onDragStarted = { appId ->
                  gestureClaimedByTile.value = true
                  onIntent(GridIntent.DragStarted(appId))
                },
                onDragMovedToCell = { page, cell ->
                  onIntent(GridIntent.DragMovedToGridCell(page, cell))
                },
                onDragPointerMovedInPager = { pointerX, pagerWidth, currentPage, pageCount ->
                  onIntent(
                      GridIntent.DragPointerMovedInPager(
                          pointerXInPager = pointerX,
                          pagerWidth = pagerWidth,
                          currentPage = currentPage,
                          pageCount = pageCount,
                      )
                  )
                },
                onDragDropped = { onIntent(GridIntent.DragDropped) },
                onDragEnded = { accepted -> onIntent(GridIntent.DragEnded(accepted)) },
                onAppTapped = { app ->
                  gestureClaimedByTile.value = true
                  onIntent(GridIntent.AppTapped(app))
                },
                onAppMenuRequested = { appId ->
                  gestureClaimedByTile.value = true
                  onIntent(GridIntent.AppMenuRequested(appId))
                },
                onAppMenuDismissed = { onIntent(GridIntent.AppMenuDismissed) },
                onAppDetails = { app -> onIntent(GridIntent.AppDetailsRequested(app)) },
                onAppRemoval = { app -> onIntent(GridIntent.AppRemovalRequested(app)) },
                onShortcutLaunch = { shortcut ->
                  onIntent(GridIntent.ShortcutLaunchRequested(shortcut))
                },
            )
          }
        }

        if (pagerState.pageCount > 0) {
          PagerPageIndicator(
              pagerState = pagerState,
              modifier = Modifier.fillMaxWidth().height(spacing.medium),
          )
        }

        if (state.isBarVisible) {
          AppGridQuickBar(
              apps = displayGrid.bar,
              columns = displayGrid.spec.cols,
              cellHeight = cellHeight,
              iconSizeDp = ICON_SIZE,
              draggingAppId = state.draggingAppId,
              activeAppMenuIdentifier = state.activeAppMenuIdentifier,
              onDragStarted = { appId ->
                gestureClaimedByTile.value = true
                onIntent(GridIntent.DragStarted(appId))
              },
              onDragMovedToSlot = { index -> onIntent(GridIntent.DragMovedToQuickBarSlot(index)) },
              onDragDropped = { onIntent(GridIntent.DragDropped) },
              onDragEnded = { accepted -> onIntent(GridIntent.DragEnded(accepted)) },
              onAppTapped = { app ->
                gestureClaimedByTile.value = true
                onIntent(GridIntent.AppTapped(app))
              },
              onAppMenuRequested = { appId ->
                gestureClaimedByTile.value = true
                onIntent(GridIntent.AppMenuRequested(appId))
              },
              onAppMenuDismissed = { onIntent(GridIntent.AppMenuDismissed) },
              onAppDetails = { app -> onIntent(GridIntent.AppDetailsRequested(app)) },
              onAppRemoval = { app -> onIntent(GridIntent.AppRemovalRequested(app)) },
              onShortcutLaunch = { shortcut ->
                onIntent(GridIntent.ShortcutLaunchRequested(shortcut))
              },
          )
        }
      }
    }
    AppGridMenu(
        isVisible = state.isGridMenuVisible,
        offset = state.gridMenuOffset,
        isDefaultLauncher = isDefaultLauncher,
        currentGridSpec = displayGrid.spec,
        isBarVisible = state.isBarVisible,
        onDismissRequest = { onIntent(GridIntent.GridMenuDismissed) },
        onSetDefaultLauncherRequested = {
          onIntent(GridIntent.DefaultLauncherSettingsOpenRequested)
        },
        onWallpaperRequested = { onIntent(GridIntent.WallpaperSettingsOpenRequested) },
        onResetDefaultsRequested = { onIntent(GridIntent.ResetDefaultsRequested) },
        onGridSpecSelected = { spec -> onIntent(GridIntent.GridSpecChangeRequested(spec)) },
        onBarVisibilityChanged = { visible -> onIntent(GridIntent.BarVisibilityChanged(visible)) },
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640, backgroundColor = 0xFF333333)
@Composable
private fun AppGridContentPreview() {
  val cellsPerPage = previewSpec.cols * previewSpec.rows
  val pages = previewApps.chunked(cellsPerPage).map { appsOnPage -> previewPage(apps = appsOnPage) }
  val grid = Grid(spec = previewSpec, grid = pages, bar = previewApps.take(previewSpec.cols))
  val state = GridScreenState(persistedGrid = grid)
  AppTheme { AppGridContent(state = state, isDefaultLauncher = false, onIntent = {}) }
}
