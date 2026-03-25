@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.grid.drag.draggableAppSource
import io.engst.launcher.ui.shared.AppIcon
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.localOffsetOf
import io.engst.launcher.ui.shared.previewApps
import io.engst.launcher.ui.shared.spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridQuickBar(
    apps: List<App>,
    columns: Int,
    cellHeight: Dp,
    iconSizeDp: Dp,
    draggingAppId: String?,
    activeAppMenuIdentifier: String?,
    onDragStarted: (appId: String) -> Unit,
    onDragMovedToSlot: (targetIndex: Int) -> Unit,
    onDragDropped: () -> Unit,
    onDragEnded: (accepted: Boolean) -> Unit,
    onAppTapped: (App) -> Unit,
    onAppMenuRequested: (appId: String) -> Unit,
    onAppMenuDismissed: () -> Unit,
    onAppDetails: (App) -> Unit,
    onAppRemoval: (App) -> Unit,
    onShortcutLaunch: (ShortcutInfo) -> Unit,
) {
  val density = LocalDensity.current
  val viewConfiguration = LocalViewConfiguration.current
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
        onDragEnded(event.toAndroidDragEvent().result)
      }
    }
  }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(cellHeight)
              .padding(horizontal = MaterialTheme.spacing.medium)
              .onPlaced { coordinates -> barCoordinates.value = coordinates }
              .dragAndDropTarget(
                  shouldStartDragAndDrop = { it.mimeTypes().contains("text/vnd.android.intent") },
                  target = dragTarget,
              ),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    for (index in 0 until columns) {
      val app = apps.getOrNull(index)
      if (app != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.weight(1f)
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .indication(interactionSource, ripple())
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
      } else {
        Box(modifier = Modifier.weight(1f).fillMaxSize())
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

private val Rect.centerX: Float
  get() = (left + right) / 2f

@Preview(showBackground = true, widthDp = 360, heightDp = 120, backgroundColor = 0xFF333333)
@Composable
private fun AppGridQuickBarPopulatedPreview() {
  AppTheme {
    AppGridQuickBar(
        apps = previewApps.take(3),
        columns = 4,
        cellHeight = 120.dp,
        iconSizeDp = 60.dp,
        draggingAppId = null,
        activeAppMenuIdentifier = null,
        onDragStarted = {},
        onDragMovedToSlot = {},
        onDragDropped = {},
        onDragEnded = {},
        onAppTapped = {},
        onAppMenuRequested = {},
        onAppMenuDismissed = {},
        onAppDetails = {},
        onAppRemoval = {},
        onShortcutLaunch = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 120, backgroundColor = 0xFF333333)
@Composable
private fun AppGridQuickBarEmptyPreview() {
  AppTheme {
    AppGridQuickBar(
        apps = emptyList(),
        columns = 4,
        cellHeight = 120.dp,
        iconSizeDp = 60.dp,
        draggingAppId = null,
        activeAppMenuIdentifier = null,
        onDragStarted = {},
        onDragMovedToSlot = {},
        onDragDropped = {},
        onDragEnded = {},
        onAppTapped = {},
        onAppMenuRequested = {},
        onAppMenuDismissed = {},
        onAppDetails = {},
        onAppRemoval = {},
        onShortcutLaunch = {},
    )
  }
}
