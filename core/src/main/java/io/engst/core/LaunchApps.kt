package io.engst.core

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

fun Context.launchOnDisplay(
    component: ComponentName,
    displayId: Int = display.displayId,
    extras: Bundle = Bundle.EMPTY,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
) {
  startActivity(
      Intent.makeMainActivity(component).apply {
        addFlags(flags)
        putExtras(extras)
      },
      ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())
}
