package io.engst.launcher.data

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import androidx.datastore.core.DataStore
import io.engst.core.Logging
import io.engst.core.scopedLogger
import io.engst.launcher.data.proto.GridDataProto
import io.engst.launcher.data.store.toGridData
import io.engst.launcher.data.store.toProto
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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

class AppsRepositoryImpl(
   private val context: Context,
   private val dataStore: DataStore<GridDataProto>,
) : AppsRepository, Logging by scopedLogger("AppsRepositoryImpl") {

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
                  logDebug("onPackagesUnavailable") { "packageNames=$packageNames" }
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
               runCatching { context.unregisterComponentCallbacks(componentCallbacks) }
               runCatching { launcherApps.unregisterCallback(appsCallback) }
               callbackThread.quitSafely()
            }
         }
      }
         .map { it.sortedBy { app -> app.componentName } }
         .shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)

   private val storedGridData: Flow<GridData> =
      dataStore.data.map { proto ->
         logDebug { "dataStore read: cols=${proto.cols} rows=${proto.rows}" }
         proto.toGridData()
      }

   override val grid: Flow<Grid> =
      installedApps
         .combine(storedGridData) { apps, store ->
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
                  buildGridFromStore(store.grid, apps, barAppIds, spec)
               }

            Grid(spec = spec, bar = barApps, grid = gridApps)
         }
         .asSharedFlow()

   override fun update(grid: Grid) {
      logDebug { "updateGrid: $grid" }
      scope.launch {
         val data = GridData(
            cols = grid.spec.cols,
            rows = grid.spec.rows,
            bar = grid.bar.map { it.componentName.flattenToString() },
            grid = grid.grid.map { page ->
               page.map { (cell, app) -> cell to app?.componentName?.flattenToString() }.toMap()
            },
            hasUserGrid = true,
            hasUserBar = true,
            barSlots = 0,
         )
         logDebug { "persisting grid update to DataStore" }
         dataStore.updateData { data.toProto() }
      }
   }

   override fun setGridSpec(spec: GridSpec) {
      logDebug { "updateSpec: $spec" }
      resetDefaults(spec)
   }

   override fun resetDefaults(spec: GridSpec) {
      logDebug { "resetDefaults: $spec" }
      scope.launch {
         val data = defaultGridData.copy(cols = spec.cols, rows = spec.rows)
         logDebug { "persisting resetDefaults to DataStore" }
         dataStore.updateData { data.toProto() }
      }
   }

   private fun buildGridFromStore(
      storeGrid: List<Map<Cell, AppId?>>,
      apps: List<App>,
      barAppIds: List<String>,
      spec: GridSpec,
   ): List<Map<Cell, App?>> {
      val orderedCells =
         (0 until spec.cols).flatMap { col ->
            (0 until spec.rows).map { row -> Cell(col, row) }
         }

      val pages =
         storeGrid
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

      fun normalizePage(index: Int) {
         val existing = pages[index]
         if (
            existing.size == pageCapacity &&
            orderedCells.all { existing.containsKey(it) }
         ) return
         val normalized = LinkedHashMap<Cell, App?>(pageCapacity)
         orderedCells.forEach { c -> normalized[c] = existing[c] }
         pages[index] = normalized
      }
      for (i in pages.indices) normalizePage(i)

      val placedIds = buildSet {
         pages.forEach { page -> page.values.forEach { app -> app?.let { add(it.id) } } }
      }

      val toAppend =
         apps.filterNot { barAppIds.contains(it.id) || placedIds.contains(it.id) }

      var lastOccupied = -1
      pages.forEachIndexed { pageIndex, page ->
         orderedCells.forEachIndexed { cellIndex, cell ->
            if (page[cell] != null) {
               lastOccupied = pageIndex * pageCapacity + cellIndex
            }
         }
      }

      var nextIndex = lastOccupied + 1
      toAppend.forEach { app ->
         val pageIndex = nextIndex / pageCapacity
         val cellIndex = nextIndex % pageCapacity
         while (pageIndex >= pages.size) {
            val newPage = LinkedHashMap<Cell, App?>(pageCapacity)
            orderedCells.forEach { c -> newPage[c] = null }
            pages.add(newPage)
         }
         normalizePage(pageIndex)
         val targetCell = orderedCells[cellIndex]
         pages[pageIndex][targetCell] = app
         nextIndex++
      }

      return pages.map { it.toMap() }
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

   private fun <T : Any> Flow<T>.asSharedFlow(replay: Int = 1) =
      shareIn(scope, SharingStarted.WhileSubscribed(5000L), replay)
}