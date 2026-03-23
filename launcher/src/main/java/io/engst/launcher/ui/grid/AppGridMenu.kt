package io.engst.launcher.ui.grid

import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.GridSpec

@Composable
fun AppGridMenu(
    isVisible: Boolean,
    offset: DpOffset,
    onDismissRequest: () -> Unit,
    onAppsListRequested: () -> Unit,
    onSetDefaultLauncherRequested: () -> Unit,
    onResetDefaultsRequested: () -> Unit,
    onGridSpecSelected: (GridSpec) -> Unit,
    onDarkModeRequested: () -> Unit,
    onColorWallpaperRequested: (Color) -> Unit,
) {
    DropdownMenu(
        expanded = isVisible,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.requiredSizeIn(minWidth = 180.dp),
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
            text = { Text("Apps list") },
            onClick = { onAppsListRequested() },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Rocket, contentDescription = null) },
            text = { Text("Set as default launcher") },
            onClick = { onSetDefaultLauncherRequested() },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) },
            text = { Text("Reset defaults") },
            onClick = { onResetDefaultsRequested() },
        )
        GridSpecMenuItems(onGridSpecSelected = onGridSpecSelected)
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Nightlight, contentDescription = null) },
            text = { Text("Dark mode") },
            onClick = { onDarkModeRequested() },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Color.Cyan) },
            text = { Text("Cyan wallpaper") },
            onClick = { onColorWallpaperRequested(Color.Cyan) },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Color.DarkGray) },
            text = { Text("Dark gray wallpaper") },
            onClick = { onColorWallpaperRequested(Color.DarkGray) },
        )
    }
}

@Composable
private fun GridSpecMenuItems(onGridSpecSelected: (GridSpec) -> Unit) {
    listOf(
        GridSpec(4, 3) to "4×3",
        GridSpec(5, 3) to "5×3",
        GridSpec(3, 4) to "3×4",
        GridSpec(4, 5) to "4×5",
    ).forEach { (spec, label) ->
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
            text = { Text(label) },
            onClick = { onGridSpecSelected(spec) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppGridMenuPreview() {
    AppGridMenu(
        isVisible = true,
        offset = DpOffset(0.dp, 0.dp),
        onDismissRequest = {},
        onAppsListRequested = {},
        onSetDefaultLauncherRequested = {},
        onResetDefaultsRequested = {},
        onGridSpecSelected = {},
        onDarkModeRequested = {},
        onColorWallpaperRequested = {},
    )
}
