@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.ClipData
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.engst.core.apps.launchIntent
import io.engst.core.logDebug
import io.engst.core.wallpaper.setColorWallpaper
import io.engst.launcher.model.App
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.shared.AppIcon
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.rememberScreenInfo

@OptIn(
   ExperimentalFoundationApi::class,
   ExperimentalMaterial3Api::class,
   ExperimentalComposeUiApi::class,
)
@Composable
fun AppGrid(
   savedState: Grid?,
   modifier: Modifier = Modifier,
   onAppLaunch: (App) -> Unit = {},
   onAppDetails: (App) -> Unit = {},
   onAppRemove: (App) -> Unit = {},
   onShortcutLaunch: (ShortcutInfo) -> Unit = {},
   onAppManager: () -> Unit = {},
   onGridChanged: (GridSpec) -> Unit = {},
   onResetDefaults: () -> Unit = {},
   onSetDefaultLauncher: () -> Unit = {},
) {
   if (savedState == null || savedState.grid.isEmpty()) {
      Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
         CircularProgressIndicator(
            Modifier.size(108.dp),
            color = LocalWallpaperState.current.suggestedForegroundColor,
         )
      }
      return
   }

   val density = LocalDensity.current
   val viewConfiguration = LocalViewConfiguration.current
   val screen = rememberScreenInfo()

   var dragMode by remember { mutableStateOf(false) }
   var workingState by remember(savedState) { mutableStateOf(savedState) }
   val state = if (dragMode) workingState else savedState
   val uiState =
      if (dragMode) state.copy(grid = state.grid + state.grid.last().mapValues { null }) else state

   var draggingId by remember { mutableStateOf<String?>(null) }
   var showGridMenu by remember { mutableStateOf(false) }
   var gridMenuOffset by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
   var appMenuForId by remember { mutableStateOf<String?>(null) }
   var suppressGridMenu by remember { mutableStateOf(false) }

   Box(
      modifier =
         modifier
            .fillMaxSize()
            .pointerInput(Unit) {
               detectTapGestures(
                  onLongPress = { pos ->
                     if (!suppressGridMenu) {
                        gridMenuOffset = with(density) { DpOffset(pos.x.toDp(), pos.y.toDp()) }
                        showGridMenu = true
                     }
                  }
               )
            }
   ) {
      val dragAndDropTarget = remember {
         object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
               logDebug { "DragAndDropTarget.onStarted: $event" }
               dragMode = true
            }

            override fun onChanged(event: DragAndDropEvent) {
               logDebug { "DragAndDropTarget.onChanged: $event" }
               super.onChanged(event)
            }

            override fun onEntered(event: DragAndDropEvent) {
               logDebug { "DragAndDropTarget.onEntered: $event" }
               super.onEntered(event)
            }

            override fun onExited(event: DragAndDropEvent) {
               logDebug { "DragAndDropTarget.onExited: $event" }
               super.onExited(event)
            }

            override fun onMoved(event: DragAndDropEvent) {
               logDebug { "DragAndDropTarget.onMoved: $event" }
               super.onMoved(event)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
               logDebug { "DragAndDropTarget.onDrop: $event" }
               dragMode = false
               draggingId = null
               return true
            }

            override fun onEnded(event: DragAndDropEvent) {
               logDebug { "DragAndDropTarget.onEnded: $event" }
               dragMode = false
               draggingId = null
            }
         }
      }

      Column(Modifier.fillMaxSize()) {
         // app grid
         val pagerState = rememberPagerState { uiState.grid.size }
         val iconSize = 60.dp
         val spacing = 12.dp
         val spacingPx = with(density) { spacing.roundToPx() }
         val pageSize = remember {
            object : PageSize {
               override fun Density.calculateMainAxisPageSize(
                  availableSpace: Int,
                  pageSpacing: Int,
               ): Int = if (dragMode) (availableSpace - spacingPx * 2) else availableSpace
            }
         }

         HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = spacing, vertical = spacing),
            pageSize = pageSize,
            snapPosition = SnapPosition.Center,
            modifier = Modifier
               .weight(1f)
               .fillMaxWidth()
               .border(1.dp, Color.Magenta),
         ) { pageIndex ->
            val page = uiState.grid[pageIndex]

            LazyVerticalGrid(
               columns = GridCells.Fixed(uiState.spec.cols),
               userScrollEnabled = false,
               verticalArrangement = Arrangement.spacedBy(spacing),
               horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
               modifier =
                  Modifier
                     .fillMaxSize()
                     .border(1.dp, Color.Yellow)
                     .padding(horizontal = spacing),
            ) {
               page.forEach { (cell, app) ->
                  item("page$pageIndex-col${cell.col}-row${cell.row}") {
                     val itemModifier = Modifier.fillMaxSize()
                     if (app == null) {
                        Spacer(itemModifier)
                     } else {
                        val dragTransferData =
                           remember(app) {
                              DragAndDropTransferData(
                                 clipData =
                                    ClipData.newIntent(
                                       app.label,
                                       Intent(Intent.ACTION_MAIN)
                                          .addCategory(Intent.CATEGORY_LAUNCHER)
                                          .setComponent(app.componentName),
                                    ),
                                 flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE,
                              )
                           }

                        Box(
                           modifier =
                              itemModifier
                                 .clip(MaterialTheme.shapes.small)
                                 .animateItem()
                                 .dragAndDropSource(
                                    drawDragDecoration = {
                                       val shadowSize = with(density) { iconSize.roundToPx() }
                                       val imageBitmap =
                                          app.icon.toBitmap(shadowSize, shadowSize).asImageBitmap()
                                       drawImage(imageBitmap)
                                    },
                                    block = {
                                       awaitEachGesture {
                                          val down = awaitFirstDown(requireUnconsumed = false)
                                          showGridMenu = false
                                          suppressGridMenu = true

                                          try {
                                             val longPress = awaitLongPressOrCancellation(down.id)
                                             if (longPress == null) {
                                                val up = waitForUpOrCancellation()
                                                if (up != null) {
                                                   val duration =
                                                      up.uptimeMillis - down.uptimeMillis
                                                   val movement =
                                                      (up.position - down.position).getDistance()
                                                   if (
                                                      duration < viewConfiguration.longPressTimeoutMillis &&
                                                      movement < viewConfiguration.touchSlop
                                                   ) {
                                                      up.consumeDownChange()
                                                      appMenuForId = null
                                                      draggingId = null
                                                      onAppLaunch(app)
                                                   }
                                                }
                                                return@awaitEachGesture
                                             }

                                             appMenuForId = app.id

                                             val pointerId = longPress.id
                                             val dragChange = awaitDragOrCancellation(pointerId)
                                             if (dragChange == null) {
                                                return@awaitEachGesture
                                             }

                                             if (dragChange.changedToUp()) {
                                                dragChange.consumeDownChange()
                                                return@awaitEachGesture
                                             }

                                             appMenuForId = null
                                             showGridMenu = false
                                             draggingId = app.id
                                             dragChange.consumeDownChange()
                                             dragChange.consumePositionChange()
                                             startTransfer(dragTransferData)
                                             return@awaitEachGesture
                                          } finally {
                                             suppressGridMenu = false
                                             showGridMenu = false
                                          }
                                       }
                                    },
                                 )
                                 .dragAndDropTarget(
                                    shouldStartDragAndDrop = { event ->
                                       event.mimeTypes().contains("text/vnd.android.intent")
                                    },
                                    target = dragAndDropTarget,
                                 )
                                 .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                       // Prevent click action if a long-press initiated a menu or drag
                                       if (appMenuForId == null && draggingId == null) {
                                          appMenuForId = null
                                          showGridMenu = false
                                          onAppLaunch(app)
                                       }
                                    },
                                    onLongClick = null,
                                 ),
                           contentAlignment = Alignment.Center,
                        ) {
                           if (draggingId != app.id) {
                              AppTile(app = app, iconSize = iconSize, screenInfo = screen)

                              // app menu
                              AppMenu(
                                 app = app,
                                 visible = appMenuForId == app.id,
                                 onDismissRequest = { appMenuForId = null },
                                 onAppDetails = onAppDetails,
                                 onAppRemove = onAppRemove,
                                 onShortcutLaunch = onShortcutLaunch,
                              )
                           }
                        }
                     }
                  }
               }
            }
         }

         // page indicator
         if (pagerState.pageCount > 0) {
            PageIndicator(
               count = pagerState.pageCount,
               currentIndex = pagerState.currentPage,
               modifier = Modifier
                  .fillMaxWidth()
                  .height(spacing),
            )
         }

         Spacer(Modifier.size(spacing))

         // quick access bar
         Row(
            modifier = Modifier
               .fillMaxWidth()
               .height(96.dp)
               .border(1.dp, Color.Cyan),
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
         ) {
            uiState.bar.forEach { app ->
               val dragTransferData =
                  remember(app) {
                     DragAndDropTransferData(
                        clipData =
                           ClipData.newIntent(
                              app.label,
                              Intent(Intent.ACTION_MAIN)
                                 .addCategory(Intent.CATEGORY_LAUNCHER)
                                 .setComponent(app.componentName),
                           ),
                        flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE,
                     )
                  }

               Box(
                  modifier =
                     Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .dragAndDropSource(
                           drawDragDecoration = {
                              val shadowSize = with(density) { iconSize.roundToPx() }
                              val imageBitmap =
                                 app.icon.toBitmap(shadowSize, shadowSize).asImageBitmap()
                              drawImage(imageBitmap)
                           },
                           block = {
                              awaitEachGesture {
                                 val down = awaitFirstDown(requireUnconsumed = false)
                                 showGridMenu = false
                                 suppressGridMenu = true

                                 try {
                                    val longPress = awaitLongPressOrCancellation(down.id)
                                    if (longPress == null) {
                                       val up = waitForUpOrCancellation()
                                       if (up != null) {
                                          val duration = up.uptimeMillis - down.uptimeMillis
                                          val movement = (up.position - down.position).getDistance()
                                          if (
                                             duration < viewConfiguration.longPressTimeoutMillis &&
                                             movement < viewConfiguration.touchSlop
                                          ) {
                                             up.consumeDownChange()
                                             appMenuForId = null
                                             draggingId = null
                                             onAppLaunch(app)
                                          }
                                       }
                                       return@awaitEachGesture
                                    }

                                    appMenuForId = app.id

                                    val pointerId = longPress.id
                                    val dragChange = awaitDragOrCancellation(pointerId)
                                    if (dragChange == null) {
                                       return@awaitEachGesture
                                    }

                                    if (dragChange.changedToUp()) {
                                       dragChange.consumeDownChange()
                                       return@awaitEachGesture
                                    }

                                    appMenuForId = null
                                    showGridMenu = false
                                    draggingId = app.id
                                    dragChange.consumeDownChange()
                                    dragChange.consumePositionChange()
                                    startTransfer(dragTransferData)
                                    return@awaitEachGesture
                                 } finally {
                                    suppressGridMenu = false
                                    showGridMenu = false
                                 }
                              }
                           },
                        )
                        .dragAndDropTarget(
                           shouldStartDragAndDrop = { event ->
                              event.mimeTypes().contains("text/vnd.android.intent")
                           },
                           target = dragAndDropTarget,
                        )
                        .combinedClickable(
                           interactionSource = remember { MutableInteractionSource() },
                           indication = null,
                           onClick = {
                              // Prevent click action if a long-press initiated a menu or drag
                              if (appMenuForId == null && draggingId == null) {
                                 appMenuForId = null
                                 showGridMenu = false
                                 onAppLaunch(app)
                              }
                           },
                           onLongClick = null,
                        )
               ) {
                  if (draggingId != app.id) {
                     AppIcon(app, size = iconSize)

                     // app menu
                     AppMenu(
                        app = app,
                        visible = appMenuForId == app.id,
                        onDismissRequest = { appMenuForId = null },
                        onShortcutLaunch = onShortcutLaunch,
                        onAppDetails = onAppDetails,
                        onAppRemove = onAppRemove,
                     )
                  }
               }
            }
      }
      }

      // grid menu
      val context = LocalContext.current
      DropdownMenu(
         expanded = showGridMenu,
         onDismissRequest = { showGridMenu = false },
         offset = gridMenuOffset,
         shape = MaterialTheme.shapes.large,
         modifier = Modifier.requiredSizeIn(minWidth = 180.dp),
      ) {
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Apps, null) },
            text = { Text("Apps list") },
            onClick = {
               showGridMenu = false
               onAppManager()
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Rocket, null) },
            text = { Text("Set as default launcher") },
            onClick = {
               showGridMenu = false
               onSetDefaultLauncher()
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.ClearAll, null) },
            text = { Text("Reset defaults") },
            onClick = {
               showGridMenu = false
               onResetDefaults()
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.GridView, null) },
            text = { Text("4x3") },
            onClick = {
               showGridMenu = false
               onGridChanged(GridSpec(4, 3))
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.GridView, null) },
            text = { Text("5x3") },
            onClick = {
               showGridMenu = false
               onGridChanged(GridSpec(5, 3))
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.GridView, null) },
            text = { Text("3x4") },
            onClick = {
               showGridMenu = false
               onGridChanged(GridSpec(3, 4))
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.GridView, null) },
            text = { Text("4x5") },
            onClick = {
               showGridMenu = false
               onGridChanged(GridSpec(4, 5))
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Nightlight, null) },
            text = { Text("Dark mode") },
            onClick = {
               showGridMenu = false
               context.launchIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = Color.Cyan) },
            text = { Text("Color") },
            onClick = { context.setColorWallpaper(Color.Cyan.toArgb()) },
         )
         DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = Color.DarkGray) },
            text = { Text("Color") },
            onClick = { context.setColorWallpaper(Color.DarkGray.toArgb()) },
         )
      }
   }
}
