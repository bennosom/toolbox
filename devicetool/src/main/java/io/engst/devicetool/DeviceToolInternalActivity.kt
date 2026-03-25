package io.engst.devicetool

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import io.engst.core.apps.launchActivity
import io.engst.core.wallpaper.setColorWallpaper

class DeviceToolActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

     launchActivity(
        component = ComponentName(this, DeviceToolInternalActivity::class.java),
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
    )
    finish()
  }
}

class DeviceToolInternalActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    window.setBackgroundDrawable(TRANSPARENT.toDrawable())

    setContent {
      DeviceToolRoot(
          onSetWallpaperColor = { color -> setColorWallpaper(color.toArgb()) },
          onClearWallpaper = { WallpaperManager.getInstance(this).clear() },
          onLaunchOnAllDisplays = { launchOnAllDisplays() },
      )
    }
  }

  private fun launchOnAllDisplays() {
    val launchComponent = intent.component ?: ComponentName(this, DeviceToolInternalActivity::class.java)
    val displayManager = getSystemService(DisplayManager::class.java)
    val currentDisplayId = display?.displayId

    displayManager.displays
        .asSequence()
        .filter { it.displayId != currentDisplayId }
        .forEach { targetDisplay ->
          launchActivity(
              component = launchComponent,
              displayId = targetDisplay.displayId,
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
          )
        }
  }
}
