package io.engst.launcher.ui

import android.content.ClipData
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import io.engst.core.logDebug
import io.engst.launcher.LauncherStateHolder
import io.engst.launcher.LauncherStateHolderImpl

class LauncherActivity : ComponentActivity() {

  private val stateHolder: LauncherStateHolder by lazy { LauncherStateHolderImpl(this) }

  @OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    logDebug {
      "onCreate: $this rootTask=$isTaskRoot task=$taskId intent=$intent display=${display.displayId}"
    }
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    window.setBackgroundDrawable(TRANSPARENT.toDrawable())

    setContent {
      val view = LocalView.current
      val density = LocalDensity.current
      MaterialTheme {
        LauncherApp(
            stateHolder = stateHolder,
            displayId = display.displayId,
            onDragStart = { app ->
              val size = with(density) { 80.dp.roundToPx() }
              view.startDragAndDrop(
                  ClipData.newIntent(app.name, app.launchIntent),
                  IconDragShadowBuilder(app.icon.toBitmap(size, size)),
                  null,
                  View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE)
            })
      }
    }
  }
}
