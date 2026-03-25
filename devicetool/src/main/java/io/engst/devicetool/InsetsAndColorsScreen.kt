package io.engst.devicetool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val wallpaperColors =
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

@Composable
fun InsetsAndColorsScreen(
    onSetWallpaperColor: (Color) -> Unit,
    onClearWallpaper: () -> Unit,
    onLaunchOnAllDisplays: () -> Unit,
    onOpenInstalledApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    SafeInsetOverlay()
    Surface(
        modifier =
            Modifier
                .fillMaxSize(0.92f)
                .safeContentPadding()
                .align(Alignment.Center),
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray,
        tonalElevation = 2.dp,
    ) {
      Column(
          modifier =
              Modifier
                  .fillMaxSize()
                  .padding(10.dp)
                  .verticalScroll(rememberScrollState())
                  .horizontalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        WallpaperColorPalette(
            onSetWallpaperColor = onSetWallpaperColor,
            onClearWallpaper = onClearWallpaper,
        )
        ImeAndDisplayTools(
            onLaunchOnAllDisplays = onLaunchOnAllDisplays,
            onOpenInstalledApps = onOpenInstalledApps,
        )
        DeviceInfo()
      }
    }
  }
}

@Composable
private fun SafeInsetOverlay() {
  Box(
      Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .border(1.dp, Color.Magenta),
  ) {
    Text("safeDrawing", color = Color.Magenta, modifier = Modifier.align(Alignment.BottomStart))
  }
  Box(
      Modifier
          .fillMaxSize()
          .safeContentPadding()
          .border(1.dp, Color.Cyan),
  ) {
    Text("safeContent", color = Color.Cyan, modifier = Modifier.align(Alignment.BottomCenter))
  }
  Box(
      Modifier
          .fillMaxSize()
          .safeGesturesPadding()
          .border(1.dp, Color.Yellow),
  ) {
    Text("safeGestures", color = Color.Yellow, modifier = Modifier.align(Alignment.BottomEnd))
  }
}

@Composable
private fun WallpaperColorPalette(
    onSetWallpaperColor: (Color) -> Unit,
    onClearWallpaper: () -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    wallpaperColors.forEach { color ->
      IconButton(onClick = { onSetWallpaperColor(color) }) {
        Spacer(
            Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(color),
        )
      }
    }
    Button(onClick = onClearWallpaper) { Text("Clear") }
  }
}

@Composable
private fun ImeAndDisplayTools(
    onLaunchOnAllDisplays: () -> Unit,
    onOpenInstalledApps: () -> Unit,
) {
  var input by remember { mutableStateOf("Click to show IME") }
  val keyboardController = LocalSoftwareKeyboardController.current

  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = Modifier.weight(1f),
    )
    Button(onClick = { keyboardController?.hide() }) { Text("Hide IME") }
    Button(onClick = onLaunchOnAllDisplays) { Text("All Displays") }
    Button(onClick = onOpenInstalledApps) { Text("Installed apps") }
  }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 820)
@Composable
private fun InsetsAndColorsScreenPreview() {
  MaterialTheme {
    InsetsAndColorsScreen(
        onSetWallpaperColor = {},
        onClearWallpaper = {},
        onLaunchOnAllDisplays = {},
        onOpenInstalledApps = {},
    )
  }
}
