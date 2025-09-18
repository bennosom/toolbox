package io.engst.core.apps

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.engst.core.logError

fun Context.launchActivity(
   component: ComponentName,
   displayId: Int = display.displayId,
   extras: Bundle = Bundle.EMPTY,
   flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
) {
   runCatching {
      startActivity(
         Intent.makeMainActivity(component).apply {
            addFlags(flags)
            putExtras(extras)
         },
         ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle(),
      )
   }
      .onFailure { logError(it) { "Failed to launch $component" } }
}

fun Context.launchIntent(
   intent: Intent,
   flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
) {
   runCatching { startActivity(intent.apply { addFlags(flags) }) }
      .onFailure { logError(it) { "Failed to launch $intent" } }
}
