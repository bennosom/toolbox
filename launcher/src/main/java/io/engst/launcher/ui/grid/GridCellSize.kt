package io.engst.launcher.ui.grid

import androidx.compose.ui.unit.Dp

/**
 * Calculates the uniform cell height so that the grid rows, inter-row spacing,
 * quick-bar (when visible), indicator, spacer, and pager padding all fit exactly
 * within [totalHeight].
 *
 * Fixed overhead = `(rows + 3) × spacing`
 * (pager top/bottom padding 2×, indicator 1×, spacer 1×, inter-row gaps (rows−1)×).
 *
 * With bar: `cellHeight = (totalHeight − overhead) / (rows + 1)`
 * Without bar: `cellHeight = (totalHeight − overhead) / rows`
 */
fun calculateCellHeight(totalHeight: Dp, rows: Int, spacing: Dp, barVisible: Boolean): Dp {
    require(rows > 0) { "rows must be positive, was $rows" }
    val overhead = spacing * (rows + 3)
    val divisor = if (barVisible) rows + 1 else rows
    return (totalHeight - overhead) / divisor
}
