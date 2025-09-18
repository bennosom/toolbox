package io.engst.launcher.model

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable

data class Cell(val col: Int, val row: Int) {
   override fun toString(): String {
      return "(${col}/${row})"
   }
}

data class App(
   val id: String,
   val label: String,
   val icon: Drawable,
   val componentName: ComponentName,
   val launchIntent: Intent,
   val shortcuts: List<ShortcutInfo>,
   val versionName: String,
   val versionCode: Long,
   val minSdk: Int,
   val targetSdk: Int,
   val lastUpdatedTimeMillis: Long,
   val installedTimeMillis: Long,
   val isSystemApp: Boolean,
) {
   override fun toString(): String {
      return "App($id)"
   }
}

data class GridSpec(val cols: Int, val rows: Int) {
   override fun toString(): String {
      return "${cols}x${rows}"
   }
}

data class Grid(val spec: GridSpec, val grid: List<Map<Cell, App?>>, val bar: List<App>)
