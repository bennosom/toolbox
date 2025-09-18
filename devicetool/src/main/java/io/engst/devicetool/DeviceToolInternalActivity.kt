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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
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
      val context = LocalContext.current
      Box(Modifier.fillMaxSize()) {
         Box(Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .border(1.dp, Color.Magenta)) {
          Text(
              "safeDrawing",
              color = Color.Magenta,
             modifier = Modifier.align(Alignment.BottomStart),
          )
        }
         Box(Modifier
            .fillMaxSize()
            .safeContentPadding()
            .border(1.dp, Color.Cyan)) {
          Text("safeContent", color = Color.Cyan, modifier = Modifier.align(Alignment.BottomCenter))
        }
         Box(Modifier
            .fillMaxSize()
            .safeGesturesPadding()
            .border(1.dp, Color.Yellow)) {
          Text("safeGestures", color = Color.Yellow, modifier = Modifier.align(Alignment.BottomEnd))
        }

        Box(
           Modifier
              .fillMaxSize(0.9f)
              .safeContentPadding()
              .align(Alignment.Center)
              .clip(MaterialTheme.shapes.medium)
              .background(Color.LightGray)
              .padding(6.dp)
        ) {
           Column(
              modifier =
                 Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(12.dp),
           ) {
              Row {
                 listOf(
                    Color(0xFF0F5790),
                    Color(0xFFC35214),
                    Color(0xFF575757),
                    Color.Cyan,
                    Color.Magenta,
                    Color.Yellow,
                    Color.Red,
                    Color.Green,
                    Color.Blue,
                    Color.Black,
                    Color.White,
                 )
                    .map { color ->
                       IconButton(onClick = { context.setColorWallpaper(color.toArgb()) }) {
                          Spacer(Modifier
                             .fillMaxSize()
                             .background(color))
                       }
                    }
                 IconButton(onClick = { WallpaperManager.getInstance(context).clear() }) {
                    Icon(Icons.Filled.Clear, null)
                 }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                 var input by remember { mutableStateOf("Click to show IME") }
                 OutlinedTextField(value = input, onValueChange = { input = it })
                 val keyboardController = LocalSoftwareKeyboardController.current
                 Button(onClick = { keyboardController?.hide() }) { Text("Hide IME") }
                 Button(
                    onClick = {
                       val displayManager = context.getSystemService(DisplayManager::class.java)
                       val current = context.display.displayId
                       displayManager.displays
                          .filter { it.displayId != current }
                          .forEach { display ->
                             context.launchActivity(
                                component = intent.component!!,
                                displayId = display.displayId,
                                flags =
                                   Intent.FLAG_ACTIVITY_NEW_TASK or
                                     Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                             )
                          }
                    }
                 ) {
                    Text("All Displays")
                 }
              }
              DeviceInfo()
           }
        }
      }
    }
  }
}
