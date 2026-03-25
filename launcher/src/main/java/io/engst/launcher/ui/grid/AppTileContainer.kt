@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.grid.drag.draggableAppSource
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.previewApp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppTileContainer(
    app: App,
    iconSizeDp: Dp,
    draggingAppId: String?,
    activeAppMenuIdentifier: String?,
    onDragStarted: (appId: String) -> Unit,
    onAppTapped: (App) -> Unit,
    onAppMenuRequested: (appId: String) -> Unit,
    onAppMenuDismissed: () -> Unit,
    onAppDetails: (App) -> Unit,
    onAppRemoval: (App) -> Unit,
    onShortcutLaunch: (ShortcutInfo) -> Unit,
) {
  val density = LocalDensity.current
  val viewConfiguration = LocalViewConfiguration.current
  val interactionSource = remember { MutableInteractionSource() }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .testTag("app_tile_${app.id}")
              .clip(MaterialTheme.shapes.medium)
              .indication(interactionSource, ripple())
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
      contentAlignment = Alignment.Center,
  ) {
    if (draggingAppId == app.id) {
      DropIndicator(modifier = Modifier.fillMaxSize())
    } else {
      AppTile(app = app, iconSize = iconSizeDp)
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

@Preview(showBackground = true, widthDp = 120, heightDp = 120, backgroundColor = 0xFF333333)
@Composable
private fun AppTileContainerPopulatedPreview() {
  AppTheme {
    AppTileContainer(
        app = previewApp("a", "foo"),
        iconSizeDp = ICON_SIZE,
        draggingAppId = null,
        activeAppMenuIdentifier = null,
        onDragStarted = {},
        onAppTapped = {},
        onAppMenuRequested = {},
        onAppMenuDismissed = {},
        onAppDetails = {},
        onAppRemoval = {},
        onShortcutLaunch = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 120, heightDp = 120, backgroundColor = 0xFF333333)
@Composable
private fun AppTileContainerDraggingPreview() {
  val app = previewApp("chrome", "Chrome")
  AppTheme {
    AppTileContainer(
        app = app,
        iconSizeDp = ICON_SIZE,
        draggingAppId = app.id,
        activeAppMenuIdentifier = null,
        onDragStarted = {},
        onAppTapped = {},
        onAppMenuRequested = {},
        onAppMenuDismissed = {},
        onAppDetails = {},
        onAppRemoval = {},
        onShortcutLaunch = {},
    )
  }
}
