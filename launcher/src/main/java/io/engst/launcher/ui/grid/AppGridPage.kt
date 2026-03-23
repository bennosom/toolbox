@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.ViewConfiguration
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.ui.grid.drag.draggableAppSource
import io.engst.launcher.ui.shared.ScreenInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridPage(
    pageIndex: Int,
    page: Map<Cell, App?>,
    columns: Int,
    iconSizeDp: Dp,
    spacing: Dp,
    draggingAppId: String?,
    activeAppMenuIdentifier: String?,
    density: Density,
    viewConfiguration: ViewConfiguration,
    screenInfo: ScreenInfo,
    pagerContainerCoordinates: LayoutCoordinates?,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    onDragStarted: (appId: String) -> Unit,
    onDragMovedToCell: (pageIndex: Int, cell: Cell) -> Unit,
    onDragDropped: () -> Unit,
    onDragCancelled: () -> Unit,
    onAppTapped: (App) -> Unit,
    onAppMenuRequested: (appId: String) -> Unit,
    onAppMenuDismissed: () -> Unit,
    onAppDetails: (App) -> Unit,
    onAppRemoval: (App) -> Unit,
    onShortcutLaunch: (ShortcutInfo) -> Unit,
) {
    val cellOrder = remember(page) { page.keys.toList() }
    val lazyGridState = remember(pageIndex) { LazyGridState() }
    val gridCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    val dragTarget = remember(pageIndex) {
        object : DragAndDropTarget {
            override fun onMoved(event: DragAndDropEvent) {
                val coordinates = gridCoordinates.value ?: return
                val localOffset = coordinates.localOffsetOf(event) ?: return
                val targetCell = lazyGridState.findCellAt(localOffset, cellOrder) ?: return
                onDragMovedToCell(pageIndex, targetCell)

                val pagerCoords = pagerContainerCoordinates ?: return
                val pagerLocalOffset = pagerCoords.localOffsetOf(event) ?: return
                val pagerWidth = pagerCoords.size.width.toFloat()
                val targetPage = detectEdgeScrollTarget(
                    pointerXInPager = pagerLocalOffset.x,
                    pagerWidth = pagerWidth,
                    currentPage = pagerState.currentPage,
                    pageCount = pagerState.pageCount,
                ) ?: return
                if (!pagerState.isScrollInProgress) {
                    coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                }
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDragDropped()
                return true
            }

            override fun onEnded(event: DragAndDropEvent) {
                onDragCancelled()
            }
        }
    }

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(columns),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing)
            .onPlaced { coordinates -> gridCoordinates.value = coordinates }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { it.mimeTypes().contains("text/vnd.android.intent") },
                target = dragTarget,
            ),
    ) {
        page.forEach { (cell, app) ->
            item("page$pageIndex-col${cell.col}-row${cell.row}") {
                if (app == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                            .animateItem(),
                    )
                } else {
                    AppTileContainer(
                        app = app,
                        iconSizeDp = iconSizeDp,
                        draggingAppId = draggingAppId,
                        activeAppMenuIdentifier = activeAppMenuIdentifier,
                        density = density,
                        viewConfiguration = viewConfiguration,
                        screenInfo = screenInfo,
                        onDragStarted = onDragStarted,
                        onAppTapped = onAppTapped,
                        onAppMenuRequested = onAppMenuRequested,
                        onAppMenuDismissed = onAppMenuDismissed,
                        onAppDetails = onAppDetails,
                        onAppRemoval = onAppRemoval,
                        onShortcutLaunch = onShortcutLaunch,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTileContainer(
    app: App,
    iconSizeDp: Dp,
    draggingAppId: String?,
    activeAppMenuIdentifier: String?,
    density: Density,
    viewConfiguration: ViewConfiguration,
    screenInfo: ScreenInfo,
    onDragStarted: (appId: String) -> Unit,
    onAppTapped: (App) -> Unit,
    onAppMenuRequested: (appId: String) -> Unit,
    onAppMenuDismissed: () -> Unit,
    onAppDetails: (App) -> Unit,
    onAppRemoval: (App) -> Unit,
    onShortcutLaunch: (ShortcutInfo) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .animateItem()
            .draggableAppSource(
                app = app,
                iconSizeDp = iconSizeDp,
                density = density,
                viewConfiguration = viewConfiguration,
                onTap = { onAppTapped(app) },
                onLongPress = { onAppMenuRequested(app.id) },
                onDragStarted = { onDragStarted(app.id) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (draggingAppId != app.id) {
            AppTile(app = app, iconSize = iconSizeDp, screenInfo = screenInfo)
            AppMenu(
                app = app,
                visible = activeAppMenuIdentifier == app.id,
                onDismissRequest = onAppMenuDismissed,
                onAppDetails = onAppDetails,
                onAppRemove = onAppRemoval,
                onShortcutLaunch = onShortcutLaunch,
            )
        }
    }
}

private fun LayoutCoordinates.localOffsetOf(event: DragAndDropEvent): Offset? {
    if (!isAttached) return null
    val root = findRootCoordinates()
    if (!root.isAttached) return null
    val drag = event.toAndroidDragEvent()
    return localPositionOf(root, Offset(drag.x, drag.y))
}

private fun LazyGridState.findCellAt(localOffset: Offset, cells: List<Cell>): Cell? {
    val x = localOffset.x.toInt()
    val y = localOffset.y.toInt()
    if (x < 0 || y < 0) return null
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { info ->
        x in info.offset.x until (info.offset.x + info.size.width) &&
            y in info.offset.y until (info.offset.y + info.size.height)
    } ?: return null
    return cells.getOrNull(itemInfo.index)
}
