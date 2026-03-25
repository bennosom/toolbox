@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.localOffsetOf
import io.engst.launcher.ui.shared.spacing
import io.engst.launcher.ui.shared.previewApps
import io.engst.launcher.ui.shared.previewPage
import io.engst.launcher.ui.shared.previewSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridPage(
    pageIndex: Int,
    page: Map<Cell, App?>,
    columns: Int,
    cellHeight: Dp,
    iconSizeDp: Dp,
    draggingAppId: String?,
    activeAppMenuIdentifier: String?,
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
    modifier: Modifier = Modifier
) {
    val gridCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val cellBounds = remember { mutableStateMapOf<Cell, Rect>() }

    val rows = remember(page, columns) {
        page.entries.chunked(columns)
    }

    val dragTarget = remember(pageIndex) {
        object : DragAndDropTarget {
            override fun onMoved(event: DragAndDropEvent) {
                val coordinates = gridCoordinates.value ?: return
                val localOffset = coordinates.localOffsetOf(event) ?: return
                val targetCell = findCellAt(localOffset, cellBounds) ?: return
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { coordinates -> gridCoordinates.value = coordinates }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { it.mimeTypes().contains("text/vnd.android.intent") },
                target = dragTarget,
            ),
    ) {
        rows.forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowEntries.forEach { (cell, app) ->
                    if (app == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .onPlaced { coords ->
                                    val gridCoords = gridCoordinates.value ?: return@onPlaced
                                    val topLeft = gridCoords.localPositionOf(coords, Offset.Zero)
                                    cellBounds[cell] = Rect(topLeft, coords.size.toSize())
                                },
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .onPlaced { coords ->
                                    val gridCoords = gridCoordinates.value ?: return@onPlaced
                                    val topLeft = gridCoords.localPositionOf(coords, Offset.Zero)
                                    cellBounds[cell] = Rect(topLeft, coords.size.toSize())
                                },
                        ) {
                            AppTileContainer(
                                app = app,
                                iconSizeDp = iconSizeDp,
                                draggingAppId = draggingAppId,
                                activeAppMenuIdentifier = activeAppMenuIdentifier,
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, widthDp = 400, heightDp = 500, backgroundColor = 0xFF333333)
@Composable
private fun AppGridPagePreview() {
    val page = previewPage(apps = previewApps)
    val pagerState = rememberPagerState { 1 }
    val coroutineScope = rememberCoroutineScope()
    AppTheme {
        AppGridPage(
            pageIndex = 0,
            page = page,
            columns = previewSpec.cols,
            cellHeight = 120.dp,
            iconSizeDp = ICON_SIZE,
            draggingAppId = null,
            activeAppMenuIdentifier = null,
            pagerContainerCoordinates = null,
            pagerState = pagerState,
            coroutineScope = coroutineScope,
            onDragStarted = {},
            onDragMovedToCell = { _, _ -> },
            onDragDropped = {},
            onDragCancelled = {},
            onAppTapped = {},
            onAppMenuRequested = {},
            onAppMenuDismissed = {},
            onAppDetails = {},
            onAppRemoval = {},
            onShortcutLaunch = {},
        )
    }
}

private fun findCellAt(localOffset: Offset, cellBounds: Map<Cell, Rect>): Cell? {
    return cellBounds.entries.firstOrNull { (_, rect) -> rect.contains(localOffset) }?.key
}