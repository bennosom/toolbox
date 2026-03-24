package io.engst.launcher.model

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for every acceptance criterion in STORY-002-1, STORY-002-2, and STORY-002-3.
 */
@RunWith(RobolectricTestRunner::class)
class GridMovesTest {

    // ---------------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------------

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
    private val appD = makeApp("D")

    /** Single page: A(0,0) B(1,0) C(0,1) null(1,1) */
    private fun singlePageGrid(): Grid {
        val page = mapOf(
            Cell(0, 0) to appA,
            Cell(1, 0) to appB,
            Cell(0, 1) to appC,
            Cell(1, 1) to null,
        )
        return Grid(spec = spec, grid = listOf(page), bar = emptyList())
    }

    // ---------------------------------------------------------------------------
    // findAppLocation
    // ---------------------------------------------------------------------------

    @Test
    fun findAppLocation_returns_grid_cell_for_app_on_grid() {
        val grid = singlePageGrid()
        assertEquals(AppLocation.GridCell(pageIndex = 0, cell = Cell(0, 0)), grid.findAppLocation("A"))
    }

    @Test
    fun findAppLocation_returns_quick_bar_slot_for_bar_app() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        assertEquals(AppLocation.QuickBarSlot(index = 0), grid.findAppLocation("D"))
    }

    @Test
    fun findAppLocation_returns_null_for_unknown_app() {
        val grid = singlePageGrid()
        assertEquals(null, grid.findAppLocation("Z"))
    }

    // ---------------------------------------------------------------------------
    // moveGridToGrid — STORY-002-2 AC
    // ---------------------------------------------------------------------------

    @Test
    fun moveGridToGrid_moves_app_to_empty_cell_and_clears_source() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.GridCell(0, Cell(1, 1))

        val result = grid.moveGridToGrid(origin, destination)

        assertEquals(null, result.grid[0][Cell(0, 0)])
        assertEquals(appA, result.grid[0][Cell(1, 1)])
    }

    @Test
    fun moveGridToGrid_is_no_op_when_destination_is_occupied() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.GridCell(0, Cell(1, 0)) // occupied by B

        val result = grid.moveGridToGrid(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveGridToGrid_is_no_op_when_source_and_destination_are_same_cell() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.GridCell(0, Cell(0, 0))

        val result = grid.moveGridToGrid(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveGridToGrid_moves_app_across_pages() {
        val page1 = mapOf(Cell(0, 0) to appA, Cell(1, 0) to null, Cell(0, 1) to null, Cell(1, 1) to null)
        val page2 = mapOf(Cell(0, 0) to null, Cell(1, 0) to null, Cell(0, 1) to null, Cell(1, 1) to null)
        val grid = Grid(spec = spec, grid = listOf(page1, page2), bar = emptyList())

        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.GridCell(1, Cell(1, 1))
        val result = grid.moveGridToGrid(origin, destination)

        assertEquals(null, result.grid[0][Cell(0, 0)])
        assertEquals(appA, result.grid[1][Cell(1, 1)])
    }

    // ---------------------------------------------------------------------------
    // moveBarToGrid — STORY-002-3 AC
    // ---------------------------------------------------------------------------

    @Test
    fun moveBarToGrid_moves_app_from_bar_to_empty_grid_cell() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        val origin = AppLocation.QuickBarSlot(0)
        val destination = DragDestination.GridCell(0, Cell(1, 1))

        val result = grid.moveBarToGrid(origin, destination)

        assertEquals(appD, result.grid[0][Cell(1, 1)])
        assertEquals(emptyList<App>(), result.bar)
    }

    @Test
    fun moveBarToGrid_is_no_op_when_destination_is_occupied() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        val origin = AppLocation.QuickBarSlot(0)
        val destination = DragDestination.GridCell(0, Cell(0, 0)) // occupied by A

        val result = grid.moveBarToGrid(origin, destination)

        assertSame(grid, result)
    }

    // ---------------------------------------------------------------------------
    // moveGridToBar — STORY-002-3 AC
    // ---------------------------------------------------------------------------

    @Test
    fun moveGridToBar_moves_app_from_grid_to_empty_bar_slot() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.QuickBarSlot(0)

        val result = grid.moveGridToBar(origin, destination)

        assertEquals(null, result.grid[0][Cell(0, 0)])
        assertEquals(listOf(appA), result.bar)
    }

    @Test
    fun moveGridToBar_displaces_bar_app_to_source_grid_cell() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        val origin = AppLocation.GridCell(0, Cell(1, 1)) // empty cell; we move a real app from grid
        // Let's use appC at (0,1) to make it concrete
        val origin2 = AppLocation.GridCell(0, Cell(0, 1))
        val destination = DragDestination.QuickBarSlot(0) // displaces D

        val result = grid.moveGridToBar(origin2, destination)

        assertEquals(appC, result.bar[0])
        assertEquals(appD, result.grid[0][Cell(0, 1)]) // D displaced to source cell
    }

    // ---------------------------------------------------------------------------
    // moveBarToBar — STORY-002-3 AC
    // ---------------------------------------------------------------------------

    @Test
    fun moveBarToBar_shifts_neighbours_without_swapping() {
        val grid = singlePageGrid().copy(bar = listOf(appA, appB, appC))
        val origin = AppLocation.QuickBarSlot(0)
        val destination = DragDestination.QuickBarSlot(2)

        val result = grid.moveBarToBar(origin, destination)

        assertEquals(listOf(appB, appC, appA), result.bar)
    }

    @Test
    fun moveBarToBar_is_no_op_when_same_index() {
        val grid = singlePageGrid().copy(bar = listOf(appA, appB))
        val origin = AppLocation.QuickBarSlot(1)
        val destination = DragDestination.QuickBarSlot(1)

        val result = grid.moveBarToBar(origin, destination)

        assertSame(grid, result)
    }

    // ---------------------------------------------------------------------------
    // applyDragMove routing
    // ---------------------------------------------------------------------------

    @Test
    fun applyDragMove_routes_grid_to_grid_correctly() {
        val grid = singlePageGrid()
        val result = grid.applyDragMove("A", DragDestination.GridCell(0, Cell(1, 1)))
        assertEquals(appA, result.grid[0][Cell(1, 1)])
    }

    @Test
    fun applyDragMove_routes_bar_to_grid_correctly() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        val result = grid.applyDragMove("D", DragDestination.GridCell(0, Cell(1, 1)))
        assertEquals(appD, result.grid[0][Cell(1, 1)])
    }

    @Test
    fun applyDragMove_routes_grid_to_bar_correctly() {
        val grid = singlePageGrid()
        val result = grid.applyDragMove("A", DragDestination.QuickBarSlot(0))
        assertEquals(listOf(appA), result.bar)
    }

    @Test
    fun applyDragMove_routes_bar_to_bar_correctly() {
        val appE = makeApp("E")
        val appF = makeApp("F")
        val grid = singlePageGrid().copy(bar = listOf(appE, appF))
        val result = grid.applyDragMove("E", DragDestination.QuickBarSlot(1))
        assertEquals(listOf(appF, appE), result.bar)
    }

    @Test
    fun applyDragMove_is_no_op_for_unknown_app_id() {
        val grid = singlePageGrid()
        val result = grid.applyDragMove("UNKNOWN", DragDestination.GridCell(0, Cell(1, 1)))
        assertSame(grid, result)
    }

    // ---------------------------------------------------------------------------
    // moveGridToGrid — boundary branches
    // ---------------------------------------------------------------------------

    @Test
    fun moveGridToGrid_is_no_op_when_source_page_index_out_of_bounds() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(99, Cell(0, 0))
        val destination = DragDestination.GridCell(0, Cell(1, 1))

        val result = grid.moveGridToGrid(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveGridToGrid_is_no_op_when_destination_page_index_out_of_bounds() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.GridCell(99, Cell(1, 1))

        val result = grid.moveGridToGrid(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveGridToGrid_is_no_op_when_source_cell_is_empty() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(1, 1)) // null cell
        val destination = DragDestination.GridCell(0, Cell(0, 0))

        val result = grid.moveGridToGrid(origin, destination)

        assertSame(grid, result)
    }

    // ---------------------------------------------------------------------------
    // moveBarToGrid — boundary branches
    // ---------------------------------------------------------------------------

    @Test
    fun moveBarToGrid_is_no_op_when_bar_index_out_of_bounds() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        val origin = AppLocation.QuickBarSlot(99)
        val destination = DragDestination.GridCell(0, Cell(1, 1))

        val result = grid.moveBarToGrid(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveBarToGrid_is_no_op_when_destination_page_out_of_bounds() {
        val grid = singlePageGrid().copy(bar = listOf(appD))
        val origin = AppLocation.QuickBarSlot(0)
        val destination = DragDestination.GridCell(99, Cell(1, 1))

        val result = grid.moveBarToGrid(origin, destination)

        assertSame(grid, result)
    }

    // ---------------------------------------------------------------------------
    // moveGridToBar — boundary branches
    // ---------------------------------------------------------------------------

    @Test
    fun moveGridToBar_is_no_op_when_source_page_out_of_bounds() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(99, Cell(0, 0))
        val destination = DragDestination.QuickBarSlot(0)

        val result = grid.moveGridToBar(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveGridToBar_is_no_op_when_source_cell_is_empty() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(1, 1)) // null cell
        val destination = DragDestination.QuickBarSlot(0)

        val result = grid.moveGridToBar(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveGridToBar_appends_to_end_when_bar_slot_exceeds_size() {
        val grid = singlePageGrid()
        val origin = AppLocation.GridCell(0, Cell(0, 0))
        val destination = DragDestination.QuickBarSlot(99)

        val result = grid.moveGridToBar(origin, destination)

        assertEquals(listOf(appA), result.bar)
        assertEquals(null, result.grid[0][Cell(0, 0)])
    }

    // ---------------------------------------------------------------------------
    // moveBarToBar — boundary branches
    // ---------------------------------------------------------------------------

    @Test
    fun moveBarToBar_is_no_op_when_origin_index_out_of_bounds() {
        val grid = singlePageGrid().copy(bar = listOf(appA, appB))
        val origin = AppLocation.QuickBarSlot(99)
        val destination = DragDestination.QuickBarSlot(0)

        val result = grid.moveBarToBar(origin, destination)

        assertSame(grid, result)
    }

    @Test
    fun moveBarToBar_coerces_destination_to_valid_range() {
        val appE = makeApp("E")
        val appF = makeApp("F")
        val grid = singlePageGrid().copy(bar = listOf(appE, appF))
        val origin = AppLocation.QuickBarSlot(0)
        val destination = DragDestination.QuickBarSlot(99) // coerced to end

        val result = grid.moveBarToBar(origin, destination)

        assertEquals(listOf(appF, appE), result.bar)
    }
}
