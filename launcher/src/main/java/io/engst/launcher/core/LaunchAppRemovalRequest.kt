package io.engst.launcher.core

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import io.engst.core.logError

fun Context.launchAppRemovalRequest(packageName: String) {
   runCatching {
      startActivity(
         Intent(Intent.ACTION_DELETE).apply {
            data = "package:${packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
         }
      )
   }
      .onFailure {
         logError(it) { "Failed to launch app removal request for package '$packageName'" }
      }
}
