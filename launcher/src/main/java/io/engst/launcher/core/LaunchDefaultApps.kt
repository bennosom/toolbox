package io.engst.launcher.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.engst.core.logError

fun Context.launchDefaultAppSettings() {
   listOf(
      Intent(Settings.ACTION_HOME_SETTINGS),
      Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
   )
      .forEach { intent ->
         try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
         } catch (ex: Exception) {
            logError(ex) { "Failed to launch $intent" }
         }
      }
}
