package io.engst.launcher.data

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import io.engst.core.Logging
import io.engst.core.scopedLogger
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val defaultGridSpec = GridSpec(4, 4)
val defaultGridData =
   GridData(cols = defaultGridSpec.cols, rows = defaultGridSpec.rows, bar = null, grid = null)

interface AppsRepository {
   val installedApps: Flow<List<App>>
   val grid: Flow<Grid>

   fun setGridSpec(spec: GridSpec)

   fun update(grid: Grid)

   fun resetDefaults(spec: GridSpec = defaultGridSpec)
}

class AppsRepositoryImpl(private val context: Context) :
   AppsRepository, Logging by scopedLogger("AppsRepositoryImpl") {

   private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
   private val callbackThread =
      HandlerThread("LauncherCallback", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
   private val callbackHandler = Handler(callbackThread.looper)
   private val launcherApps = context.getSystemService(LauncherApps::class.java)

   override val installedApps =
      callbackFlow {
         logInfo { "setup ${Thread.currentThread().name}" }

         fun update() {
            trySend(launcherApps.getInstalledApps(context.packageManager)).onFailure {
               logError(it) { "update: something went wrong" }
            }
         }

         val appsCallback =
            object : LauncherApps.Callback() {
               override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                  logDebug("onPackageAdded") { "packageName=$packageName" }
                  update()
               }

               override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                  logDebug("onPackageRemoved") { "packageName=$packageName" }
                  update()
               }

               override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                  logDebug("onPackageChanged") { "packageName=$packageName" }
                  update()
               }

               override fun onPackagesAvailable(
                  packageNames: Array<out String>?,
                  user: UserHandle?,
                  replacing: Boolean,
               ) {
                  logDebug("onPackagesAvailable") { "packageNames=$packageNames" }
                  update()
               }

               override fun onPackagesUnavailable(
                  packageNames: Array<out String>?,
                  user: UserHandle?,
                  replacing: Boolean,
               ) {
                  logDebug("LauncherApps.onPackagesUnavailable") { "packageNames=$packageNames" }
                  update()
               }
            }
         launcherApps.registerCallback(appsCallback, callbackHandler)

         val componentCallbacks =
            object : ComponentCallbacks2 {
               override fun onConfigurationChanged(newConfig: Configuration) {
                  logDebug { "onConfigurationChanged" }
                  update()
               }

               override fun onLowMemory() {}

               override fun onTrimMemory(level: Int) {}
            }
         context.registerComponentCallbacks(componentCallbacks)

         update()

         awaitClose {
            callbackHandler.post {
               logInfo { "teardown ${Thread.currentThread().name}" }

               // Unregister configuration callbacks
               runCatching { context.unregisterComponentCallbacks(componentCallbacks) }

               // Unregister package callbacks on the handler thread
               runCatching { launcherApps.unregisterCallback(appsCallback) }

               callbackThread.quitSafely()
            }
         }
      }
         .map { it.sortedBy { it.componentName } }
         .shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)

   private val dataStore = MutableStateFlow<GridData>(defaultGridData)

   override val grid: Flow<Grid> =
      installedApps
         .combine(dataStore) { apps, store ->
            val spec = GridSpec(store.cols, store.rows)

            val barApps =
               if (store.bar == null) {
                  buildBar(apps).take(spec.rows)
               } else {
                  store.bar.mapNotNull { appId ->
                     apps.find { it.componentName.flattenToString() == appId }
                  }
               }
            val barAppIds = barApps.map { it.id }

            val gridApps =
               if (store.grid == null) {
                  buildGrid(apps.filterNot { barAppIds.contains(it.id) }, spec)
               } else {
                  // Build pages from store, then append any newly installed apps not in the store
                  val orderedCells =
                     (0 until spec.cols).flatMap { col ->
                        (0 until spec.rows).map { row -> Cell(col, row) }
                     }

                  // Start with pages as LinkedHashMaps to preserve insertion order
                  val pages =
                     store.grid
                        .map { page ->
                           val linked = LinkedHashMap<Cell, App?>()
                           page.forEach { (cell, appId) ->
                              val app = apps.find { it.componentName.flattenToString() == appId }
                              linked[cell] = app
                           }
                           linked
                        }
                        .toMutableList()

                  val pageCapacity = orderedCells.size

                  // Normalize existing pages to ensure they contain all cells in the expected order
                  fun normalizePage(index: Int) {
                     val existing = pages[index]
                     if (
                        existing.size == pageCapacity &&
                        orderedCells.all { existing.containsKey(it) }
                     )
                        return
                     val normalized = LinkedHashMap<Cell, App?>(pageCapacity)
                     orderedCells.forEach { c -> normalized[c] = existing[c] }
                     pages[index] = normalized
                  }
                  for (i in pages.indices) normalizePage(i)

                  // Collect app IDs already placed on the grid (exclude nulls)
                  val placedIds = buildSet {
                     pages.forEach { page -> page.values.forEach { app -> app?.let { add(it.id) } } }
                  }

                  // Determine apps to append: installed apps not in bar and not already placed
                  val toAppend =
                     apps.filterNot { barAppIds.contains(it.id) || placedIds.contains(it.id) }

                  // Find the last occupied global cell index across all pages
                  var lastOccupied = -1
                  pages.forEachIndexed { pageIndex, page ->
                     orderedCells.forEachIndexed { cellIndex, cell ->
                        if (page[cell] != null) {
                           lastOccupied = pageIndex * pageCapacity + cellIndex
                        }
                     }
                  }

                  // Append missing apps after the last occupied cell, creating new pages as needed
                  var nextIndex = lastOccupied + 1
                  toAppend.forEach { app ->
                     val pageIndex = nextIndex / pageCapacity
                     val cellIndex = nextIndex % pageCapacity
                     while (pageIndex >= pages.size) {
                        // Create a new empty page with all cells preset to null
                        val newPage = LinkedHashMap<Cell, App?>(pageCapacity)
                        orderedCells.forEach { c -> newPage[c] = null }
                        pages.add(newPage)
                     }
                     normalizePage(pageIndex)
                     val targetCell = orderedCells[cellIndex]
                     pages[pageIndex][targetCell] = app
                     nextIndex++
                  }

                  pages.map { it.toMap() }
               }

            Grid(spec = spec, bar = barApps, grid = gridApps)
         }
         .asSharedFlow()

   override fun update(grid: Grid) {
      logDebug { "updateGrid: $grid" }
      scope.launch {
         dataStore.update {
            GridData(
               cols = grid.spec.cols,
               rows = grid.spec.rows,
               bar = grid.bar.map { it.componentName.flattenToString() },
               grid =
                  grid.grid.map { page ->
                     page.map { (cell, app) -> cell to app?.componentName?.flattenToString() }
                        .toMap()
                  },
            )
         }
      }
   }

   override fun setGridSpec(spec: GridSpec) {
      logDebug { "updateSpec: $spec" }
      // TODO: add merge strategy to keep order as much as possible
      resetDefaults(spec)
   }

   override fun resetDefaults(spec: GridSpec) {
      logDebug { "resetDefaults: $spec" }
      scope.launch { dataStore.update { defaultGridData.copy(cols = spec.cols, rows = spec.rows) } }
   }

   private fun buildBar(apps: List<App>): List<App> {
      val defaultAppIds = resolveDefaultApps()
      return defaultAppIds.mapNotNull { appId ->
         apps.find { it.componentName.flattenToString().contains(appId) }
      }
   }

   private fun buildGrid(apps: List<App>, spec: GridSpec): List<Map<Cell, App?>> = buildList {
      val pageCapacity = spec.cols * spec.rows
      if (pageCapacity <= 0) return@buildList
      val totalPages = if (apps.isEmpty()) 0 else (apps.size + pageCapacity - 1) / pageCapacity
      var index = 0
      (0 until totalPages).forEach { _ ->
         val page = buildMap {
            (0 until spec.cols).forEach { col ->
               (0 until spec.rows).forEach { row ->
                  val app = if (index < apps.size) apps[index++] else null
                  put(Cell(col, row), app)
               }
            }
         }
         add(page)
      }
   }

   private fun resolveDefaultApps(): List<String> =
      listOfNotNull(
         context.resolveDefaultPhoneApp()?.flattenToString(),
         context.resolveDefaultMessengerApp()?.flattenToString(),
         context.resolveDefaultBrowserApp()?.flattenToString(),
      )

   private fun <T : Any> Flow<T>.asStateFlow(initial: T) =
      stateIn(scope, SharingStarted.WhileSubscribed(5000L), initial)

   private fun <T : Any> Flow<T>.asSharedFlow(replay: Int = 1) =
      shareIn(scope, SharingStarted.WhileSubscribed(5000L), replay)
}
