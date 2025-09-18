package io.engst.launcher.ui.shared

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class WallpaperState(val isLight: Boolean, val suggestedForegroundColor: Color) {
   companion object {
      fun fromColors(colors: WallpaperColors?): WallpaperState? {
         return colors?.let {
            val hints = colors.colorHints
            val isLight = (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
            WallpaperState(
               isLight = isLight,
               suggestedForegroundColor = if (isLight) Color.Black else Color.White,
            )
         }
      }
   }
}

val LocalWallpaperState =
   staticCompositionLocalOf<WallpaperState> { throw Exception("No value provided") }

@Composable
fun rememberWallpaperState(
   isDarkMode: Boolean,
   fallback: WallpaperState =
      WallpaperState(isLight = false, suggestedForegroundColor = Color.Black),
   type: Int = WallpaperManager.FLAG_SYSTEM,
): WallpaperState {
   val context = LocalContext.current
   val handler = Handler(Looper.getMainLooper())
   val wallpaperManager = context.getSystemService(WallpaperManager::class.java)
   var value by remember {
      mutableStateOf(
         WallpaperState.fromColors(wallpaperManager.getWallpaperColors(type)) ?: fallback
      )
   }

   LaunchedEffect(isDarkMode) {
      val colors = wallpaperManager.getWallpaperColors(type)
      value = WallpaperState.fromColors(colors) ?: fallback
   }

   DisposableEffect(context) {
      val listener =
         WallpaperManager.OnColorsChangedListener { colors, which ->
            if ((which and type) != 0) {
               value = WallpaperState.fromColors(colors) ?: fallback
            }
         }
      wallpaperManager.addOnColorsChangedListener(listener, handler)
      onDispose { wallpaperManager.removeOnColorsChangedListener(listener) }
   }

   return value
}
