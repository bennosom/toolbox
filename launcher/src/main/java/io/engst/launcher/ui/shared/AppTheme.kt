package io.engst.launcher.ui.shared

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(
    colorScheme: ColorScheme = darkColorScheme(),
    wallpaperState: WallpaperState = WallpaperState(
        isLight = false,
        suggestedForegroundColor = Color.White,
    ),
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWallpaperState provides wallpaperState,
        LocalSpacing provides spacing,
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            content()
        }
    }
}
