package io.engst.launcher.ui.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.engst.core.Logging
import io.engst.core.scopedLogger
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.model.DragDestination
import io.engst.launcher.model.applyDragMove
import io.engst.launcher.ui.grid.drag.DragPhase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GridViewModel(
    private val repository: AppsRepository,
) : ViewModel(), Logging by scopedLogger("GridViewModel") {

    private val _uiState = MutableStateFlow(GridScreenState())
    val uiState: StateFlow<GridScreenState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<GridEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<GridEffect> = _effects.asSharedFlow()

    init {
        logInfo { "initialised — subscribing to repository grid" }
        viewModelScope.launch {
            repository.grid.collect { grid ->
                logDebug { "grid updated spec=${grid.spec} pages=${grid.grid.size} bar=${grid.bar.size}" }
                _uiState.update { state ->
                    val updatedPhase = when (val phase = state.dragPhase) {
                        is DragPhase.Idle -> DragPhase.Idle
                        is DragPhase.Active -> phase.copy(workingGrid = grid)
                    }
                    state.copy(persistedGrid = grid, dragPhase = updatedPhase)
                }
            }
        }
    }

    fun onIntent(intent: GridIntent) {
        logDebug { "onIntent: $intent" }
        when (intent) {
            is GridIntent.DragStarted -> handleDragStarted(intent.appId)
            is GridIntent.DragMovedToGridCell -> handleDragMovedToGridCell(intent.pageIndex, intent.cell)
            is GridIntent.DragMovedToQuickBarSlot -> handleDragMovedToQuickBarSlot(intent.targetIndex)
            is GridIntent.DragDropped -> handleDragDropped()
            is GridIntent.DragCancelled -> handleDragCancelled()
            is GridIntent.AppTapped -> handleAppTapped(intent)
            is GridIntent.AppMenuRequested -> _uiState.update {
                it.copy(activeAppMenuIdentifier = intent.appId, isGridMenuVisible = false)
            }
            is GridIntent.AppMenuDismissed -> _uiState.update { it.copy(activeAppMenuIdentifier = null) }
            is GridIntent.GridMenuRequested -> _uiState.update {
                if (it.activeAppMenuIdentifier != null) it
                else it.copy(isGridMenuVisible = true, gridMenuOffset = intent.offset)
            }
            is GridIntent.GridMenuDismissed -> _uiState.update { it.copy(isGridMenuVisible = false) }
            is GridIntent.GridSpecChangeRequested -> {
                repository.setGridSpec(intent.spec)
            }
            is GridIntent.AppDetailsRequested -> emitEffect(GridEffect.OpenAppDetails(intent.app))
            is GridIntent.AppRemovalRequested -> emitEffect(GridEffect.RemoveApp(intent.app))
            is GridIntent.ShortcutLaunchRequested -> emitEffect(GridEffect.LaunchShortcut(intent.shortcut))
            is GridIntent.AppManagerOpenRequested -> {
                _uiState.update { it.copy(isGridMenuVisible = false) }
                emitEffect(GridEffect.OpenAppManager)
            }
            is GridIntent.DefaultLauncherSettingsOpenRequested -> {
                _uiState.update { it.copy(isGridMenuVisible = false) }
                emitEffect(GridEffect.OpenDefaultLauncherSettings)
            }
        }
    }

    private fun handleDragStarted(appId: String) {
        val grid = _uiState.value.persistedGrid ?: run {
            logWarn { "DragStarted ignored — no persisted grid" }
            return
        }
        logDebug { "drag started appId=$appId" }
        _uiState.update { state ->
            state.copy(
                dragPhase = DragPhase.Active(draggingAppId = appId, workingGrid = grid),
                activeAppMenuIdentifier = null,
                isGridMenuVisible = false,
            )
        }
    }

    private fun handleDragMovedToGridCell(pageIndex: Int, cell: io.engst.launcher.model.Cell) {
        val phase = _uiState.value.dragPhase as? DragPhase.Active ?: return
        val updated = phase.workingGrid.applyDragMove(
            phase.draggingAppId,
            DragDestination.GridCell(pageIndex, cell),
        )
        if (updated != phase.workingGrid) {
            _uiState.update { it.copy(dragPhase = phase.copy(workingGrid = updated)) }
        }
    }

    private fun handleDragMovedToQuickBarSlot(targetIndex: Int) {
        val phase = _uiState.value.dragPhase as? DragPhase.Active ?: return
        val updated = phase.workingGrid.applyDragMove(
            phase.draggingAppId,
            DragDestination.QuickBarSlot(targetIndex),
        )
        if (updated != phase.workingGrid) {
            _uiState.update { it.copy(dragPhase = phase.copy(workingGrid = updated)) }
        }
    }

    private fun handleDragDropped() {
        val phase = _uiState.value.dragPhase as? DragPhase.Active ?: run {
            logWarn { "DragDropped received but drag phase is already Idle" }
            return
        }
        logDebug { "drag dropped — committing working grid" }
        repository.update(phase.workingGrid)
        _uiState.update { it.copy(dragPhase = DragPhase.Idle) }
    }

    private fun handleDragCancelled() {
        if (_uiState.value.dragPhase is DragPhase.Idle) {
            logDebug { "DragCancelled ignored — already Idle" }
            return
        }
        logDebug { "drag cancelled — discarding working grid" }
        _uiState.update { it.copy(dragPhase = DragPhase.Idle) }
    }

    private fun handleAppTapped(intent: GridIntent.AppTapped) {
        _uiState.update { it.copy(activeAppMenuIdentifier = null) }
        emitEffect(GridEffect.LaunchApp(intent.app))
    }

    private fun emitEffect(effect: GridEffect) {
        logDebug { "emitEffect: $effect" }
        val emitted = _effects.tryEmit(effect)
        if (!emitted) logWarn { "effect dropped — buffer full: $effect" }
    }
}
