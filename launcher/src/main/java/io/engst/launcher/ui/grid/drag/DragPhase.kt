package io.engst.launcher.ui.grid.drag

import io.engst.launcher.model.Grid

sealed interface DragPhase {
    object Idle : DragPhase

    data class Active(
        val draggingAppId: String,
        val workingGrid: Grid,
    ) : DragPhase
}
