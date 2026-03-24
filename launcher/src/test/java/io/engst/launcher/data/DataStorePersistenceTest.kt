package io.engst.launcher.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import io.engst.launcher.data.proto.GridDataProto
import io.engst.launcher.data.store.GridDataProtoSerializer
import io.engst.launcher.data.store.toGridData
import io.engst.launcher.data.store.toProto
import io.engst.launcher.model.Cell
import io.engst.launcher.model.GridSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStorePersistenceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<GridDataProto>

    @Before
    fun setUp() {
        dataStoreFile = tempFolder.newFile("test_grid_data.pb")
        dataStore = DataStoreFactory.create(
            serializer = GridDataProtoSerializer,
            produceFile = { dataStoreFile },
        )
    }

    @After
    fun tearDown() {
        dataStoreFile.delete()
    }

    // ---------------------------------------------------------------------------
    // STORY-007-1: DataStore wiring — default read
    // ---------------------------------------------------------------------------

    @Test
    fun fresh_datastore_returns_default_grid_data() = runTest {
        val proto = dataStore.data.first()
        val data = proto.toGridData()

        assertEquals(4, data.cols)
        assertEquals(4, data.rows)
        assertNull(data.grid)
        assertNull(data.bar)
        assertFalse(data.hasUserGrid)
        assertFalse(data.hasUserBar)
    }

    // ---------------------------------------------------------------------------
    // STORY-007-2: Cold-start reads persisted data
    // ---------------------------------------------------------------------------

    @Test
    fun cold_start_reads_persisted_grid_layout() = runTest {
        val gridData = GridData(
            cols = 3,
            rows = 5,
            grid = listOf(
                mapOf(Cell(0, 0) to "pkg.A/cls.A", Cell(1, 0) to null),
            ),
            bar = listOf("pkg.X/cls.X"),
            hasUserGrid = true,
            hasUserBar = true,
            barSlots = 5,
        )
        dataStore.updateData { gridData.toProto() }

        // Simulate cold-start by reading the persisted bytes directly
        val restored = GridDataProto.parseFrom(dataStoreFile.inputStream()).toGridData()

        assertEquals(3, restored.cols)
        assertEquals(5, restored.rows)
        assertTrue(restored.hasUserGrid)
        assertTrue(restored.hasUserBar)
        assertEquals(listOf("pkg.X/cls.X"), restored.bar)
        assertEquals("pkg.A/cls.A", restored.grid?.get(0)?.get(Cell(0, 0)))
        assertNull(restored.grid?.get(0)?.get(Cell(1, 0)))
    }

    // ---------------------------------------------------------------------------
    // STORY-007-2: has_user_grid = false → auto-populate
    // ---------------------------------------------------------------------------

    @Test
    fun has_user_grid_false_returns_null_grid_for_auto_populate() = runTest {
        val gridData = GridData(
            cols = 4,
            rows = 4,
            grid = null,
            bar = null,
            hasUserGrid = false,
            hasUserBar = false,
        )
        dataStore.updateData { gridData.toProto() }

        val restored = dataStore.data.first().toGridData()

        assertNull(restored.grid)
        assertNull(restored.bar)
    }

    // ---------------------------------------------------------------------------
    // STORY-007-3: Write path — mutations persist
    // ---------------------------------------------------------------------------

    @Test
    fun update_sets_has_user_grid_and_has_user_bar_to_true() = runTest {
        val gridData = GridData(
            cols = 4,
            rows = 4,
            grid = listOf(
                mapOf(Cell(0, 0) to "pkg.A/cls.A", Cell(1, 0) to null),
            ),
            bar = listOf("pkg.B/cls.B"),
            hasUserGrid = true,
            hasUserBar = true,
        )
        dataStore.updateData { gridData.toProto() }

        val restored = dataStore.data.first().toGridData()

        assertTrue(restored.hasUserGrid)
        assertTrue(restored.hasUserBar)
    }

    // ---------------------------------------------------------------------------
    // STORY-007-3: resetDefaults sets sentinels to false
    // ---------------------------------------------------------------------------

    @Test
    fun reset_defaults_sets_sentinel_flags_to_false() = runTest {
        val customised = GridData(
            cols = 3,
            rows = 3,
            grid = listOf(mapOf(Cell(0, 0) to "pkg.A/cls.A")),
            bar = listOf("pkg.B/cls.B"),
            hasUserGrid = true,
            hasUserBar = true,
        )
        dataStore.updateData { customised.toProto() }

        val reset = defaultGridData.copy(cols = 4, rows = 4)
        dataStore.updateData { reset.toProto() }

        val restored = dataStore.data.first().toGridData()

        assertFalse(restored.hasUserGrid)
        assertFalse(restored.hasUserBar)
        assertNull(restored.grid)
        assertNull(restored.bar)
        assertEquals(4, restored.cols)
        assertEquals(4, restored.rows)
    }

    // ---------------------------------------------------------------------------
    // STORY-007-4: App install — newly placed app persists
    // ---------------------------------------------------------------------------

    @Test
    fun newly_appended_app_survives_datastore_round_trip() = runTest {
        val gridData = GridData(
            cols = 2,
            rows = 2,
            grid = listOf(
                mapOf(
                    Cell(0, 0) to "pkg.A/cls.A",
                    Cell(1, 0) to null,
                    Cell(0, 1) to null,
                    Cell(1, 1) to null,
                ),
            ),
            bar = emptyList(),
            hasUserGrid = true,
            hasUserBar = true,
        )
        dataStore.updateData { gridData.toProto() }

        val withNewApp = gridData.copy(
            grid = listOf(
                mapOf(
                    Cell(0, 0) to "pkg.A/cls.A",
                    Cell(1, 0) to "pkg.NewApp/cls.NewApp",
                    Cell(0, 1) to null,
                    Cell(1, 1) to null,
                ),
            ),
        )
        dataStore.updateData { withNewApp.toProto() }

        val restored = dataStore.data.first().toGridData()
        assertEquals("pkg.NewApp/cls.NewApp", restored.grid?.get(0)?.get(Cell(1, 0)))
    }

    // ---------------------------------------------------------------------------
    // STORY-007-4: App uninstall — vacant cell persists
    // ---------------------------------------------------------------------------

    @Test
    fun uninstalled_app_leaves_vacant_cell_after_round_trip() = runTest {
        val gridData = GridData(
            cols = 2,
            rows = 1,
            grid = listOf(
                mapOf(Cell(0, 0) to "pkg.A/cls.A", Cell(1, 0) to "pkg.B/cls.B"),
            ),
            bar = emptyList(),
            hasUserGrid = true,
            hasUserBar = true,
        )
        dataStore.updateData { gridData.toProto() }

        val afterUninstall = gridData.copy(
            grid = listOf(
                mapOf(Cell(0, 0) to "pkg.A/cls.A", Cell(1, 0) to null),
            ),
        )
        dataStore.updateData { afterUninstall.toProto() }

        val restored = dataStore.data.first().toGridData()
        assertEquals("pkg.A/cls.A", restored.grid?.get(0)?.get(Cell(0, 0)))
        assertNull(restored.grid?.get(0)?.get(Cell(1, 0)))
    }

    // ---------------------------------------------------------------------------
    // STORY-007-4: has_user_grid remains true after app install/uninstall
    // ---------------------------------------------------------------------------

    @Test
    fun has_user_grid_stays_true_after_install_event() = runTest {
        val gridData = GridData(
            cols = 2,
            rows = 2,
            grid = listOf(mapOf(Cell(0, 0) to "pkg.A/cls.A")),
            bar = emptyList(),
            hasUserGrid = true,
            hasUserBar = true,
        )
        dataStore.updateData { gridData.toProto() }

        val afterInstall = gridData.copy(
            grid = listOf(mapOf(Cell(0, 0) to "pkg.A/cls.A", Cell(1, 0) to "pkg.New/cls.New")),
        )
        dataStore.updateData { afterInstall.toProto() }

        val restored = dataStore.data.first().toGridData()
        assertTrue(restored.hasUserGrid)
    }
}
