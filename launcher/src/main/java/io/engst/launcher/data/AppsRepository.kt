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
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.model.collapseEmptyPages
import io.engst.launcher.ui.shared.DarkModePreference
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
   val darkModePreference: Flow<DarkModePreference>
   val isBarVisible: Flow<Boolean>

   fun setGridSpec(spec: GridSpec)

   fun update(grid: Grid)

   fun resetDefaults(spec: GridSpec = defaultGridSpec)

   fun setDarkModePreference(preference: DarkModePreference)

   fun setBarVisible(visible: Boolean)
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

   override val darkModePreference: Flow<DarkModePreference> =
      storedGridData.map { it.darkModePreference }
         .asSharedFlow()

   override val isBarVisible: Flow<Boolean> =
      storedGridData.map { it.isBarVisible }
         .asSharedFlow()

   override val grid: Flow<Grid> =
      installedApps
         .combine(storedGridData) { apps, store ->
            val spec = GridSpec(store.cols, store.rows)
            val barCapacity = if (store.barSlots > 0) store.barSlots else spec.cols

            val barApps =
               if (store.bar == null) {
                  buildBar(apps).take(barCapacity)
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

            Grid(spec = spec, bar = barApps, grid = gridApps).collapseEmptyPages()
         }
         .asSharedFlow()

   override fun update(grid: Grid) {
      logDebug { "updateGrid: $grid" }
      scope.launch {
         dataStore.updateData { current ->
            val existing = current.toGridData()
            val data = GridData(
               cols = grid.spec.cols,
               rows = grid.spec.rows,
               bar = grid.bar.map { it.componentName.flattenToString() },
               grid = grid.grid.map { page ->
                  page.map { (cell, app) -> cell to app?.componentName?.flattenToString() }.toMap()
               },
               hasUserGrid = true,
               hasUserBar = true,
               barSlots = existing.barSlots,
               darkModePreference = existing.darkModePreference,
               isBarVisible = existing.isBarVisible,
            )
            logDebug { "persisting grid update to DataStore" }
            data.toProto()
         }
      }
   }

   override fun setGridSpec(spec: GridSpec) {
      logDebug { "updateSpec: $spec" }
      resetDefaults(spec)
   }

   override fun resetDefaults(spec: GridSpec) {
      logDebug { "resetDefaults: $spec" }
      scope.launch {
         dataStore.updateData { current ->
            val existing = current.toGridData()
            val data = defaultGridData.copy(
               cols = spec.cols,
               rows = spec.rows,
               darkModePreference = existing.darkModePreference,
               isBarVisible = existing.isBarVisible,
            )
            logDebug { "persisting resetDefaults to DataStore" }
            data.toProto()
         }
      }
   }

   override fun setDarkModePreference(preference: DarkModePreference) {
      logDebug { "setDarkModePreference: $preference" }
      scope.launch {
         dataStore.updateData { current ->
            current.toGridData().copy(darkModePreference = preference).toProto()
         }
      }
   }

   override fun setBarVisible(visible: Boolean) {
      logDebug { "setBarVisible: $visible" }
      scope.launch {
         dataStore.updateData { current ->
            current.toGridData().copy(isBarVisible = visible).toProto()
         }
      }
   }

   private fun buildBar(apps: List<App>): List<App> {
      val defaultAppIds = resolveDefaultApps()
      return defaultAppIds.mapNotNull { appId ->
         apps.find { it.componentName.flattenToString().contains(appId) }
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