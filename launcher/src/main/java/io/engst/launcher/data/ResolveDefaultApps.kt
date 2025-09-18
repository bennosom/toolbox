package io.engst.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.net.toUri
import io.engst.core.logError

fun Context.resolveDefaultPhoneApp(): ComponentName? =
   runCatching {
      val packageName = getSystemService(TelecomManager::class.java)?.defaultDialerPackage
      val className =
         packageManager
            .resolveActivity(
               Intent(Intent.ACTION_MAIN).apply {
                  setPackage(packageName)
                  addCategory(Intent.CATEGORY_LAUNCHER)
               },
               PackageManager.MATCH_DEFAULT_ONLY,
            )
            ?.activityInfo
            ?.name
      if (packageName != null && className != null) ComponentName(packageName, className)
      else null
   }
      .onFailure { logError(it) { "Failed to resolve default phone app" } }
      .getOrNull()

fun Context.resolveDefaultMessengerApp(): ComponentName? =
   runCatching {
      val packageName = Telephony.Sms.getDefaultSmsPackage(this@resolveDefaultMessengerApp)
      val className =
         packageManager
            .resolveActivity(
               Intent(Intent.ACTION_MAIN).apply {
                  setPackage(packageName)
                  addCategory(Intent.CATEGORY_LAUNCHER)
               },
               PackageManager.MATCH_DEFAULT_ONLY,
            )
            ?.activityInfo
            ?.name
      if (packageName != null && className != null) ComponentName(packageName, className)
      else null
   }
      .onFailure { logError(it) { "Failed to resolve default messenger app" } }
      .getOrNull()

fun Context.resolveDefaultBrowserApp(): ComponentName? =
   runCatching {
      val packageName =
         packageManager
            .resolveActivity(
               Intent(Intent.ACTION_VIEW, "http://example.com".toUri()),
               PackageManager.MATCH_DEFAULT_ONLY,
            )
            ?.activityInfo
            ?.packageName
      val className =
         packageManager
            .resolveActivity(
               Intent(Intent.ACTION_MAIN).apply {
                  setPackage(packageName)
                  addCategory(Intent.CATEGORY_LAUNCHER)
               },
               PackageManager.MATCH_DEFAULT_ONLY,
            )
            ?.activityInfo
            ?.name
      if (packageName != null && className != null) ComponentName(packageName, className)
      else null
   }
      .onFailure { logError(it) { "Failed to resolve default browser app" } }
      .getOrNull()
