package io.engst.launcher.ui.grid

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.grid.drag.DragPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests covering the ViewModel state machine for STORY-002-1 through STORY-002-4.
 * Focuses on drag phase transitions and menu state management.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeAppsRepository
    private lateinit var fakeEffectHandler: FakeGridEffectHandler
    private lateinit var viewModel: GridViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppsRepository()
        fakeEffectHandler = FakeGridEffectHandler()
        viewModel = GridViewModel(fakeRepository, fakeEffectHandler)
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
    // STORY-002-3: DragMovedToQuickBarSlot — bar drag via ViewModel
    // ---------------------------------------------------------------------------

    @Test
    fun DragMovedToQuickBarSlot_updates_working_grid() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToQuickBarSlot(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val phase = viewModel.uiState.value.dragPhase as? DragPhase.Active
        assertEquals(listOf(appA), phase?.workingGrid?.bar)
    }

    @Test
    fun DragMovedToQuickBarSlot_is_no_op_when_idle() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragMovedToQuickBarSlot(0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
    }

    @Test
    fun DragMovedToGridCell_is_no_op_when_idle() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragMovedToGridCell(0, Cell(0, 0)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-2: GridMenuRequested — guard and normal branches
    // ---------------------------------------------------------------------------

    @Test
    fun GridMenuRequested_is_suppressed_when_app_menu_is_active() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        viewModel.onIntent(GridIntent.GridMenuRequested(DpOffset(10.dp, 20.dp)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isGridMenuVisible)
        assertEquals("A", viewModel.uiState.value.activeAppMenuIdentifier)
    }

    @Test
    fun GridMenuRequested_shows_grid_menu_when_no_app_menu_active() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val offset = DpOffset(10.dp, 20.dp)
        viewModel.onIntent(GridIntent.GridMenuRequested(offset))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isGridMenuVisible)
        assertEquals(offset, viewModel.uiState.value.gridMenuOffset)
    }

    @Test
    fun GridMenuDismissed_clears_grid_menu_visibility() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.GridMenuRequested(DpOffset(0.dp, 0.dp)))
        viewModel.onIntent(GridIntent.GridMenuDismissed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isGridMenuVisible)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-2: GridSpecChangeRequested delegates to repository
    // ---------------------------------------------------------------------------

    @Test
    fun GridSpecChangeRequested_delegates_to_repository() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val newSpec = GridSpec(5, 5)
        viewModel.onIntent(GridIntent.GridSpecChangeRequested(newSpec))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(newSpec, fakeRepository.lastSetSpec)
    }

    // ---------------------------------------------------------------------------
    // AppTapped clears menu and emits LaunchApp effect
    // ---------------------------------------------------------------------------

    @Test
    fun AppTapped_clears_app_menu_and_emits_LaunchApp() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        viewModel.onIntent(GridIntent.AppTapped(appA))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeAppMenuIdentifier)
    }

    // ---------------------------------------------------------------------------
    // AppMenuRequested hides grid menu
    // ---------------------------------------------------------------------------

    @Test
    fun AppMenuRequested_hides_grid_menu() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.GridMenuRequested(DpOffset(0.dp, 0.dp)))
        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isGridMenuVisible)
        assertEquals("A", viewModel.uiState.value.activeAppMenuIdentifier)
    }

    // ---------------------------------------------------------------------------
    // DefaultLauncherSettingsOpenRequested hides grid menu
    // ---------------------------------------------------------------------------

    @Test
    fun DefaultLauncherSettingsOpenRequested_hides_grid_menu() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.GridMenuRequested(DpOffset(0.dp, 0.dp)))
        viewModel.onIntent(GridIntent.DefaultLauncherSettingsOpenRequested)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isGridMenuVisible)
    }

    @Test
    fun WallpaperSettingsOpenRequested_hides_grid_menu() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.GridMenuRequested(DpOffset(0.dp, 0.dp)))
        viewModel.onIntent(GridIntent.WallpaperSettingsOpenRequested)
        testDispatcher.scheduler.advanceUntilIdle()

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

    @Test
    fun displayGrid_is_null_when_no_grid_emitted() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.displayGrid)
    }

    // ---------------------------------------------------------------------------
    // STORY-002-4: cancelling drag over trailing page discards it
    // ---------------------------------------------------------------------------

    @Test
    fun DragCancelled_discards_trailing_page() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.displayGrid?.grid?.size)

        viewModel.onIntent(GridIntent.DragCancelled)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.displayGrid?.grid?.size)
    }

    // ---------------------------------------------------------------------------
    // Repository grid update during active drag updates working grid
    // ---------------------------------------------------------------------------

    @Test
    fun repository_grid_update_during_active_drag_updates_working_grid() = runTest {
        val grid1 = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        val grid2 = gridWith(Cell(0, 0) to appA, Cell(1, 0) to appB)
        fakeRepository.emitGrid(grid2)
        testDispatcher.scheduler.advanceUntilIdle()

        val phase = viewModel.uiState.value.dragPhase as DragPhase.Active
        assertEquals(grid2, phase.workingGrid)
    }

    // ---------------------------------------------------------------------------
    // effectiveGrid returns persisted grid when idle, working grid when active
    // ---------------------------------------------------------------------------

    @Test
    fun effectiveGrid_returns_persisted_grid_when_idle() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(grid, viewModel.uiState.value.effectiveGrid)
    }

    @Test
    fun effectiveGrid_returns_working_grid_when_drag_active() = runTest {
        val grid = gridWith(Cell(0, 0) to appA)
        fakeRepository.emitGrid(grid)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToGridCell(0, Cell(1, 1)))
        testDispatcher.scheduler.advanceUntilIdle()

        val effective = viewModel.uiState.value.effectiveGrid
        assertEquals(appA, effective?.grid?.get(0)?.get(Cell(1, 1)))
    }

    // ---------------------------------------------------------------------------
    // Fake repository
    // ---------------------------------------------------------------------------

    private class FakeGridEffectHandler : GridEffectHandler {
        val effects = mutableListOf<String>()

        override fun launchApp(app: App) { effects += "LaunchApp(${app.id})" }
        override fun openAppDetails(app: App) { effects += "OpenAppDetails(${app.id})" }
        override fun removeApp(app: App) { effects += "RemoveApp(${app.id})" }
        override fun launchShortcut(shortcut: android.content.pm.ShortcutInfo) { effects += "LaunchShortcut" }
        override fun openDefaultLauncherSettings() { effects += "OpenDefaultLauncherSettings" }
        override fun openWallpaperSettings() { effects += "OpenWallpaperSettings" }
        override fun showResetDefaultsSnackbar() { effects += "ShowResetDefaultsSnackbar" }
        override fun expandNotificationsPanel() { effects += "ExpandNotificationsPanel" }
        override fun openSearch() { effects += "OpenSearch" }
    }

    private class FakeAppsRepository : AppsRepository {
        private val gridFlow = MutableSharedFlow<Grid>(replay = 1)
        var updateCallCount = 0
        var lastSetSpec: GridSpec? = null

        override val installedApps: Flow<List<App>> = MutableStateFlow(emptyList())
        override val grid: Flow<Grid> = gridFlow
        override val darkModePreference: Flow<io.engst.launcher.ui.shared.DarkModePreference> =
            MutableStateFlow(io.engst.launcher.ui.shared.DarkModePreference.SYSTEM)
        override val isBarVisible: Flow<Boolean> = MutableStateFlow(true)

        fun emitGrid(grid: Grid) { gridFlow.tryEmit(grid) }

        override fun setGridSpec(spec: GridSpec) { lastSetSpec = spec }
        override fun update(grid: Grid) { updateCallCount++ }
        override fun resetDefaults(spec: GridSpec) {}
        override fun setDarkModePreference(preference: io.engst.launcher.ui.shared.DarkModePreference) {}
        override fun setBarVisible(visible: Boolean) {}
    }
}
