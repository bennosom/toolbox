package io.engst.devicetool

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import io.engst.core.apps.launchIntent

data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val installedTimeMillis: Long,
    val lastUpdatedTimeMillis: Long,
    val isSystemApp: Boolean,
    val icon: Drawable?,
) {
  val id: String = packageName
}

fun Context.loadInstalledApps(): List<InstalledApp> {
  val packageManager = packageManager
  val packages = packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
  return packages.mapNotNull { packageInfo ->
    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
    InstalledApp(
        packageName = packageInfo.packageName,
        label = packageManager.getApplicationLabel(appInfo).toString(),
        versionName = packageInfo.versionName ?: "",
        versionCode = packageInfo.longVersionCode,
        targetSdk = appInfo.targetSdkVersion,
        minSdk = appInfo.minSdkVersion,
        installedTimeMillis = packageInfo.firstInstallTime,
        lastUpdatedTimeMillis = packageInfo.lastUpdateTime,
        isSystemApp = appInfo.isSystemApp(),
        icon = runCatching { packageManager.getApplicationIcon(appInfo) }.getOrNull(),
    )
  }
}

fun Context.launchInstalledApp(packageName: String) {
  val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
  launchIntent(launchIntent)
}

fun Context.openAppDetails(packageName: String) {
  launchIntent(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
      },
  )
}

fun Context.requestAppRemoval(packageName: String) {
  launchIntent(
      Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
      },
  )
}

private fun ApplicationInfo.isSystemApp(): Boolean {
  val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
  return (flags and systemFlags) != 0
}
