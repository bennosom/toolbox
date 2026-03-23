package io.engst.launcher.model

import io.engst.core.scopedLogger

private val logger = scopedLogger("GridMoves")

sealed interface AppLocation {
    data class GridCell(val pageIndex: Int, val cell: Cell) : AppLocation
    data class QuickBarSlot(val index: Int) : AppLocation
}

sealed interface DragDestination {
    data class GridCell(val pageIndex: Int, val cell: Cell) : DragDestination
    data class QuickBarSlot(val index: Int) : DragDestination
}

fun Grid.findAppLocation(appId: String): AppLocation? {
    grid.forEachIndexed { pageIndex, page ->
        page.forEach { (cell, app) ->
            if (app?.id == appId) return AppLocation.GridCell(pageIndex, cell)
        }
    }
    bar.forEachIndexed { index, app ->
        if (app.id == appId) return AppLocation.QuickBarSlot(index)
    }
    return null
}

fun Grid.applyDragMove(appId: String, destination: DragDestination): Grid {
    val origin = findAppLocation(appId) ?: run {
        logger.logWarn { "applyDragMove: app not found appId=$appId" }
        return this
    }
    logger.logDebug { "applyDragMove: appId=$appId origin=$origin destination=$destination" }
    return when (destination) {
        is DragDestination.GridCell -> when (origin) {
            is AppLocation.GridCell -> moveGridToGrid(origin, destination)
            is AppLocation.QuickBarSlot -> moveBarToGrid(origin, destination)
        }
        is DragDestination.QuickBarSlot -> when (origin) {
            is AppLocation.GridCell -> moveGridToBar(origin, destination)
            is AppLocation.QuickBarSlot -> moveBarToBar(origin, destination)
        }
    }
}

fun Grid.moveGridToGrid(
    origin: AppLocation.GridCell,
    destination: DragDestination.GridCell,
): Grid {
    if (origin.pageIndex == destination.pageIndex && origin.cell == destination.cell) return this
    val pages = grid.map { LinkedHashMap(it) }.toMutableList()
    val sourcePage = pages.getOrNull(origin.pageIndex) ?: return this
    val destinationPage = pages.getOrNull(destination.pageIndex) ?: return this
    val sourceApp = sourcePage[origin.cell] ?: return this
    val destinationApp = destinationPage[destination.cell]
    if (destinationApp != null) {
        logger.logDebug { "moveGridToGrid: rejected — destination occupied cell=${destination.cell}" }
        return this
    }
    sourcePage[origin.cell] = null
    destinationPage[destination.cell] = sourceApp
    logger.logDebug { "moveGridToGrid: moved ${sourceApp.id} → page=${destination.pageIndex} cell=${destination.cell}" }
    return copy(grid = pages.map { it.toMap() })
}

fun Grid.moveBarToGrid(
    origin: AppLocation.QuickBarSlot,
    destination: DragDestination.GridCell,
): Grid {
    if (origin.index !in bar.indices) return this
    val pages = grid.map { LinkedHashMap(it) }.toMutableList()
    val destinationPage = pages.getOrNull(destination.pageIndex) ?: return this
    val destinationApp = destinationPage[destination.cell]
    if (destinationApp != null) {
        logger.logDebug { "moveBarToGrid: rejected — destination occupied cell=${destination.cell}" }
        return this
    }
    val barList = bar.toMutableList()
    val sourceApp = barList.removeAt(origin.index)
    destinationPage[destination.cell] = sourceApp
    logger.logDebug { "moveBarToGrid: moved ${sourceApp.id} from bar[${origin.index}] to grid" }
    return copy(grid = pages.map { it.toMap() }, bar = barList)
}

fun Grid.moveGridToBar(
    origin: AppLocation.GridCell,
    destination: DragDestination.QuickBarSlot,
): Grid {
    val pages = grid.map { LinkedHashMap(it) }.toMutableList()
    val sourcePage = pages.getOrNull(origin.pageIndex) ?: return this
    val sourceApp = sourcePage[origin.cell] ?: return this
    val barList = bar.toMutableList()
    val insertIndex = destination.index.coerceIn(0, barList.size)
    val displacedApp = if (insertIndex < barList.size) barList[insertIndex] else null
    if (insertIndex < barList.size) {
        barList[insertIndex] = sourceApp
    } else {
        barList.add(sourceApp)
    }
    sourcePage[origin.cell] = displacedApp
    logger.logDebug { "moveGridToBar: moved ${sourceApp.id} to bar[$insertIndex], displaced=$displacedApp" }
    return copy(grid = pages.map { it.toMap() }, bar = barList)
}

fun Grid.moveBarToBar(
    origin: AppLocation.QuickBarSlot,
    destination: DragDestination.QuickBarSlot,
): Grid {
    if (origin.index == destination.index) return this
    if (origin.index !in bar.indices) return this
    val barList = bar.toMutableList()
    val item = barList.removeAt(origin.index)
    val targetIndex = destination.index.coerceIn(0, barList.size)
    barList.add(targetIndex, item)
    logger.logDebug { "moveBarToBar: moved ${item.id} from [${origin.index}] to [$targetIndex]" }
    return copy(bar = barList)
}
