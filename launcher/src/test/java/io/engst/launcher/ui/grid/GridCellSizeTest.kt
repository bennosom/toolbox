package io.engst.launcher.ui.grid

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GridCellSizeTest {

    @Test
    fun `4 rows with bar in 640dp — cell height accounts for bar row`() {
        // overhead = (4+3)*12 = 84dp, divisor = 5
        // (640 - 84) / 5 = 111.2dp
        val result = calculateCellHeight(
            totalHeight = 640.dp, rows = 4, spacing = 12.dp, barVisible = true,
        )
        assertEquals(111.2f, result.value, 0.01f)
    }

    @Test
    fun `4 rows without bar in 640dp — full height for grid`() {
        // overhead = 84dp, divisor = 4
        // (640 - 84) / 4 = 139dp
        val result = calculateCellHeight(
            totalHeight = 640.dp, rows = 4, spacing = 12.dp, barVisible = false,
        )
        assertEquals(139f, result.value, 0.01f)
    }

    @Test
    fun `5 rows with bar — smaller cells`() {
        // overhead = (5+3)*12 = 96dp, divisor = 6
        // (640 - 96) / 6 = 90.67dp
        val result = calculateCellHeight(
            totalHeight = 640.dp, rows = 5, spacing = 12.dp, barVisible = true,
        )
        assertEquals(90.67f, result.value, 0.01f)
    }

    @Test
    fun `single row with bar`() {
        // overhead = (1+3)*12 = 48dp, divisor = 2
        // (400 - 48) / 2 = 176dp
        val result = calculateCellHeight(
            totalHeight = 400.dp, rows = 1, spacing = 12.dp, barVisible = true,
        )
        assertEquals(176f, result.value, 0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero rows throws`() {
        calculateCellHeight(totalHeight = 640.dp, rows = 0, spacing = 12.dp, barVisible = true)
    }
}
