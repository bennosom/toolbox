package io.engst.launcher.ui.grid

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Integration test that verifies tapping an app tile launches the correct activity intent.
 *
 * Tests two layers:
 * 1. ViewModel: [GridIntent.AppTapped] → [GridEffectHandler.launchApp] with correct [ComponentName]
 * 2. UI: full [AppGrid] composable tap → [startActivity] with correct intent
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

    private fun buildTestGrid(): Grid {
        val page = mapOf(
            Cell(0, 0) to appA,
            Cell(1, 0) to null,
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

    // -----------------------------------------------------------------------
    // 1. ViewModel: AppTapped intent calls effectHandler.launchApp
    // -----------------------------------------------------------------------

    @Test
    fun app_tapped_intent_calls_effect_handler_with_correct_component() = runTest {
        setUpGrid()

        viewModel.onIntent(GridIntent.AppTapped(appA))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeEffectHandler.launchedApps.size)
        assertEquals(appA.componentName, fakeEffectHandler.launchedApps.first().componentName)
    }

    // -----------------------------------------------------------------------
    // 2. UI: tap on app tile clears menu (proves gesture reaches ViewModel)
    // -----------------------------------------------------------------------

    @Test
    fun tap_on_app_tile_clears_active_menu() {
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        // Set a menu to a non-null value so we can detect when the tap clears it
        viewModel.onIntent(GridIntent.AppMenuRequested("A"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("A", viewModel.uiState.value.activeAppMenuIdentifier)

        composeTestRule.onNodeWithTag("app_tile_A").performClick()
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Tap should clear the menu (proves handleAppTapped ran)",
            null,
            viewModel.uiState.value.activeAppMenuIdentifier,
        )
    }

    // -----------------------------------------------------------------------
    // 3. UI: tap on app tile triggers startActivity with correct intent
    // -----------------------------------------------------------------------

    @Test
    fun tap_on_app_tile_launches_activity_with_correct_component() {
        val realEffectHandler = GridEffectHandlerImpl(composeTestRule.activity)
        viewModel = GridViewModel(fakeRepository, realEffectHandler)
        setUpGrid()
        setContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("app_tile_A").performClick()

        // Process gesture → ViewModel → effectHandler.launchApp → startActivity
        repeat(5) {
            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()
        }

        val shadowActivity = shadowOf(composeTestRule.activity)
        val launchedIntent = shadowActivity.nextStartedActivity

        assertNotNull("An activity intent should have been launched", launchedIntent)
        assertEquals(
            "Launched intent should target the tapped app's component",
            appA.componentName,
            launchedIntent!!.component,
        )
        assertEquals(
            "Launched intent should be a MAIN action",
            Intent.ACTION_MAIN,
            launchedIntent.action,
        )
    }

    // -----------------------------------------------------------------------
    // Fakes
    // -----------------------------------------------------------------------

    private class FakeGridEffectHandler : GridEffectHandler {
        val launchedApps = mutableListOf<App>()

        override fun launchApp(app: App) { launchedApps += app }
        override fun openAppDetails(app: App) {}
        override fun removeApp(app: App) {}
        override fun launchShortcut(shortcut: android.content.pm.ShortcutInfo) {}
        override fun openAppManager() {}
        override fun openDefaultLauncherSettings() {}
        override fun showResetDefaultsSnackbar() {}
        override fun expandNotificationsPanel() {}
        override fun openSearch() {}
    }

    private class FakeAppsRepository : AppsRepository {
        private val gridFlow = MutableSharedFlow<Grid>(replay = 1)

        override val installedApps: Flow<List<App>> = MutableStateFlow(emptyList())
        override val grid: Flow<Grid> = gridFlow
        override val darkModePreference: Flow<DarkModePreference> =
            MutableStateFlow(DarkModePreference.SYSTEM)
        override val isBarVisible: Flow<Boolean> = MutableStateFlow(true)

        fun emitGrid(grid: Grid) { gridFlow.tryEmit(grid) }

        override fun setGridSpec(spec: GridSpec) {}
        override fun update(grid: Grid) {}
        override fun resetDefaults(spec: GridSpec) {}
        override fun setDarkModePreference(preference: DarkModePreference) {}
        override fun setBarVisible(visible: Boolean) {}
    }
}
