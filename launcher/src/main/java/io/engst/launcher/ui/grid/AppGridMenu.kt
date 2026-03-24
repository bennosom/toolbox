package io.engst.launcher.ui.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.GridSpec

private const val MIN_GRID_SIZE = 3
private const val MAX_GRID_SIZE = 8

@Composable
fun AppGridMenu(
    isVisible: Boolean,
    offset: DpOffset,
    isDefaultLauncher: Boolean,
    currentGridSpec: GridSpec,
    onDismissRequest: () -> Unit,
    onAppsListRequested: () -> Unit,
    onSetDefaultLauncherRequested: () -> Unit,
    onGridSpecSelected: (GridSpec) -> Unit,
) {
    DropdownMenu(
        expanded = isVisible,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.requiredSizeIn(minWidth = 180.dp),
    ) {
        if (!isDefaultLauncher) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Rocket, contentDescription = null) },
                text = { Text("Set as default launcher") },
                onClick = { onSetDefaultLauncherRequested() },
            )
        }
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
            text = { Text("Apps list") },
            onClick = { onAppsListRequested() },
        )
        GridLayoutEditor(
            currentSpec = currentGridSpec,
            onGridSpecSelected = onGridSpecSelected,
        )
    }
}

@Composable
private fun GridLayoutEditor(
    currentSpec: GridSpec,
    onGridSpecSelected: (GridSpec) -> Unit,
) {
    var cols by remember(currentSpec) { mutableIntStateOf(currentSpec.cols) }
    var rows by remember(currentSpec) { mutableIntStateOf(currentSpec.rows) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row {
                HeaderCell("Columns")
                HeaderCell("Rows")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpinBox(
                    value = cols,
                    onValueChange = { newCols ->
                        cols = newCols
                        onGridSpecSelected(GridSpec(newCols, rows))
                    },
                )
                SpinBox(
                    value = rows,
                    onValueChange = { newRows ->
                        rows = newRows
                        onGridSpecSelected(GridSpec(cols, newRows))
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.SpinBox(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f),
    ) {
        IconButton(
            onClick = { onValueChange((value - 1).coerceIn(MIN_GRID_SIZE, MAX_GRID_SIZE)) },
            enabled = value > MIN_GRID_SIZE,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(
            onClick = { onValueChange((value + 1).coerceIn(MIN_GRID_SIZE, MAX_GRID_SIZE)) },
            enabled = value < MAX_GRID_SIZE,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppGridMenuPreview() {
    AppGridMenu(
        isVisible = true,
        offset = DpOffset(0.dp, 0.dp),
        isDefaultLauncher = false,
        currentGridSpec = GridSpec(4, 4),
        onDismissRequest = {},
        onAppsListRequested = {},
        onSetDefaultLauncherRequested = {},
        onGridSpecSelected = {},
    )
}
