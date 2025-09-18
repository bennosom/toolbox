package io.engst.launcher.ui

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.engst.core.Logging
import io.engst.core.apps.launchActivity
import io.engst.core.scopedLogger
import io.engst.launcher.core.isDefaultLauncher
import io.engst.launcher.core.launchAppDetails
import io.engst.launcher.core.launchAppRemovalRequest
import io.engst.launcher.core.launchDefaultAppSettings
import io.engst.launcher.core.launchShortcut
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.ui.grid.AppGrid
import io.engst.launcher.ui.manager.AppManager
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.SyncWallpaperToSystemBars
import io.engst.launcher.ui.shared.rememberWallpaperState
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LauncherActivity : ComponentActivity(), Logging by scopedLogger("LauncherActivity") {

  private val isDefaultHomeState = mutableStateOf(false)

  @OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    logDebug {
      "onCreate: $this rootTask=$isTaskRoot task=$taskId intent=$intent display=${display.displayId}"
    }
    super.onCreate(savedInstanceState)

     // Show the system wallpaper behind our window
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    window.setBackgroundDrawable(TRANSPARENT.toDrawable())

     enableEdgeToEdge()

    setContent {
       val context = LocalContext.current
       val darkMode = isSystemInDarkTheme()
       val wallpaperState = rememberWallpaperState(darkMode)
       SyncWallpaperToSystemBars(wallpaperState)
       CompositionLocalProvider(LocalWallpaperState provides wallpaperState) {
          MaterialTheme(
             colorScheme =
                if (darkMode) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
          ) {
          Box(modifier = Modifier.fillMaxSize()) {
             val repository: AppsRepository by inject()

             var showAppManager by remember { mutableStateOf(false) }
             if (showAppManager) {
                AppManager(
                   modifier = Modifier
                      .fillMaxSize()
                      .padding(horizontal = 6.dp),
                   apps = repository.installedApps.collectAsStateWithLifecycle(emptyList()).value,
                   onAppLaunch = { app -> context.launchActivity(app.componentName) },
                   onAppDetails = { app -> context.launchAppDetails(app.componentName.packageName) },
                   onAppRemove = { app ->
                      context.launchAppRemovalRequest(app.componentName.packageName)
                   },
                   onClose = { showAppManager = false },
                )
             } else {
                AppGrid(
                   modifier = Modifier
                      .fillMaxSize()
                      .safeDrawingPadding(),
                   savedState = repository.grid.collectAsStateWithLifecycle(null).value,
                   onAppLaunch = { app -> context.launchActivity(app.componentName) },
                   onAppDetails = { app -> context.launchAppDetails(app.componentName.packageName) },
                   onAppRemove = { app ->
                      context.launchAppRemovalRequest(app.componentName.packageName)
                   },
                   onShortcutLaunch = { shortcut -> context.launchShortcut(shortcut) },
                   onAppManager = { showAppManager = true },
                   onGridChanged = { repository.setGridSpec(it) },
                   onResetDefaults = { repository.resetDefaults() },
                   onSetDefaultLauncher = { context.launchDefaultAppSettings() },
                   onGridUpdate = { repository.update(it) },
                )
             }

             val scope = rememberCoroutineScope()
             val snackbarHostState = remember { SnackbarHostState() }
             LaunchedEffect(isDefaultHomeState.value) {
                if (!isDefaultHomeState.value) {
                   scope.launch {
                      val result =
                         snackbarHostState.showSnackbar(
                            message = "Set as default Launcher app",
                            actionLabel = "Show Settings",
                            duration = SnackbarDuration.Short,
                         )
                      if (result == SnackbarResult.ActionPerformed) {
                         context.launchDefaultAppSettings()
                      }
                   }
                }
             }

             Box(
                modifier = Modifier
                   .fillMaxSize()
                   .safeContentPadding(),
                contentAlignment = Alignment.TopCenter,
             ) {
                SnackbarHost(hostState = snackbarHostState)
             }
          }
          }
       }
    }
  }

   override fun onResume() {
      super.onResume()
      isDefaultHomeState.value = isDefaultLauncher()
   }
}
