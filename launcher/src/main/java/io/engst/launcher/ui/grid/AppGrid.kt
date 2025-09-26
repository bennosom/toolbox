@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.ClipData
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.engst.core.apps.launchIntent
import io.engst.core.wallpaper.setColorWallpaper
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.shared.AppIcon
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.rememberScreenInfo
import kotlinx.coroutines.launch

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
   onGridUpdate: (Grid) -> Unit = {},
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
   val state by rememberUpdatedState(workingState)
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
      val pagerState = rememberPagerState { uiState.grid.size }
      val scope = rememberCoroutineScope()
      Column(Modifier.fillMaxSize()) {
         // app grid
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
               .fillMaxWidth(),
         ) { pageIndex ->
            val page = uiState.grid[pageIndex]
            val cellOrder = remember(page) { page.keys.toList() }
            val lazyGridState = remember(pageIndex) { LazyGridState() }
            val gridCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

            LazyVerticalGrid(
               state = lazyGridState,
               columns = GridCells.Fixed(uiState.spec.cols),
               userScrollEnabled = false,
               verticalArrangement = Arrangement.spacedBy(spacing),
               horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
               modifier =
                  Modifier
                     .fillMaxSize()
                     .padding(horizontal = spacing)
                     .onPlaced { coordinates -> gridCoordinates.value = coordinates }
                     .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                           event.mimeTypes().contains("text/vnd.android.intent")
                        },
                        target =
                           object : DragAndDropTarget {
                              override fun onStarted(event: DragAndDropEvent) {
                                 dragMode = true
                              }

                              override fun onMoved(event: DragAndDropEvent) {
                                 val appId = draggingId ?: return
                                 val coordinates = gridCoordinates.value ?: return
                                 val localOffset = coordinates.localOffsetOf(event) ?: return
                                 val targetCell =
                                    lazyGridState.findCellAt(localOffset, cellOrder) ?: return
                                 val updated =
                                    workingState.applyDrag(
                                       appId,
                                       DragDestination.Grid(pageIndex, targetCell),
                                    )
                                 if (updated != workingState) {
                                    workingState = updated
                                 }
                              }

                              override fun onDrop(event: DragAndDropEvent): Boolean {
                                 dragMode = false
                                 draggingId = null
                                 onGridUpdate(workingState)
                                 return true
                              }

                              override fun onEnded(event: DragAndDropEvent) {
                                 dragMode = false
                                 draggingId = null
                              }
                           },
                     ),
            ) {
               page.forEach { (cell, app) ->
                  item("page$pageIndex-col${cell.col}-row${cell.row}") {
                     val itemModifier = Modifier.fillMaxSize()
                     if (app == null) {
                        Box(
                           modifier =
                              itemModifier
                                 .clip(MaterialTheme.shapes.small)
                                 .animateItem()
                        ) {}
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
                                             scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage)
                                             }
                                          }
                                       }
                                    },
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

         val quickBarCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
         val quickBarItemBounds = remember { mutableStateMapOf<String, Rect>() }
         SideEffect {
            val ids = uiState.bar.map { it.id }.toSet()
            quickBarItemBounds.keys.retainAll(ids)
         }

         // quick access bar
         Row(
            modifier =
               Modifier
                  .fillMaxWidth()
                  .height(96.dp)
                  .onPlaced { coordinates -> quickBarCoordinates.value = coordinates }
                  .dragAndDropTarget(
                     shouldStartDragAndDrop = { event ->
                        event.mimeTypes().contains("text/vnd.android.intent")
                     },
                     target =
                        object : DragAndDropTarget {
                           override fun onStarted(event: DragAndDropEvent) {
                              dragMode = true
                           }

                           override fun onMoved(event: DragAndDropEvent) {
                              val appId = draggingId ?: return
                              val rowCoordinates = quickBarCoordinates.value ?: return
                              val localOffset = rowCoordinates.localOffsetOf(event) ?: return
                              val rowHeight = rowCoordinates.size.height.toFloat()
                              if (localOffset.y !in 0f..rowHeight) return
                              val targetIndex =
                                 determineQuickBarTargetIndex(
                                    uiState.bar,
                                    quickBarItemBounds,
                                    localOffset,
                                 )
                                    ?: return
                              val updated =
                                 workingState.applyDrag(
                                    appId,
                                    DragDestination.QuickBar(targetIndex),
                                 )
                              if (updated != workingState) {
                                 workingState = updated
                              }
                           }

                           override fun onDrop(event: DragAndDropEvent): Boolean {
                              dragMode = false
                              draggingId = null
                              onGridUpdate(workingState)
                              return true
                           }

                           override fun onEnded(event: DragAndDropEvent) {
                              dragMode = false
                              draggingId = null
                           }
                        },
                  ),
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
                        .onPlaced { coordinates ->
                           quickBarItemBounds[app.id] = coordinates.boundsInParent()
                        }
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

private fun LayoutCoordinates.localOffsetOf(event: DragAndDropEvent): Offset? {
   if (!isAttached) return null
   val rootCoordinates = findRootCoordinates()
   if (!rootCoordinates.isAttached) return null
   val dragEvent = event.toAndroidDragEvent()
   val rootOffset = Offset(dragEvent.x, dragEvent.y)
   return localPositionOf(rootCoordinates, rootOffset)
}

private fun LazyGridState.findCellAt(localOffset: Offset, cells: List<Cell>): Cell? {
   val x = localOffset.x.toInt()
   val y = localOffset.y.toInt()
   if (x < 0 || y < 0) return null
   val itemInfo =
      layoutInfo.visibleItemsInfo.firstOrNull { info ->
         val withinX = x in info.offset.x until (info.offset.x + info.size.width)
         val withinY = y in info.offset.y until (info.offset.y + info.size.height)
         withinX && withinY
      }
         ?: return null
   return cells.getOrNull(itemInfo.index)
}

private fun determineQuickBarTargetIndex(
   bar: List<App>,
   bounds: Map<String, Rect>,
   localOffset: Offset,
): Int? {
   if (bar.isEmpty()) return 0
   var lastRect: Rect? = null
   bar.forEachIndexed { index, app ->
      val rect = bounds[app.id] ?: return null
      if (localOffset.x < rect.centerX()) {
         return index
      }
      lastRect = rect
   }
   return if (lastRect == null) null else bar.size
}

private fun Rect.centerX(): Float = (left + right) / 2f

private sealed interface DragDestination {
   data class Grid(val pageIndex: Int, val cell: Cell) : DragDestination

   data class QuickBar(val index: Int) : DragDestination
}

private sealed interface DragLocation {
   data class Grid(val pageIndex: Int, val cell: Cell) : DragLocation

   data class QuickBar(val index: Int) : DragLocation
}

private fun Grid.applyDrag(appId: String, destination: DragDestination): Grid {
   val origin = findLocation(appId) ?: return this
   return when (destination) {
      is DragDestination.Grid ->
         when (origin) {
            is DragLocation.Grid -> moveGridToGrid(origin, destination)
            is DragLocation.QuickBar -> moveBarToGrid(origin, destination)
         }

      is DragDestination.QuickBar ->
         when (origin) {
            is DragLocation.Grid -> moveGridToBar(origin, destination)
            is DragLocation.QuickBar -> moveBarToBar(origin, destination)
         }
   }
}

private fun Grid.findLocation(appId: String): DragLocation? {
   grid.forEachIndexed { pageIndex, page ->
      page.forEach { (cell, app) ->
         if (app?.id == appId) {
            return DragLocation.Grid(pageIndex, cell)
         }
      }
   }
   bar.forEachIndexed { index, app ->
      if (app.id == appId) {
         return DragLocation.QuickBar(index)
      }
   }
   return null
}

private fun Grid.moveGridToGrid(
   origin: DragLocation.Grid,
   destination: DragDestination.Grid
): Grid {
   if (origin.pageIndex == destination.pageIndex && origin.cell == destination.cell) return this
   val pages = grid.map { LinkedHashMap(it) }.toMutableList()
   val sourcePage = pages.getOrNull(origin.pageIndex) ?: return this
   val destinationPage = pages.getOrNull(destination.pageIndex) ?: return this
   val sourceApp = sourcePage[origin.cell] ?: return this
   val destinationApp = destinationPage[destination.cell]
   if (destinationApp === sourceApp) return this
   sourcePage[origin.cell] = destinationApp
   destinationPage[destination.cell] = sourceApp
   return copy(grid = pages.map { it.toMap() })
}

private fun Grid.moveBarToGrid(
   origin: DragLocation.QuickBar,
   destination: DragDestination.Grid
): Grid {
   if (origin.index !in bar.indices) return this
   val pages = grid.map { LinkedHashMap(it) }.toMutableList()
   val destinationPage = pages.getOrNull(destination.pageIndex) ?: return this
   val barList = bar.toMutableList()
   val sourceApp = barList.removeAt(origin.index)
   val destinationApp = destinationPage[destination.cell]
   destinationPage[destination.cell] = sourceApp
   if (destinationApp != null) {
      barList.add(origin.index.coerceIn(0, barList.size), destinationApp)
   }
   return copy(grid = pages.map { it.toMap() }, bar = barList)
}

private fun Grid.moveGridToBar(
   origin: DragLocation.Grid,
   destination: DragDestination.QuickBar
): Grid {
   val pages = grid.map { LinkedHashMap(it) }.toMutableList()
   val sourcePage = pages.getOrNull(origin.pageIndex) ?: return this
   val sourceApp = sourcePage[origin.cell] ?: return this
   val barList = bar.toMutableList()
   val insertIndex = destination.index.coerceIn(0, barList.size)
   val displacedApp = if (insertIndex < barList.size) barList[insertIndex] else null
   if (insertIndex < barList.size) {
      barList[insertIndex] = sourceApp
   } else {
      barList.add(sourceApp)
   }
   sourcePage[origin.cell] = displacedApp
   return copy(grid = pages.map { it.toMap() }, bar = barList)
}

private fun Grid.moveBarToBar(
   origin: DragLocation.QuickBar,
   destination: DragDestination.QuickBar
): Grid {
   if (origin.index == destination.index) return this
   if (origin.index !in bar.indices) return this
   val barList = bar.toMutableList()
   val item = barList.removeAt(origin.index)
   val targetIndex = destination.index.coerceIn(0, barList.size)
   barList.add(targetIndex, item)
   return copy(bar = barList)
}
