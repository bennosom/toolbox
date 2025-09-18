package io.engst.launcher.data

import io.engst.launcher.model.Cell

typealias AppId = String

data class GridData(
   val cols: Int,
   val rows: Int,
   val grid: List<Map<Cell, AppId?>>?,
   val bar: List<AppId>?,
)
