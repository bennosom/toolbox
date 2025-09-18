package io.engst.launcher.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.engst.launcher.ui.shared.LocalWallpaperState

@Composable
fun PageIndicator(count: Int, currentIndex: Int, modifier: Modifier = Modifier) {
   if (count <= 1) return
   Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Companion.CenterHorizontally),
      verticalAlignment = Alignment.Companion.CenterVertically,
      modifier = modifier,
   ) {
      repeat(count) { i ->
         val isActive = i == currentIndex
         val color =
            if (isActive) LocalWallpaperState.current.suggestedForegroundColor.copy(alpha = 0.8f)
            else LocalWallpaperState.current.suggestedForegroundColor.copy(alpha = 0.4f)
         Box(
            modifier =
               Modifier.Companion
                  .size(if (isActive) 7.dp else 6.dp)
                  .clip(CircleShape)
                  .background(color)
         )
      }
   }
}
