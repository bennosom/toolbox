package io.engst.launcher.ui.shared

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

enum class ScreenWidth {
  SMALL,
  MEDIUM,
  LARGE
}

enum class ScreenHeight {
  SMALL,
  MEDIUM,
  LARGE
}

enum class ScreenInfo {
  MOBILE,
  MOBILE_PORTRAIT,
  MOBILE_LANDSCAPE,
  TABLET_PORTRAIT,
  TABLET_LANDSCAPE,
  TABLET,
  DESKTOP,
  FOLDABLE_BOOK,
  FOLDABLE_TABLETOP,
  FOLDABLE_FLAT;

  companion object {

     fun fromWindowInfo(windowInfo: WindowAdaptiveInfo): ScreenInfo {
      val windowSizeClass = windowInfo.windowSizeClass
      val hingeList = windowInfo.windowPosture.hingeList

      return if (hingeList.isNotEmpty()) {
        when {
          hingeList.any { !it.isVertical && it.isSeparating } -> FOLDABLE_TABLETOP

          hingeList.any { it.isVertical && it.isSeparating } -> FOLDABLE_BOOK

          else -> FOLDABLE_FLAT
        }
      } else {
        val width =
            when {
              windowSizeClass.isWidthAtLeastBreakpoint(
                  WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> ScreenWidth.LARGE

              windowSizeClass.isWidthAtLeastBreakpoint(
                  WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> ScreenWidth.MEDIUM

              else -> ScreenWidth.SMALL
            }

        val height =
            when {
              windowSizeClass.isHeightAtLeastBreakpoint(
                  WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> ScreenHeight.LARGE

              windowSizeClass.isHeightAtLeastBreakpoint(
                  WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> ScreenHeight.MEDIUM

              else -> ScreenHeight.SMALL
            }

        /**
         * TODO: Fix logic - first determine portrait/landscape orientation, then decide form factor
         *   based on smallest edge
         */
        when {
          width == ScreenWidth.SMALL &&
              (height == ScreenHeight.MEDIUM || height == ScreenHeight.LARGE) -> MOBILE_PORTRAIT

          (width == ScreenWidth.MEDIUM || width == ScreenWidth.LARGE) &&
              height == ScreenHeight.SMALL -> MOBILE_LANDSCAPE

          width == ScreenWidth.MEDIUM && height == ScreenHeight.LARGE -> TABLET_PORTRAIT

          width == ScreenWidth.LARGE && height == ScreenHeight.MEDIUM -> TABLET_LANDSCAPE

          else -> DESKTOP
        }
      }
    }
  }
}

@Composable
fun rememberScreenInfo(): ScreenInfo {
  val windowInfo = currentWindowAdaptiveInfo()
   return ScreenInfo.fromWindowInfo(windowInfo)
}
