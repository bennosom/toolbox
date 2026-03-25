package io.engst.core.apps

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import io.engst.core.logError
import io.engst.core.logWarn

fun Context.launchActivity(
   component: ComponentName,
   displayId: Int? = null,
   extras: Bundle = Bundle.EMPTY,
   flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
) {
   runCatching {
      val resolvedDisplayId = displayId ?: resolveDisplayId()
      startActivity(
         Intent.makeMainActivity(component).apply {
            addFlags(flags)
            putExtras(extras)
         },
         ActivityOptions.makeBasic().setLaunchDisplayId(resolvedDisplayId).toBundle(),
      )
   }
      .onFailure { logError(it) { "Failed to launch $component" } }
}

private fun Context.resolveDisplayId(): Int =
   runCatching { display.displayId }
      .getOrElse {
         logWarn { "Could not obtain display from context, falling back to DEFAULT_DISPLAY" }
         Display.DEFAULT_DISPLAY
      }

fun Context.launchIntent(
   intent: Intent,
   flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
) {
   runCatching { startActivity(intent.apply { addFlags(flags) }) }
      .onFailure { logError(it) { "Failed to launch $intent" } }
}
