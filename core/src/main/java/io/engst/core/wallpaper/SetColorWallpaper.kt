package io.engst.core.wallpaper

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.ColorInt
import androidx.annotation.RequiresPermission
import androidx.core.graphics.createBitmap
import io.engst.core.logError

@RequiresPermission(Manifest.permission.SET_WALLPAPER)
fun Context.setColorWallpaper(@ColorInt color: Int) {
   val bitmap = createBitmap(display.width, display.height, Bitmap.Config.ARGB_8888)
   Canvas(bitmap).drawColor(color)
   try {
      val wallpaperManager = WallpaperManager.getInstance(this)
      wallpaperManager.setBitmap(bitmap)
   } catch (ex: Exception) {
      logError(ex) { "Failed to set wallpaper" }
   }
}
