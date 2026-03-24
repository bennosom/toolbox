@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid.drag

import android.content.ClipData
import android.content.Intent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import io.engst.core.scopedLogger
import io.engst.launcher.model.App

private val logger = scopedLogger("DraggableAppSource")

/**
 * Shared modifier that adds long-press → drag gesture handling to any app tile.
 * Encapsulates ClipData construction, drag shadow rendering, and gesture routing.
 * Used by both grid tiles and quick-bar icons to eliminate duplicated gesture code.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.draggableAppSource(
    app: App,
    iconSizeDp: Dp,
    density: Density,
    viewConfiguration: ViewConfiguration,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDragStarted: () -> Unit,
    onGestureCompleted: () -> Unit = {},
): Modifier {
    val transferData = DragAndDropTransferData(
        clipData = ClipData.newIntent(
            app.label,
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.componentName),
        ),
        flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE,
    )
    return dragAndDropSource(
        drawDragDecoration = {
            val shadowSizePx = with(density) { iconSizeDp.roundToPx() }
            val bitmap = app.icon.toBitmap(shadowSizePx, shadowSizePx).asImageBitmap()
            drawImage(bitmap)
        },
        block = {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                try {
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            val duration = up.uptimeMillis - down.uptimeMillis
                            val movement = (up.position - down.position).getDistance()
                            val isTap = duration < viewConfiguration.longPressTimeoutMillis &&
                                movement < viewConfiguration.touchSlop
                            if (isTap) {
                                up.consumeDownChange()
                                logger.logDebug { "tap detected appId=${app.id}" }
                                onTap()
                            }
                        }
                        return@awaitEachGesture
                    }

                    logger.logDebug { "long press detected appId=${app.id}" }
                    onLongPress()

                    val pointerId = longPress.id
                    val dragChange = awaitDragOrCancellation(pointerId)
                    if (dragChange == null || dragChange.changedToUp()) {
                        dragChange?.consumeDownChange()
                        return@awaitEachGesture
                    }

                    logger.logDebug { "drag started appId=${app.id}" }
                    onDragStarted()
                    dragChange.consumeDownChange()
                    dragChange.consumePositionChange()
                    startTransfer(transferData)
                } finally {
                    onGestureCompleted()
                }
            }
        },
    )
}
