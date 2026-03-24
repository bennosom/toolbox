package io.engst.launcher.data

import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.GridSpec

/**
 * Builds the initial grid layout from a flat list of apps, filling cells column-by-column.
 * Creates as many pages as needed to accommodate all apps.
 */
fun buildGrid(apps: List<App>, spec: GridSpec): List<Map<Cell, App?>> = buildList {
   val pageCapacity = spec.cols * spec.rows
   if (pageCapacity <= 0) return@buildList
   val totalPages = if (apps.isEmpty()) 0 else (apps.size + pageCapacity - 1) / pageCapacity
   var index = 0
   (0 until totalPages).forEach { _ ->
      val page = buildMap {
         (0 until spec.cols).forEach { col ->
            (0 until spec.rows).forEach { row ->
               val app = if (index < apps.size) apps[index++] else null
               put(Cell(col, row), app)
            }
         }
      }
      add(page)
   }
}

/**
 * Reconstructs the grid from a persisted layout, resolving stored app IDs against the
 * current list of installed apps. Newly installed apps not yet in the layout are appended
 * after the last occupied cell, creating new pages as needed.
 */
fun buildGridFromStore(
   storeGrid: List<Map<Cell, AppId?>>,
   apps: List<App>,
   barAppIds: List<String>,
   spec: GridSpec,
): List<Map<Cell, App?>> {
   val orderedCells =
      (0 until spec.cols).flatMap { col ->
         (0 until spec.rows).map { row -> Cell(col, row) }
      }

   val pages =
      storeGrid
         .map { page ->
            val linked = LinkedHashMap<Cell, App?>(orderedCells.size)
            page.forEach { (cell, appId) ->
               val app = apps.find { it.componentName.flattenToString() == appId }
               linked[cell] = app
            }
            linked
         }
         .toMutableList()

   val pageCapacity = orderedCells.size

   fun normalizePage(index: Int) {
      val existing = pages[index]
      if (
         existing.size == pageCapacity &&
         orderedCells.all { existing.containsKey(it) }
      ) return
      val normalized = LinkedHashMap<Cell, App?>(pageCapacity)
      orderedCells.forEach { c -> normalized[c] = existing[c] }
      pages[index] = normalized
   }
   for (i in pages.indices) normalizePage(i)

   val placedIds = buildSet {
      pages.forEach { page -> page.values.forEach { app -> app?.let { add(it.id) } } }
   }

   val toAppend =
      apps.filterNot { barAppIds.contains(it.id) || placedIds.contains(it.id) }

   var lastOccupied = -1
   pages.forEachIndexed { pageIndex, page ->
      orderedCells.forEachIndexed { cellIndex, cell ->
         if (page[cell] != null) {
            lastOccupied = pageIndex * pageCapacity + cellIndex
         }
      }
   }

   var nextIndex = lastOccupied + 1
   toAppend.forEach { app ->
      val pageIndex = nextIndex / pageCapacity
      val cellIndex = nextIndex % pageCapacity
      while (pageIndex >= pages.size) {
         val newPage = LinkedHashMap<Cell, App?>(pageCapacity)
         orderedCells.forEach { c -> newPage[c] = null }
         pages.add(newPage)
      }
      normalizePage(pageIndex)
      val targetCell = orderedCells[cellIndex]
      pages[pageIndex][targetCell] = app
      nextIndex++
   }

   return pages.map { it.toMap() }
}
