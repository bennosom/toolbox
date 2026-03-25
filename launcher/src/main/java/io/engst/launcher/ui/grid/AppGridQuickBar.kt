@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import io.engst.launcher.ui.shared.localOffsetOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.grid.drag.draggableAppSource
import io.engst.launcher.ui.shared.AppIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridQuickBar(
    apps: List<App>,
    cellHeight: Dp,
    iconSizeDp: Dp,
    spacing: Dp,
    draggingAppId: String?,
    activeAppMenuIdentifier: String?,
    density: Density,
    viewConfiguration: ViewConfiguration,
    onDragStarted: (appId: String) -> Unit,
    onDragMovedToSlot: (targetIndex: Int) -> Unit,
    onDragDropped: () -> Unit,
    onDragCancelled: () -> Unit,
    onAppTapped: (App) -> Unit,
    onAppMenuRequested: (appId: String) -> Unit,
    onAppMenuDismissed: () -> Unit,
    onAppDetails: (App) -> Unit,
    onAppRemoval: (App) -> Unit,
    onShortcutLaunch: (ShortcutInfo) -> Unit,
) {
    val barCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }

    SideEffect {
        val currentIds = apps.map { it.id }.toSet()
        itemBounds.keys.retainAll(currentIds)
    }

    val dragTarget = remember {
        object : DragAndDropTarget {
            override fun onMoved(event: DragAndDropEvent) {
                val rowCoords = barCoordinates.value ?: return
                val localOffset = rowCoords.localOffsetOf(event) ?: return
                val rowHeight = rowCoords.size.height.toFloat()
                if (localOffset.y !in 0f..rowHeight) return
                val targetIndex = determineQuickBarTargetIndex(apps, itemBounds, localOffset) ?: return
                onDragMovedToSlot(targetIndex)
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

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .onPlaced { coordinates -> barCoordinates.value = coordinates }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { it.mimeTypes().contains("text/vnd.android.intent") },
                target = dragTarget,
            ),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(apps, key = { _, app -> app.id }) { _, app ->
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .onPlaced { coordinates -> itemBounds[app.id] = coordinates.boundsInParent() }
                    .draggableAppSource(
                        app = app,
                        iconSizeDp = iconSizeDp,
                        density = density,
                        viewConfiguration = viewConfiguration,
                        interactionSource = interactionSource,
                        onTap = { onAppTapped(app) },
                        onLongPress = { onAppMenuRequested(app.id) },
                        onDragStarted = { onDragStarted(app.id) },
                    ),
            ) {
                if (draggingAppId != app.id) {
                    AppIcon(app, size = iconSizeDp)
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
    }
}

private fun determineQuickBarTargetIndex(
    bar: List<App>,
    bounds: Map<String, Rect>,
    localOffset: Offset,
): Int? {
    if (bar.isEmpty()) return 0
    bar.forEachIndexed { index, app ->
        val rect = bounds[app.id] ?: return null
        if (localOffset.x < rect.centerX) return index
    }
    return bar.size
}

private val Rect.centerX: Float get() = (left + right) / 2f

@Preview(showBackground = true, widthDp = 360, heightDp = 96)
@Composable
private fun AppGridQuickBarPreview() {
    // Preview requires real App instances with Drawable icons.
    // Shown here for structural verification — replace with stub Apps in IDE.
}
