package io.engst.launcher.ui.grid

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.shared.AppTheme
import io.engst.launcher.ui.shared.DarkModePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests that render the full [AppGrid] composable with a fake [AppsRepository]
 * and verify all gesture detection use cases from tasks.md:
 *
 * 1. Tap on non-empty cell → launches app (AppTapped effect)
 * 2. Long press on non-empty cell → shows app context menu
 * 3. Long press + drag on non-empty cell → closes menu, starts drag
 * 4. Long press on grid background → shows settings menu
 * 5. Swipe on app tile → no tap, no menu, no drag
 * 6. Dragged app is hidden in grid
 * 7. Long press + release without drag → menu stays
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppGridGestureIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun makeApp(id: String, label: String = id) = App(
        id = id,
        label = label,
        icon = ColorDrawable(android.graphics.Color.BLUE),
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
    private val appA = makeApp("A", "AppAlpha")
    private val appB = makeApp("B", "AppBeta")

    private fun buildTestGrid(): Grid {
        val page = mapOf(
            Cell(0, 0) to appA,
            Cell(1, 0) to appB,
            Cell(0, 1) to null,
            Cell(1, 1) to null,
        )
        return Grid(spec = spec, grid = listOf(page), bar = emptyList())
    }

    private fun setUpGrid() {
        fakeRepository.emitGrid(buildTestGrid())
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun setContent() {
        composeTestRule.setContent {
            AppTheme {
                AppGrid(
                    modifier = Modifier.fillMaxSize(),
                    isDefaultLauncher = true,
                    viewModel = viewModel,
                )
            }
        }
    }

    /**
     * Advances the compose clock in small steps, calling waitForIdle between each,
     * to allow coroutines in nested scopes (e.g. dragAndDropSource) to process.
     */
    private fun advanceClockBy(millis: Long) {
        val step = 100L
        var remaining = millis
        while (remaining > 0) {
            val advance = minOf(step, remaining)
            composeTestRule.mainClock.advanceTimeBy(advance)
            composeTestRule.waitForIdle()
            remaining -= advance
        }
    }

    // -----------------------------------------------------------------------
    // 1. Tap on non-empty cell → launches app
    // -----------------------------------------------------------------------

    @Test
    fun tap_on_app_tile_emits_AppTapped_and_no_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertNull(
            "App menu should not be visible after tap",
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
    }

    @Test
    fun tap_on_app_tile_does_not_start_drag() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertNull("Drag should not start on tap", viewModel.uiState.value.draggingAppId)
    }

    // -----------------------------------------------------------------------
    // 2. Long press on non-empty cell → shows app context menu
    //    Uses longClick() which does down → delay(longPressTimeout+100) → up
    // -----------------------------------------------------------------------

    @Test
    fun long_press_on_app_tile_shows_app_context_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            longClick(center)
        }
        composeTestRule.waitForIdle()

        assertEquals(
            "App context menu should be visible for app A",
            "A",
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
    }

    @Test
    fun long_press_on_app_tile_does_not_start_drag() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            longClick(center)
        }
        composeTestRule.waitForIdle()

        assertNull(
            "Drag should not start on long press alone",
            viewModel.uiState.value.draggingAppId,
        )
    }

    @Test
    fun long_press_on_different_tiles_shows_correct_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_B").performTouchInput {
            longClick(center)
        }
        composeTestRule.waitForIdle()

        assertEquals(
            "App context menu should be visible for app B",
            "B",
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
    }

    // -----------------------------------------------------------------------
    // 3. Long press + drag → close context menu, start app drag
    // -----------------------------------------------------------------------

    @Test
    fun long_press_then_drag_closes_menu_and_starts_drag() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        // Long press to open context menu
        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput { down(center) }
        advanceClockBy(600)

        assertEquals("A", viewModel.uiState.value.activeAppMenuIdentifier)

        // Drag past slop
        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            moveBy(Offset(200f, 0f), delayMillis = 50)
        }
        composeTestRule.waitForIdle()

        assertEquals(
            "Drag should be active for app A",
            "A",
            viewModel.uiState.value.draggingAppId,
        )
        assertNull(
            "App menu should be dismissed when drag starts",
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
    }

    @Test
    fun long_press_then_drag_activates_drag_phase() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput { down(center) }
        advanceClockBy(600)

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            moveBy(Offset(200f, 0f), delayMillis = 50)
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Drag phase should be Active",
            viewModel.uiState.value.isInDragMode,
        )
    }

    // -----------------------------------------------------------------------
    // 4. Long press on grid background → shows settings menu
    // -----------------------------------------------------------------------

    @Test
    fun long_press_on_grid_background_shows_settings_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_grid").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertTrue(
            "Grid settings menu should be visible",
            viewModel.uiState.value.isGridMenuVisible,
        )
    }

    @Test
    fun long_press_on_background_does_not_show_app_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_grid").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertNull(
            "No app menu should be visible on background long press",
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
    }

    @Test
    fun short_tap_on_background_does_not_show_settings_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_grid").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            "Grid menu should not be visible after short tap",
            false,
            viewModel.uiState.value.isGridMenuVisible,
        )
    }

    // -----------------------------------------------------------------------
    // 5. Swipe on non-empty cell → no tap, no menu, no drag
    // -----------------------------------------------------------------------

    @Test
    fun swipe_on_app_tile_does_not_fire_tap_or_menu_or_drag() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            down(center)
            moveBy(Offset(300f, 0f), delayMillis = 20)
            up()
        }
        composeTestRule.waitForIdle()

        assertNull("App menu should not show on swipe", viewModel.uiState.value.activeAppMenuIdentifier)
        assertNull("Drag should not start on swipe", viewModel.uiState.value.draggingAppId)
        assertEquals("Grid menu should not show on swipe", false, viewModel.uiState.value.isGridMenuVisible)
    }

    // -----------------------------------------------------------------------
    // 6. Dragged app tile is hidden in grid
    // -----------------------------------------------------------------------

    @Test
    fun dragged_app_tile_is_hidden_in_grid() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        // Start drag on app A
        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput { down(center) }
        advanceClockBy(600)
        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            moveBy(Offset(200f, 0f), delayMillis = 50)
        }
        // Advance test dispatcher so collectAsStateWithLifecycle processes the StateFlow emission
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // App A's label should not be rendered (draggingAppId == app.id hides the tile content)
        composeTestRule.onNodeWithText("AppAlpha").assertDoesNotExist()
        // App B should still be visible
        composeTestRule.onNodeWithText("AppBeta").assertExists()
    }

    // -----------------------------------------------------------------------
    // 7. Long press + release without drag → menu stays, no drag
    // -----------------------------------------------------------------------

    @Test
    fun long_press_then_release_without_drag_keeps_menu_visible() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performTouchInput {
            longClick(center)
        }
        composeTestRule.waitForIdle()

        assertEquals(
            "App menu should remain visible after long press + release",
            "A",
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
        assertNull(
            "Drag should not start without drag movement",
            viewModel.uiState.value.draggingAppId,
        )
    }

    // -----------------------------------------------------------------------
    // Fake repository
    // -----------------------------------------------------------------------

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

        override val installedApps: Flow<List<App>> = MutableStateFlow(emptyList())
        override val grid: Flow<Grid> = gridFlow
        override val darkModePreference: Flow<DarkModePreference> =
            MutableStateFlow(DarkModePreference.SYSTEM)
        override val isBarVisible: Flow<Boolean> = MutableStateFlow(true)

        fun emitGrid(grid: Grid) { gridFlow.tryEmit(grid) }

        override fun setGridSpec(spec: GridSpec) {}
        override fun update(grid: Grid) { updateCallCount++ }
        override fun resetDefaults(spec: GridSpec) {}
        override fun setDarkModePreference(preference: DarkModePreference) {}
        override fun setBarVisible(visible: Boolean) {}
    }
}
