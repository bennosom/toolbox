package io.engst.launcher.model

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GridMovesPlaceholderPageTest {

    private fun makeApp(id: String) = App(
        id = id,
        label = id,
        icon = ColorDrawable(),
        componentName = ComponentName("pkg.$id", "cls.$id"),
        launchIntent = Intent(),
        shortcuts = emptyList(),
        versionName = "1.0",
        versionCode = 1L,
        minSdk = 31,
        targetSdk = 36,
        lastUpdatedTimeMillis = 0L,
        installedTimeMillis = 0L,
        isSystemApp = false,
    )

    private val spec = GridSpec(cols = 2, rows = 2)
    private val appA = makeApp("A")
    private val appB = makeApp("B")
    private val appC = makeApp("C")

    private fun baseGrid(): Grid {
        val page = mapOf(
            Cell(0, 0) to appA,
            Cell(1, 0) to appB,
            Cell(0, 1) to null,
            Cell(1, 1) to null,
        )
        return Grid(spec = spec, grid = listOf(page), bar = emptyList())
    }

    @Test
    fun moveGridToGrid_appends_new_page_when_destination_is_trailing_placeholder_index() {
        val grid = baseGrid()
        val result = grid.moveGridToGrid(
            origin = AppLocation.GridCell(pageIndex = 0, cell = Cell(0, 0)),
            destination = DragDestination.GridCell(pageIndex = 1, cell = Cell(1, 1)),
        )

        assertEquals(2, result.grid.size)
        assertEquals(null, result.grid[0][Cell(0, 0)])
        assertEquals(appA, result.grid[1][Cell(1, 1)])
    }

    @Test
    fun moveBarToGrid_appends_new_page_when_destination_is_trailing_placeholder_index() {
        val grid = baseGrid().copy(bar = listOf(appC))
        val result = grid.moveBarToGrid(
            origin = AppLocation.QuickBarSlot(index = 0),
            destination = DragDestination.GridCell(pageIndex = 1, cell = Cell(0, 0)),
        )

        assertEquals(2, result.grid.size)
        assertEquals(appC, result.grid[1][Cell(0, 0)])
        assertEquals(emptyList<App>(), result.bar)
    }

    @Test
    fun moveGridToGrid_is_no_op_when_destination_page_skips_trailing_placeholder() {
        val grid = baseGrid()
        val result = grid.moveGridToGrid(
            origin = AppLocation.GridCell(pageIndex = 0, cell = Cell(0, 0)),
            destination = DragDestination.GridCell(pageIndex = 2, cell = Cell(1, 1)),
        )

        assertSame(grid, result)
    }
}
