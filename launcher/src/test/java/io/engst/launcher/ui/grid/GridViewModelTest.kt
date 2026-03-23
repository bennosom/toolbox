package io.engst.launcher.ui.grid

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.grid.drag.DragPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests covering the ViewModel state machine for STORY-002-1 through STORY-002-4.
 * Focuses on drag phase transitions and menu state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeAppsRepository
    private lateinit var viewModel: GridViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppsRepository()
        viewModel = GridViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // Helpers
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

    private fun emptyPage() = mapOf(
        Cell(0, 0) to null, Cell(1, 0) to null,
        Cell(0, 1) to null, Cell(1, 1) to null,
    )

    private fun gridWith(vararg pairs: Pair<Cell, App?>): Grid {
        val page = mapOf(*pairs) + (emptyPage() - pairs.map { it.first }.toSet())
        return Grid(spec = spec, grid = listOf(page), bar = emptyList())
    }

    // ---------------------------------------------------------------------------
    // STORY-002-1: drag phase starts on DragStarted intent
    // ---------------------------------------------------------------------------

    @Test
    fun drag_phase_transitions_to_active_on_DragStarted() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        val phase = viewModel.uiState.value.dragPhase
        assertTrue(phase is DragPhase.Active)
        assertEquals("A", (phase as DragPhase.Active).draggingAppId)
    }

    @Test
    fun draggingAppId_is_set_immediately_on_DragStarted() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("A", viewModel.uiState.value.draggingAppId)
    }

    @Test
    fun DragStarted_ignored_when_no_persisted_grid_available() = runTest {
        // Do not emit any grid
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-1: cancel path discards working grid
    // ---------------------------------------------------------------------------

    @Test
    fun DragCancelled_resets_drag_phase_to_idle_without_committing() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToGridCell(0, Cell(1, 1)))
        viewModel.onIntent(GridIntent.DragCancelled)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
        assertEquals(0, fakeRepository.updateCallCount)
    }

    @Test
    fun DragCancelled_is_no_op_when_already_idle() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragCancelled)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-2: intra-page grid move state machine
    // ---------------------------------------------------------------------------

    @Test
    fun DragMovedToGridCell_updates_working_grid_for_empty_target_cell() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToGridCell(0, Cell(1, 1)))
        testDispatcher.scheduler.advanceUntilIdle()

        val phase = viewModel.uiState.value.dragPhase as? DragPhase.Active
        assertEquals(appA, phase?.workingGrid?.grid?.get(0)?.get(Cell(1, 1)))
    }

    @Test
    fun DragMovedToGridCell_is_no_op_for_occupied_target_cell() = runTest {
        val grid = gridWith(Cell(0, 0) to appA, Cell(1, 0) to appB)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        val phaseBeforeMove = viewModel.uiState.value.dragPhase
        viewModel.onIntent(GridIntent.DragMovedToGridCell(0, Cell(1, 0))) // occupied by B
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(phaseBeforeMove, viewModel.uiState.value.dragPhase)
    }

    @Test
    fun DragDropped_commits_working_grid_and_resets_to_idle() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToGridCell(0, Cell(1, 1)))
        viewModel.onIntent(GridIntent.DragDropped)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
        assertEquals(1, fakeRepository.updateCallCount)
    }

    @Test
    fun DragDropped_when_already_idle_is_a_no_op() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragDropped)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.updateCallCount)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-2: gesture state machine — menu state
    // ---------------------------------------------------------------------------

    @Test
    fun AppMenuRequested_sets_active_app_menu_identifier() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("A", viewModel.uiState.value.activeAppMenuIdentifier)
    }

    @Test
    fun AppMenuDismissed_clears_active_app_menu_identifier() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        viewModel.onIntent(GridIntent.AppMenuDismissed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeAppMenuIdentifier)
    }

    @Test
    fun DragStarted_clears_app_menu_and_grid_menu() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeAppMenuIdentifier)
        assertEquals(false, viewModel.uiState.value.isGridMenuVisible)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-4: display grid includes trailing page in drag mode
    // ---------------------------------------------------------------------------

    @Test
    fun displayGrid_appends_empty_trailing_page_while_drag_is_active() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        val displayGrid = viewModel.uiState.value.displayGrid
        assertEquals(2, displayGrid?.grid?.size)
        assertTrue(displayGrid?.grid?.last()?.values?.all { it == null } == true)
    }

    @Test
    fun displayGrid_does_not_append_trailing_page_when_idle() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.displayGrid?.grid?.size)
    }

    // ---------------------------------------------------------------------------
    // Fake repository
    // ---------------------------------------------------------------------------

    private class FakeAppsRepository : AppsRepository {
        private val gridFlow = MutableStateFlow<Grid>(Grid(GridSpec(2, 2), emptyList(), emptyList()))
        var updateCallCount = 0

        override val installedApps: Flow<List<App>> = MutableStateFlow(emptyList())
        override val grid: Flow<Grid> = gridFlow

        fun emitGrid(grid: Grid) { gridFlow.value = grid }

        override fun setGridSpec(spec: GridSpec) {}
        override fun update(grid: Grid) { updateCallCount++ }
        override fun resetDefaults(spec: GridSpec) {}
    }
}
