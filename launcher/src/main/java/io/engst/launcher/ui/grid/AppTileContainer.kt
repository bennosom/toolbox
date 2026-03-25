@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.grid.drag.draggableAppSource
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.previewApp
import io.engst.launcher.ui.shared.previewApps
import io.engst.launcher.ui.shared.previewPage
import io.engst.launcher.ui.shared.previewSpec
import io.engst.launcher.ui.shared.spacing

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
        modifier = Modifier
            .fillMaxSize()
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
            )
            .padding(MaterialTheme.spacing.medium),
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

@Preview(showBackground = true, widthDp = 360, heightDp = 480, backgroundColor = 0xFF333333)
@Composable
private fun AppTileContainerPopulatedPreview() {
    val page = previewPage(apps = previewApps)
    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            page.entries.chunked(previewSpec.cols).forEach { rowEntries ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowEntries.forEach { (_, app) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                        ) {
                            if (app != null) {
                                AppTileContainer(
                                    app = app,
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
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480, backgroundColor = 0xFF333333)
@Composable
private fun AppTileContainerDraggingPreview() {
    val app = previewApp("chrome", "Chrome")
    val page = previewPage(apps = listOf(app))
    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            page.entries.chunked(previewSpec.cols).forEach { rowEntries ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowEntries.forEach { (_, cellApp) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                        ) {
                            if (cellApp != null) {
                                AppTileContainer(
                                    app = cellApp,
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
                    }
                }
            }
        }
    }
}