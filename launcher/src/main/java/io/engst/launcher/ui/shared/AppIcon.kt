package io.engst.launcher.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.engst.launcher.model.App

@Composable
fun AppIcon(app: App, modifier: Modifier = Modifier, size: Dp) {
   val density = LocalDensity.current
   val size = with(density) { size.roundToPx() }
   Image(
      bitmap = app.icon.toBitmap(size, size).asImageBitmap(),
      contentDescription = app.label,
      modifier = modifier.padding(3.dp),
   )
}
