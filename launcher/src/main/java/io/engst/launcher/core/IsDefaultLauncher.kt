package io.engst.launcher.core

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

fun Context.isDefaultLauncher(): Boolean {
   // Try RoleManager when possible
   val roleManager = getSystemService(RoleManager::class.java)
   if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
      try {
         if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return true
      } catch (_: Exception) {
         // ignore and fallback below
      }
   }

   val pm = packageManager
   val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
   val resolve = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
   val defaultPackage = resolve?.activityInfo?.packageName
   return defaultPackage == packageName
}