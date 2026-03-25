package io.engst.launcher.ui.grid

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.LocalWallpaperState
import io.engst.launcher.ui.shared.spacing

@Composable
fun PageIndicator(count: Int, currentIndex: Int, modifier: Modifier = Modifier) {
  if (count <= 1) return
  Row(
      horizontalArrangement =
          Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier,
  ) {
    repeat(count) { i ->
      val isActive = i == currentIndex
      val color =
          if (isActive) LocalWallpaperState.current.suggestedForegroundColor.copy(alpha = 0.8f)
          else LocalWallpaperState.current.suggestedForegroundColor.copy(alpha = 0.4f)
      val width by animateDpAsState(if (isActive) 12.dp else 6.dp)
      Box(
          modifier = Modifier.width(12.dp).height(6.dp).clip(CircleShape),
          contentAlignment = Alignment.Center,
      ) {
        Box(modifier = Modifier.width(width).height(6.dp).clip(CircleShape).background(color))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PageIndicatorPreview() {
  AppTheme { PageIndicator(count = 4, currentIndex = 1) }
}
