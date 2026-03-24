package io.engst.launcher.data.store

import io.engst.launcher.data.GridData
import io.engst.launcher.data.proto.GridCellProto
import io.engst.launcher.data.proto.GridDataProto
import io.engst.launcher.data.proto.GridPageProto
import io.engst.launcher.model.Cell

fun GridDataProto.toGridData(): GridData {
    val gridPages = if (hasUserGrid) {
        gridList.map { page ->
            page.cellsList.associate { cell ->
                Cell(cell.col, cell.row) to cell.appId.ifEmpty { null }
            }
        }
    } else {
        null
    }

    val barList = if (hasUserBar) {
        barList.filter { it.isNotEmpty() }
    } else {
        null
    }

    return GridData(
        cols = if (cols > 0) cols else DEFAULT_COLS,
        rows = if (rows > 0) rows else DEFAULT_ROWS,
        grid = gridPages,
        bar = barList,
        hasUserGrid = hasUserGrid,
        hasUserBar = hasUserBar,
        barSlots = barSlots,
    )
}

fun GridData.toProto(): GridDataProto =
    GridDataProto.newBuilder().apply {
        cols = this@toProto.cols
        rows = this@toProto.rows
        hasUserGrid = this@toProto.hasUserGrid
        hasUserBar = this@toProto.hasUserBar
        barSlots = this@toProto.barSlots

        this@toProto.grid?.forEach { page ->
            addGrid(
                GridPageProto.newBuilder().apply {
                    page.forEach { (cell, appId) ->
                        addCells(
                            GridCellProto.newBuilder().apply {
                                col = cell.col
                                row = cell.row
                                this.appId = appId ?: ""
                            }
                        )
                    }
                }
            )
        }

        this@toProto.bar?.forEach { appId -> addBar(appId) }
    }.build()

private const val DEFAULT_COLS = 4
private const val DEFAULT_ROWS = 4
