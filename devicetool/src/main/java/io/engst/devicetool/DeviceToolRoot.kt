package io.engst.devicetool

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DeviceToolRoot(
    onSetWallpaperColor: (Color) -> Unit,
    onClearWallpaper: () -> Unit,
    onLaunchOnAllDisplays: () -> Unit,
    modifier: Modifier = Modifier,
) {
  var showInstalledApps by remember { mutableStateOf(false) }

  if (showInstalledApps) {
    AppsScreen(
        modifier = modifier.fillMaxSize(),
        onNavigateBack = { showInstalledApps = false },
    )
  } else {
    InsetsAndColorsScreen(
        modifier = modifier.fillMaxSize(),
        onSetWallpaperColor = onSetWallpaperColor,
        onClearWallpaper = onClearWallpaper,
        onLaunchOnAllDisplays = onLaunchOnAllDisplays,
        onOpenInstalledApps = { showInstalledApps = true },
    )
  }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 820)
@Composable
private fun DeviceToolRootPreview() {
  MaterialTheme {
    DeviceToolRoot(
        onSetWallpaperColor = {},
        onClearWallpaper = {},
        onLaunchOnAllDisplays = {},
    )
  }
}
