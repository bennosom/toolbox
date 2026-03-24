package io.engst.launcher.model

import io.engst.core.scopedLogger

private val logger = scopedLogger("GridCollapse")

/**
 * Removes pages where every cell is vacant (null).
 * The last remaining page is never removed — minimum one page always exists.
 */
fun Grid.collapseEmptyPages(): Grid {
    if (grid.size <= 1) return this

    val nonEmpty = grid.filter { page -> page.values.any { it != null } }
    return if (nonEmpty.isEmpty()) {
        logger.logDebug { "collapseEmptyPages: all pages empty — keeping first page" }
        copy(grid = listOf(grid.first()))
    } else if (nonEmpty.size == grid.size) {
        this
    } else {
        logger.logDebug { "collapseEmptyPages: collapsed ${grid.size - nonEmpty.size} empty page(s)" }
        copy(grid = nonEmpty)
    }
}
