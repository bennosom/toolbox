package io.engst.launcher.ui.shared

import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates

/**
 * Converts a platform [DragAndDropEvent] position into local coordinates
 * relative to this [LayoutCoordinates].
 *
 * Returns `null` if coordinates are detached.
 */
fun LayoutCoordinates.localOffsetOf(event: DragAndDropEvent): Offset? {
    if (!isAttached) return null
    val root = findRootCoordinates()
    if (!root.isAttached) return null
    val drag = event.toAndroidDragEvent()
    return localPositionOf(root, Offset(drag.x, drag.y))
}
