package io.engst.launcher.ui

import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import io.engst.core.Logging
import io.engst.core.scopedLogger
import io.engst.launcher.core.isDefaultLauncher
import io.engst.launcher.core.launchDefaultAppSettings
import io.engst.launcher.core.setImageWallpaper
import io.engst.launcher.core.setSolidColorWallpaper
import io.engst.launcher.ui.grid.AppGrid
import io.engst.launcher.ui.grid.GridEffectHandler
import io.engst.launcher.ui.grid.GridEffectHandlerImpl
import io.engst.launcher.ui.grid.GridIntent
import io.engst.launcher.ui.grid.GridViewModel
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.SyncWallpaperToSystemBars
import io.engst.launcher.ui.shared.rememberWallpaperState
import io.engst.launcher.ui.wallpaper.WallpaperSettingsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
       val systemDarkMode = isSystemInDarkTheme()
       val gridViewModel: GridViewModel = org.koin.androidx.compose.koinViewModel()

       val darkMode = systemDarkMode
       val wallpaperState = rememberWallpaperState(darkMode)
       SyncWallpaperToSystemBars(wallpaperState)
       AppTheme(
          colorScheme =
             if (darkMode) dynamicDarkColorScheme(context)
             else dynamicLightColorScheme(context),
          wallpaperState = wallpaperState,
       ) {
          Box(modifier = Modifier.fillMaxSize()) {
             val scope = rememberCoroutineScope()
             val snackbarHostState = remember { SnackbarHostState() }
             var isWallpaperDialogVisible by remember { mutableStateOf(false) }

             val wallpaperPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
             ) { }
             val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
             ) { uri ->
                if (uri == null) {
                   return@rememberLauncherForActivityResult
                }
                scope.launch {
                   logDebug { "setting wallpaper from SAF uri=$uri" }
                   val result = withContext(Dispatchers.IO) { context.setImageWallpaper(uri) }
                   val message =
                      if (result.isSuccess) "Wallpaper image applied" else "Failed to apply wallpaper image"
                   snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                }
             }

             val effectHandler: GridEffectHandler by inject()
             (effectHandler as GridEffectHandlerImpl).apply {
                onSetDefaultLauncher = { context.launchDefaultAppSettings() }
                onOpenWallpaperSettings = { isWallpaperDialogVisible = true }
                onShowResetDefaultsSnackbar = {
                   scope.launch {
                      val result = snackbarHostState.showSnackbar(
                         message = "Layout reset to defaults",
                         actionLabel = "Undo",
                         duration = SnackbarDuration.Short,
                      )
                      when (result) {
                         SnackbarResult.ActionPerformed ->
                            gridViewModel.onIntent(GridIntent.ResetDefaultsUndone)
                         SnackbarResult.Dismissed ->
                            gridViewModel.onIntent(GridIntent.ResetDefaultsConfirmed)
                      }
                   }
                 }
             }

             if (isWallpaperDialogVisible) {
                WallpaperSettingsDialog(
                   onDismissRequest = { isWallpaperDialogVisible = false },
                   onSetColorRequested = { color ->
                      scope.launch {
                         logDebug { "setting solid wallpaper color=$color" }
                         val result = withContext(Dispatchers.IO) { context.setSolidColorWallpaper(color) }
                         val message =
                            if (result.isSuccess) "Wallpaper color applied"
                            else "Failed to apply wallpaper color"
                         snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short,
                         )
                      }
                   },
                   onOpenWallpaperPickerRequested = {
                      runCatching {
                         wallpaperPickerLauncher.launch(Intent(Intent.ACTION_SET_WALLPAPER))
                      }.onFailure {
                         logWarn { "failed to launch wallpaper picker: ${it.message}" }
                         scope.launch {
                            snackbarHostState.showSnackbar(
                               message = "Failed to open wallpaper picker",
                               duration = SnackbarDuration.Short,
                            )
                         }
                      }
                   },
                   onOpenPhotoPickerRequested = {
                      photoPickerLauncher.launch(arrayOf("image/*"))
                   },
                )
             }

             AppGrid(
                modifier = Modifier
                   .fillMaxSize()
                   .safeDrawingPadding(),
                isDefaultLauncher = isDefaultHomeState.value,
                viewModel = gridViewModel,
             )

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

   override fun onResume() {
      super.onResume()
      isDefaultHomeState.value = isDefaultLauncher()
   }
}
