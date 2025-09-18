package io.engst.launcher.ui.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.shared.AppIcon
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.ScreenInfo

@Composable
fun AppTile(app: App, screenInfo: ScreenInfo, iconSize: Dp, modifier: Modifier = Modifier) {
   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(3.dp),
      horizontalAlignment = Alignment.Companion.CenterHorizontally,
   ) {
      AppIcon(app, size = iconSize)
      Text(
         text = app.label,
         color = LocalWallpaperState.current.suggestedForegroundColor,
         style =
            when (screenInfo) {
               ScreenInfo.MOBILE_PORTRAIT -> MaterialTheme.typography.bodySmall
               else -> MaterialTheme.typography.headlineSmall
            },
         maxLines = 1,
         overflow = TextOverflow.Companion.Ellipsis,
      )
   }
}
