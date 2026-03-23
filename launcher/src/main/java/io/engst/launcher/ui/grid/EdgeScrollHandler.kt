package io.engst.launcher.ui.grid

import io.engst.core.scopedLogger

private val logger = scopedLogger("EdgeScrollHandler")

private const val EDGE_THRESHOLD_FRACTION = 0.15f

/**
 * Determines whether a drag pointer position near the left or right edge of the
 * pager viewport should trigger navigation to an adjacent page.
 *
 * Returns the target page index to navigate to, or null if no edge trigger applies.
 */
fun detectEdgeScrollTarget(
    pointerXInPager: Float,
    pagerWidth: Float,
    currentPage: Int,
    pageCount: Int,
): Int? {
    val threshold = pagerWidth * EDGE_THRESHOLD_FRACTION
    return when {
        pointerXInPager < threshold && currentPage > 0 -> {
            val target = currentPage - 1
            logger.logDebug { "edge scroll left: currentPage=$currentPage → target=$target" }
            target
        }
        pointerXInPager > pagerWidth - threshold && currentPage < pageCount - 1 -> {
            val target = currentPage + 1
            logger.logDebug { "edge scroll right: currentPage=$currentPage → target=$target" }
            target
        }
        else -> null
    }
}
