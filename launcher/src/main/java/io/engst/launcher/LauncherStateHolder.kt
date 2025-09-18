package io.engst.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import io.engst.core.launchOnDisplay
import io.engst.core.logDebug
import io.engst.core.logError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class App(
    val id: String,
    val name: String,
    val icon: Drawable,
    val componentName: ComponentName,
    val launchIntent: Intent
)

interface LauncherStateHolder {
  val apps: StateFlow<List<App>>
  val isLoading: StateFlow<Boolean>

  fun launch(app: App, displayId: Int)
}

class LauncherStateHolderNoop() : LauncherStateHolder {
  override val apps: StateFlow<List<App>> =
      MutableStateFlow(
          List(20) {
            App(
                id = it.toString(),
                name = it.toString(),
                icon = Color.Magenta.toArgb().toDrawable(),
                componentName = ComponentName("a", "b"),
                launchIntent = Intent())
          })
  override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)

  override fun launch(app: App, displayId: Int) {}
}

class LauncherStateHolderImpl(private val context: Context) : LauncherStateHolder {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val launcherService = context.getSystemService(LauncherApps::class.java)
  private val callbackThread =
      HandlerThread("LauncherAppsCallbacks", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
  private val callbackHandler = Handler(callbackThread.looper)

  override val apps: StateFlow<List<App>> =
      callbackFlow {
            fun update() {
              trySend(getLauncherApps()).onFailure { logError { "something went wrong" } }
            }

            val callback =
                object : LauncherApps.Callback() {
                  override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                    logDebug { "onPackageAdded: $packageName" }
                    update()
                  }

                  override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                    logDebug { "onPackageRemoved: $packageName" }
                    update()
                  }

                  override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                    logDebug { "onPackageChanged: $packageName" }
                    update()
                  }

                  override fun onPackagesAvailable(
                      packageNames: Array<out String>?,
                      user: UserHandle?,
                      replacing: Boolean
                  ) {
                    logDebug { "onPackagesAvailable: $packageNames" }
                    update()
                  }

                  override fun onPackagesUnavailable(
                      packageNames: Array<out String>?,
                      user: UserHandle?,
                      replacing: Boolean
                  ) {
                    logDebug { "onPackagesUnavailable: $packageNames" }
                    update()
                  }
                }

            update()
            launcherService.registerCallback(callback, callbackHandler)
            awaitClose {
              callbackHandler.post {
                runCatching { launcherService.unregisterCallback(callback) }
                callbackThread.quitSafely()
              }
            }
          }
          .asStateFlow(emptyList())

  override val isLoading = apps.map { it.isEmpty() }.asStateFlow(true)

  override fun launch(app: App, displayId: Int) {
    context.launchOnDisplay(app.componentName, displayId)
  }

  private fun <T : Any> Flow<T>.asStateFlow(initial: T) =
      stateIn(scope, SharingStarted.WhileSubscribed(5000L), initial)

  private fun getLauncherApps(): List<App> {
    return launcherService
        .getActivityList(null, Process.myUserHandle())
        .sortedBy { it.componentName }
        .filter { it.componentName.packageName != context.packageName }
        .map { info ->
          App(
              id = info.componentName.flattenToString(),
              name = info.label.toString(),
              icon = info.getBadgedIcon(0),
              componentName = info.componentName,
              launchIntent =
                  Intent.makeMainActivity(info.componentName).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                  })
        }
  }
}
