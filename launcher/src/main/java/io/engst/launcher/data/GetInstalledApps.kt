package io.engst.launcher.data

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import io.engst.launcher.model.App

fun LauncherApps.getInstalledApps(packageManager: PackageManager): List<App> =
   getActivityList(null, Process.myUserHandle()).map { info ->
      val packageName = info.componentName.packageName
      val packageInfo =
         runCatching { packageManager.getPackageInfo(packageName, PackageManager.MATCH_ALL) }
            .getOrNull()
      val isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
      val isUpdated = (info.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
      val shortcuts =
         runCatching {
            getShortcuts(
               LauncherApps.ShortcutQuery()
                  .setQueryFlags(
                     LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                       LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                       LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                  )
                  .setPackage(packageName),
               Process.myUserHandle(),
            )
         }
            .getOrNull()
      App(
         id = info.componentName.flattenToString(),
         label = info.label.toString(),
         icon = info.getBadgedIcon(0),
         componentName = info.componentName,
         launchIntent =
            Intent.makeMainActivity(info.componentName).apply {
               addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            },
         shortcuts = shortcuts ?: emptyList(),
         versionName = packageInfo?.versionName ?: "unknown",
         versionCode = packageInfo?.longVersionCode ?: -1L,
         minSdk = info.applicationInfo.minSdkVersion,
         targetSdk = info.applicationInfo.targetSdkVersion,
         lastUpdatedTimeMillis = packageInfo?.lastUpdateTime ?: 0L,
         installedTimeMillis = packageInfo?.firstInstallTime ?: 0L,
         isSystemApp = isSystem || isUpdated,
      )
   }
