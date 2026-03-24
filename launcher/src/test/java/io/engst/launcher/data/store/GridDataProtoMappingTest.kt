package io.engst.launcher.data.store

import io.engst.launcher.data.GridData
import io.engst.launcher.data.proto.GridCellProto
import io.engst.launcher.data.proto.GridDataProto
import io.engst.launcher.data.proto.GridPageProto
import io.engst.launcher.model.Cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridDataProtoMappingTest {

    // ---------------------------------------------------------------------------
    // toGridData — default instance
    // ---------------------------------------------------------------------------

    @Test
    fun default_proto_maps_to_default_grid_data() {
        val proto = GridDataProto.getDefaultInstance()
        val data = proto.toGridData()

        assertEquals(4, data.cols)
        assertEquals(4, data.rows)
        assertNull(data.grid)
        assertNull(data.bar)
        assertFalse(data.hasUserGrid)
        assertFalse(data.hasUserBar)
        assertEquals(0, data.barSlots)
    }

    // ---------------------------------------------------------------------------
    // toGridData — with user grid
    // ---------------------------------------------------------------------------

    @Test
    fun proto_with_user_grid_maps_grid_pages_correctly() {
        val proto = GridDataProto.newBuilder()
            .setCols(2)
            .setRows(2)
            .setHasUserGrid(true)
            .addGrid(
                GridPageProto.newBuilder()
                    .addCells(cell(0, 0, "pkg.A/cls.A"))
                    .addCells(cell(1, 0, ""))
                    .addCells(cell(0, 1, "pkg.B/cls.B"))
                    .addCells(cell(1, 1, ""))
            )
            .build()

        val data = proto.toGridData()

        assertEquals(2, data.cols)
        assertEquals(2, data.rows)
        assertTrue(data.hasUserGrid)
        assertEquals(1, data.grid?.size)
        assertEquals("pkg.A/cls.A", data.grid?.get(0)?.get(Cell(0, 0)))
        assertNull(data.grid?.get(0)?.get(Cell(1, 0)))
        assertEquals("pkg.B/cls.B", data.grid?.get(0)?.get(Cell(0, 1)))
    }

    // ---------------------------------------------------------------------------
    // toGridData — without user grid (sentinel false)
    // ---------------------------------------------------------------------------

    @Test
    fun proto_without_user_grid_returns_null_grid() {
        val proto = GridDataProto.newBuilder()
            .setCols(3)
            .setRows(3)
            .setHasUserGrid(false)
            .addGrid(GridPageProto.getDefaultInstance())
            .build()

        val data = proto.toGridData()

        assertNull(data.grid)
    }

    // ---------------------------------------------------------------------------
    // toGridData — with user bar
    // ---------------------------------------------------------------------------

    @Test
    fun proto_with_user_bar_maps_bar_list_correctly() {
        val proto = GridDataProto.newBuilder()
            .setCols(4)
            .setRows(4)
            .setHasUserBar(true)
            .addBar("pkg.Phone/cls.Phone")
            .addBar("pkg.Browser/cls.Browser")
            .build()

        val data = proto.toGridData()

        assertTrue(data.hasUserBar)
        assertEquals(listOf("pkg.Phone/cls.Phone", "pkg.Browser/cls.Browser"), data.bar)
    }

    // ---------------------------------------------------------------------------
    // toGridData — without user bar (sentinel false)
    // ---------------------------------------------------------------------------

    @Test
    fun proto_without_user_bar_returns_null_bar() {
        val proto = GridDataProto.newBuilder()
            .setCols(4)
            .setRows(4)
            .setHasUserBar(false)
            .addBar("pkg.Phone/cls.Phone")
            .build()

        val data = proto.toGridData()

        assertNull(data.bar)
    }

    // ---------------------------------------------------------------------------
    // toGridData — barSlots
    // ---------------------------------------------------------------------------

    @Test
    fun proto_bar_slots_maps_correctly() {
        val proto = GridDataProto.newBuilder()
            .setCols(4)
            .setRows(4)
            .setBarSlots(6)
            .build()

        val data = proto.toGridData()

        assertEquals(6, data.barSlots)
    }

    // ---------------------------------------------------------------------------
    // Round-trip: toProto → toGridData
    // ---------------------------------------------------------------------------

    @Test
    fun round_trip_preserves_grid_data_with_user_layout() {
        val original = GridData(
            cols = 3,
            rows = 5,
            grid = listOf(
                mapOf(
                    Cell(0, 0) to "pkg.A/cls.A",
                    Cell(1, 0) to null,
                    Cell(2, 0) to "pkg.C/cls.C",
                ),
            ),
            bar = listOf("pkg.X/cls.X"),
            hasUserGrid = true,
            hasUserBar = true,
            barSlots = 5,
        )

        val roundTripped = original.toProto().toGridData()

        assertEquals(original.cols, roundTripped.cols)
        assertEquals(original.rows, roundTripped.rows)
        assertEquals(original.hasUserGrid, roundTripped.hasUserGrid)
        assertEquals(original.hasUserBar, roundTripped.hasUserBar)
        assertEquals(original.barSlots, roundTripped.barSlots)
        assertEquals(original.bar, roundTripped.bar)
        assertEquals(original.grid?.size, roundTripped.grid?.size)
        assertEquals(original.grid?.get(0), roundTripped.grid?.get(0))
    }

    @Test
    fun round_trip_preserves_default_state() {
        val original = GridData(
            cols = 4,
            rows = 4,
            grid = null,
            bar = null,
            hasUserGrid = false,
            hasUserBar = false,
            barSlots = 0,
        )

        val roundTripped = original.toProto().toGridData()

        assertEquals(original.cols, roundTripped.cols)
        assertEquals(original.rows, roundTripped.rows)
        assertNull(roundTripped.grid)
        assertNull(roundTripped.bar)
        assertFalse(roundTripped.hasUserGrid)
        assertFalse(roundTripped.hasUserBar)
    }

    // ---------------------------------------------------------------------------
    // toProto — grid cells serialised correctly
    // ---------------------------------------------------------------------------

    @Test
    fun toProto_empty_cells_serialised_with_empty_app_id() {
        val data = GridData(
            cols = 2,
            rows = 1,
            grid = listOf(mapOf(Cell(0, 0) to "pkg.A/cls.A", Cell(1, 0) to null)),
            bar = null,
            hasUserGrid = true,
            hasUserBar = false,
        )

        val proto = data.toProto()
        val cells = proto.getGrid(0).cellsList

        assertEquals(2, cells.size)
        assertEquals("pkg.A/cls.A", cells[0].appId)
        assertEquals("", cells[1].appId)
    }

    // ---------------------------------------------------------------------------
    // toProto — bar serialised correctly
    // ---------------------------------------------------------------------------

    @Test
    fun toProto_bar_entries_written() {
        val data = GridData(
            cols = 4,
            rows = 4,
            grid = null,
            bar = listOf("pkg.P/cls.P", "pkg.Q/cls.Q"),
            hasUserGrid = false,
            hasUserBar = true,
        )

        val proto = data.toProto()

        assertEquals(listOf("pkg.P/cls.P", "pkg.Q/cls.Q"), proto.barList)
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun cell(col: Int, row: Int, appId: String): GridCellProto =
        GridCellProto.newBuilder()
            .setCol(col)
            .setRow(row)
            .setAppId(appId)
            .build()
}
