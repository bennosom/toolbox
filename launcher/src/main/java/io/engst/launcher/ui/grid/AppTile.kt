package io.engst.launcher.ui.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.shared.AppIcon
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.ScreenInfo
import io.engst.launcher.ui.shared.previewApp
import io.engst.launcher.ui.shared.rememberScreenInfo
import io.engst.launcher.ui.shared.spacing

@Composable
fun AppTile(app: App, iconSize: Dp, modifier: Modifier = Modifier) {
   val screenInfo = rememberScreenInfo()
   Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Spacer(Modifier.size(MaterialTheme.spacing.small))
      AppIcon(app, size = iconSize)
      Spacer(Modifier.size(MaterialTheme.spacing.small))
      Text(
         text = app.label,
         color = LocalWallpaperState.current.suggestedForegroundColor,
         style =
            when (screenInfo) {
               ScreenInfo.MOBILE_PORTRAIT -> MaterialTheme.typography.bodySmall
               else -> MaterialTheme.typography.headlineSmall
            },
         maxLines = 1,
         overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.size(MaterialTheme.spacing.small))
   }
}

@Preview(showBackground = true, widthDp = 80, heightDp = 100, backgroundColor = 0xFF333333)
@Composable
private fun AppTileMobilePreview() {
   AppTheme {
      AppTile(
         app = previewApp("chrome", "Chrome"),
         iconSize = ICON_SIZE,
      )
   }
}

@Preview(showBackground = true, widthDp = 120, heightDp = 140, backgroundColor = 0xFF333333)
@Composable
private fun AppTileTabletPreview() {
   AppTheme {
      AppTile(
         app = previewApp("maps", "Maps"),
         iconSize = ICON_SIZE,
      )
   }
}