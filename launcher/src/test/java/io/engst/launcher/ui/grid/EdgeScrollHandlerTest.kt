package io.engst.launcher.ui.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for edge-scroll detection — covers STORY-002-4 AC:
 * "Holding the dragged item at a page edge triggers navigation to the adjacent page."
 */
@RunWith(RobolectricTestRunner::class)
class EdgeScrollHandlerTest {

    private val pagerWidth = 1000f

    @Test
    fun returns_previous_page_when_pointer_is_near_left_edge() {
        val target = detectEdgeScrollTarget(
            pointerXInPager = 50f,  // 5 % — inside 15 % threshold
            pagerWidth = pagerWidth,
            currentPage = 2,
            pageCount = 4,
        )
        assertEquals(1, target)
    }

    @Test
    fun returns_next_page_when_pointer_is_near_right_edge() {
        val target = detectEdgeScrollTarget(
            pointerXInPager = 960f, // 96 % — inside 15 % threshold from right
            pagerWidth = pagerWidth,
            currentPage = 1,
            pageCount = 4,
        )
        assertEquals(2, target)
    }

    @Test
    fun returns_null_when_pointer_is_in_safe_centre_zone() {
        val target = detectEdgeScrollTarget(
            pointerXInPager = 500f,
            pagerWidth = pagerWidth,
            currentPage = 1,
            pageCount = 4,
        )
        assertNull(target)
    }

    @Test
    fun returns_null_on_left_edge_when_already_on_first_page() {
        val target = detectEdgeScrollTarget(
            pointerXInPager = 10f,
            pagerWidth = pagerWidth,
            currentPage = 0,
            pageCount = 3,
        )
        assertNull(target)
    }

    @Test
    fun returns_null_on_right_edge_when_already_on_last_page() {
        val target = detectEdgeScrollTarget(
            pointerXInPager = 990f,
            pagerWidth = pagerWidth,
            currentPage = 2,
            pageCount = 3,
        )
        assertNull(target)
    }

    @Test
    fun returns_null_when_pointer_is_exactly_at_threshold_boundary() {
        val threshold = pagerWidth * 0.15f // 150f
        val target = detectEdgeScrollTarget(
            pointerXInPager = threshold, // at boundary — not strictly less than
            pagerWidth = pagerWidth,
            currentPage = 1,
            pageCount = 3,
        )
        assertNull(target)
    }
}
