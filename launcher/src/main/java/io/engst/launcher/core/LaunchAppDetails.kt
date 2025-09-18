package io.engst.launcher.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import io.engst.core.logError

fun Context.launchAppDetails(packageName: String) {
   runCatching {
      startActivity(
         Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${packageName}".toUri())
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
      )
   }
      .onFailure { logError(it) { "Failed to launch app details for package '$packageName'" } }
}