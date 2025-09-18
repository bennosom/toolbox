package io.engst.devicetool

import android.app.Activity
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.waterfall
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily

@Composable
fun ColumnScope.DeviceInfo(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val config = LocalConfiguration.current
  val layoutDirection = LocalLayoutDirection.current

  val infoText = buildString {
    appendLine()
    appendLine("DISPLAY")
    val displayManager = context.getSystemService(DisplayManager::class.java)
    displayManager.displays
        .find { it.displayId == context.display.displayId }
        ?.let { display ->
          appendLine("  displayId = ${display.displayId}")
          appendLine("  name = ${display.name}")
          appendLine("  width = ${display.width}")
          appendLine("  height = ${display.height}")
          appendLine("  refreshRate = ${display.refreshRate}")
        }

    appendLine()
    appendLine("WINDOW METRICS")
    val metricsString = run {
      val activity = context as? Activity
      if (activity != null) {
        val bounds = context.windowManager.maximumWindowMetrics.bounds
        "  bounds = [${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}]"
      } else {
        "  (Window metrics unavailable)"
      }
    }
    appendLine(metricsString)
    appendLine()

    appendLine("WINDOW INSETS")
    val insetProps =
        listOf(
            "statusBars" to WindowInsets.statusBars,
            "navigationBars" to WindowInsets.navigationBars,
            "ime" to WindowInsets.ime,
            "systemGestures" to WindowInsets.systemGestures,
            "displayCutout" to WindowInsets.displayCutout,
            "tappableElement" to WindowInsets.tappableElement,
            "captionBar" to WindowInsets.captionBar,
            "waterfall" to WindowInsets.waterfall)
    insetProps.forEach { (name, inset) ->
      val left = with(density) { inset.getLeft(density, layoutDirection) }
      val top = with(density) { inset.getTop(density) }
      val right = with(density) { inset.getRight(density, layoutDirection) }
      val bottom = with(density) { inset.getBottom(density) }
      appendLine("  $name = [$left, $top, $right, $bottom]")
    }

    appendLine()
    appendLine("RESOURCES")
    appendLine("  density = ${density.density}")
    appendLine("  fontScale = ${density.fontScale}")
    appendLine("  orientation = ${config.orientation}")
    appendLine("  screenWidthDp = ${config.screenWidthDp}")
    appendLine("  screenHeightDp = ${config.screenHeightDp}")
    appendLine("  smallestScreenWidthDp = ${config.smallestScreenWidthDp}")
    appendLine("  densityDpi = ${config.densityDpi}")
    appendLine("  locale = ${config.locales.get(0)}")
    appendLine(
        "  nightMode = " +
            when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
              Configuration.UI_MODE_NIGHT_YES -> "yes"
              Configuration.UI_MODE_NIGHT_NO -> "no"
              Configuration.UI_MODE_NIGHT_UNDEFINED -> "undefined"
              else -> "unknown"
            })

    appendLine()
    appendLine("ANDROID")
    appendLine("  manufacturer = ${Build.MANUFACTURER}")
    appendLine("  model = ${Build.MODEL}")
    appendLine("  device = ${Build.DEVICE}")
    appendLine("  product = ${Build.PRODUCT}")
    appendLine("  sdk_int = ${Build.VERSION.SDK_INT}")
    appendLine("  release = ${Build.VERSION.RELEASE}")
  }

  Text(text = infoText, fontFamily = FontFamily.Monospace, modifier = modifier)
}
