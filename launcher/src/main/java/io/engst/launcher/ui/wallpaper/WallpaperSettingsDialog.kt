package io.engst.launcher.ui.wallpaper

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.engst.launcher.ui.shared.AppTheme
import kotlin.math.roundToInt

@Composable
fun WallpaperSettingsDialog(
    onDismissRequest: () -> Unit,
    onSetColorRequested: (Int) -> Unit,
    onOpenWallpaperPickerRequested: () -> Unit,
    onOpenPhotoPickerRequested: () -> Unit,
) {
    var isColorPickerVisible by remember { mutableStateOf(false) }

    if (isColorPickerVisible) {
        ColorPickerDialog(
            onDismissRequest = { isColorPickerVisible = false },
            onColorPicked = { color ->
                isColorPickerVisible = false
                onDismissRequest()
                onSetColorRequested(color)
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Wallpaper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isColorPickerVisible = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Set color")
                }
                Button(
                    onClick = {
                        onDismissRequest()
                        onOpenPhotoPickerRequested()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choose photo")
                }
                Button(
                    onClick = {
                        onDismissRequest()
                        onOpenWallpaperPickerRequested()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open wallpaper picker")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onColorPicked: (Int) -> Unit,
) {
    var red by remember { mutableIntStateOf(255) }
    var green by remember { mutableIntStateOf(255) }
    var blue by remember { mutableIntStateOf(255) }
    val selectedColor = Color(AndroidColor.rgb(red, green, blue))

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Pick color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(selectedColor),
                )
                ColorChannelSlider(
                    label = "Red",
                    value = red,
                    onValueChange = { red = it },
                )
                ColorChannelSlider(
                    label = "Green",
                    value = green,
                    onValueChange = { green = it },
                )
                ColorChannelSlider(
                    label = "Blue",
                    value = blue,
                    onValueChange = { blue = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorPicked(AndroidColor.rgb(red, green, blue)) },
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value.toString(), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { sliderValue ->
                onValueChange(sliderValue.roundToInt().coerceIn(0, 255))
            },
            valueRange = 0f..255f,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WallpaperSettingsDialogPreview() {
    AppTheme {
        WallpaperSettingsDialog(
            onDismissRequest = {},
            onSetColorRequested = {},
            onOpenWallpaperPickerRequested = {},
            onOpenPhotoPickerRequested = {},
        )
    }
}
