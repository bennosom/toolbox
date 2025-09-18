package io.engst.launcher.ui.shared

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? =
   when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
   }

@Composable
fun SyncWallpaperToSystemBars(wallpaperState: WallpaperState?) {
   val activity = LocalContext.current.findActivity()

   fun applyTheme() {
      val window = activity?.window ?: return
      val isLight = wallpaperState?.isLight ?: false
      val controller = WindowCompat.getInsetsController(window, window.decorView)
      controller.isAppearanceLightStatusBars = isLight
      controller.isAppearanceLightNavigationBars = isLight
   }

   SideEffect { applyTheme() }

   val view = LocalView.current
   DisposableEffect(view) {
      val attachListener =
         object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) = applyTheme()

            override fun onViewDetachedFromWindow(v: android.view.View) = Unit
         }
      view.addOnAttachStateChangeListener(attachListener)

      val focusListener =
         ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (hasFocus) applyTheme() }
      view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

      onDispose {
         view.removeOnAttachStateChangeListener(attachListener)
         view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
      }
   }
}
