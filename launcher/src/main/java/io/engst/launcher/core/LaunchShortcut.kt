package io.engst.launcher.core

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import io.engst.core.logError

fun Context.launchShortcut(shortcut: ShortcutInfo) {
   runCatching {
      val launcherApps = getSystemService(LauncherApps::class.java)
      launcherApps?.startShortcut(
         shortcut.`package`,
         shortcut.id,
         null,
         null,
         Process.myUserHandle(),
      )
   }
      .onFailure {
         logError(it) {
            "Failed to launch shortcut: ${shortcut.id} in package ${shortcut.`package`}"
         }
      }
}
