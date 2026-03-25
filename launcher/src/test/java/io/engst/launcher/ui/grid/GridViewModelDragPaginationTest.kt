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
import io.engst.launcher.ui.shared.DarkModePreference
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModelDragPaginationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeAppsRepository
    private lateinit var fakeEffectHandler: FakeGridEffectHandler
    private lateinit var viewModel: GridViewModel

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

    @Test
    fun drag_pointer_moved_in_pager_sets_pending_navigation_target() = runTest {
        fakeRepository.emitGrid(baseGrid())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(
            GridIntent.DragPointerMovedInPager(
                pointerXInPager = 990f,
                pagerWidth = 1000f,
                currentPage = 0,
                pageCount = 2,
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.pendingPageNavigationTarget)
    }

    @Test
    fun page_navigation_handled_clears_pending_navigation_target() = runTest {
        fakeRepository.emitGrid(baseGrid())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(
            GridIntent.DragPointerMovedInPager(
                pointerXInPager = 10f,
                pagerWidth = 1000f,
                currentPage = 1,
                pageCount = 2,
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.pendingPageNavigationTarget)

        viewModel.onIntent(GridIntent.PageNavigationHandled)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.pendingPageNavigationTarget)
    }

    @Test
    fun drag_dropped_commits_move_into_placeholder_page() = runTest {
        fakeRepository.emitGrid(baseGrid())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToGridCell(pageIndex = 1, cell = Cell(0, 0)))
        viewModel.onIntent(GridIntent.DragDropped)
        testDispatcher.scheduler.advanceUntilIdle()

        val persisted = fakeRepository.lastUpdatedGrid
        assertEquals(1, fakeRepository.updateCallCount)
        assertEquals(2, persisted?.grid?.size)
        assertEquals(appA, persisted?.grid?.get(1)?.get(Cell(0, 0)))
        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
    }

    @Test
    fun drag_ended_with_external_acceptance_clears_drag_without_commit() = runTest {
        fakeRepository.emitGrid(baseGrid())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(GridIntent.DragStarted("A"))
        viewModel.onIntent(GridIntent.DragMovedToGridCell(pageIndex = 0, cell = Cell(0, 1)))
        viewModel.onIntent(GridIntent.DragEnded(accepted = true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dragPhase is DragPhase.Idle)
        assertEquals(0, fakeRepository.updateCallCount)
    }

    private fun baseGrid(): Grid {
        val page = mapOf(
            Cell(0, 0) to appA,
            Cell(1, 0) to appB,
            Cell(0, 1) to null,
            Cell(1, 1) to null,
        )
        return Grid(spec = spec, grid = listOf(page), bar = emptyList())
    }

    private class FakeGridEffectHandler : GridEffectHandler {
        override fun launchApp(app: App) {}
        override fun openAppDetails(app: App) {}
        override fun removeApp(app: App) {}
        override fun launchShortcut(shortcut: android.content.pm.ShortcutInfo) {}
        override fun openDefaultLauncherSettings() {}
        override fun openWallpaperSettings() {}
        override fun showResetDefaultsSnackbar() {}
        override fun expandNotificationsPanel() {}
        override fun openSearch() {}
    }

    private class FakeAppsRepository : AppsRepository {
        private val gridFlow = MutableSharedFlow<Grid>(replay = 1)

        var updateCallCount = 0
        var lastUpdatedGrid: Grid? = null

        override val installedApps: Flow<List<App>> = MutableStateFlow(emptyList())
        override val grid: Flow<Grid> = gridFlow
        override val darkModePreference: Flow<DarkModePreference> =
            MutableStateFlow(DarkModePreference.SYSTEM)
        override val isBarVisible: Flow<Boolean> = MutableStateFlow(true)

        fun emitGrid(grid: Grid) {
            gridFlow.tryEmit(grid)
        }

        override fun setGridSpec(spec: GridSpec) {}

        override fun update(grid: Grid) {
            updateCallCount++
            lastUpdatedGrid = grid
        }

        override fun resetDefaults(spec: GridSpec) {}
        override fun setDarkModePreference(preference: DarkModePreference) {}
        override fun setBarVisible(visible: Boolean) {}
    }
}
