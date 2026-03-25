package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.Grid
import io.engst.launcher.model.GridSpec
import io.engst.launcher.ui.grid.drag.DragPhase

data class GridScreenState(
    val persistedGrid: Grid? = null,
    val dragPhase: DragPhase = DragPhase.Idle,
    val activeAppMenuIdentifier: String? = null,
    val isGridMenuVisible: Boolean = false,
    val gridMenuOffset: DpOffset = DpOffset(0.dp, 0.dp),
    val isBarVisible: Boolean = true,
) {
    val isInDragMode: Boolean
        get() = dragPhase is DragPhase.Active

    val draggingAppId: String?
        get() = (dragPhase as? DragPhase.Active)?.draggingAppId

    val effectiveGrid: Grid?
        get() = when (val phase = dragPhase) {
            is DragPhase.Idle -> persistedGrid
            is DragPhase.Active -> phase.workingGrid
        }

    val displayGrid: Grid?
        get() {
            val base = effectiveGrid ?: return null
            return if (isInDragMode) {
                base.copy(grid = base.grid + base.grid.last().mapValues { null })
            } else {
                base
            }
        }
}

sealed interface GridIntent {
    data class DragStarted(val appId: String) : GridIntent
    data class DragMovedToGridCell(val pageIndex: Int, val cell: Cell) : GridIntent
    data class DragMovedToQuickBarSlot(val targetIndex: Int) : GridIntent
    object DragDropped : GridIntent
    object DragCancelled : GridIntent
    data class AppTapped(val app: App) : GridIntent
    data class AppMenuRequested(val appId: String) : GridIntent
    object AppMenuDismissed : GridIntent
    data class GridMenuRequested(val offset: DpOffset) : GridIntent
    object GridMenuDismissed : GridIntent
    data class GridSpecChangeRequested(val spec: GridSpec) : GridIntent
    data class AppDetailsRequested(val app: App) : GridIntent
    data class AppRemovalRequested(val app: App) : GridIntent
    data class ShortcutLaunchRequested(val shortcut: ShortcutInfo) : GridIntent
    object AppManagerOpenRequested : GridIntent
    object DefaultLauncherSettingsOpenRequested : GridIntent
    object ResetDefaultsRequested : GridIntent
    object ResetDefaultsConfirmed : GridIntent
    object ResetDefaultsUndone : GridIntent
    data class BarVisibilityChanged(val visible: Boolean) : GridIntent
    object SwipeDownDetected : GridIntent
    object SwipeUpDetected : GridIntent
}
