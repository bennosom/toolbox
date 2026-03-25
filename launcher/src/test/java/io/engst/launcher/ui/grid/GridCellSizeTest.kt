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
        // overhead = 3*12 = 36dp, divisor = 5
        // (640 - 36) / 5 = 120.8dp
        val result = calculateCellHeight(
            totalHeight = 640.dp, rows = 4, spacing = 12.dp, barVisible = true,
        )
        assertEquals(120.8f, result.value, 0.01f)
    }

    @Test
    fun `4 rows without bar in 640dp — full height for grid`() {
        // overhead = 36dp, divisor = 4
        // (640 - 36) / 4 = 151dp
        val result = calculateCellHeight(
            totalHeight = 640.dp, rows = 4, spacing = 12.dp, barVisible = false,
        )
        assertEquals(151f, result.value, 0.01f)
    }

    @Test
    fun `5 rows with bar — smaller cells`() {
        // overhead = 3*12 = 36dp, divisor = 6
        // (640 - 36) / 6 = 100.67dp
        val result = calculateCellHeight(
            totalHeight = 640.dp, rows = 5, spacing = 12.dp, barVisible = true,
        )
        assertEquals(100.67f, result.value, 0.01f)
    }

    @Test
    fun `single row with bar`() {
        // overhead = 3*12 = 36dp, divisor = 2
        // (400 - 36) / 2 = 182dp
        val result = calculateCellHeight(
            totalHeight = 400.dp, rows = 1, spacing = 12.dp, barVisible = true,
        )
        assertEquals(182f, result.value, 0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero rows throws`() {
        calculateCellHeight(totalHeight = 640.dp, rows = 0, spacing = 12.dp, barVisible = true)
    }
}
