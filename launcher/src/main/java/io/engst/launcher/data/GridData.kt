package io.engst.launcher.data

import io.engst.launcher.model.Cell
import io.engst.launcher.ui.shared.DarkModePreference

typealias AppId = String

data class GridData(
   val cols: Int,
   val rows: Int,
   val grid: List<Map<Cell, AppId?>>?,
   val bar: List<AppId>?,
   val hasUserGrid: Boolean = false,
   val hasUserBar: Boolean = false,
   val barSlots: Int = 0,
   val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
   val isBarVisible: Boolean = true,
)
