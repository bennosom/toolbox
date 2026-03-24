@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid.drag

import android.content.ClipData
import android.content.Intent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import io.engst.core.scopedLogger
import io.engst.launcher.model.App

private val logger = scopedLogger("DraggableAppSource")

/**
 * Touch interaction contract for app tiles:
 * 1. **Tap** → launch app
 * 2. **Long press** → show app context menu
 * 3. **Long press + drag past [ViewConfiguration.touchSlop]** → dismiss menu, start grid drag
 *    (grid shrinks to reveal page edges for cross-page dragging)
 * 4. **Release** → commit or cancel drag, grid returns to normal scale
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
                        handleTapCandidate(down, viewConfiguration, app, onTap)
                        return@awaitEachGesture
                    }

                    logger.logDebug { "long press detected appId=${app.id}" }
                    longPress.consumeDownChange()
                    onLongPress()

                    val dragDetected = awaitDragPastSlop(longPress, viewConfiguration)
                    if (dragDetected) {
                        logger.logDebug { "drag started appId=${app.id}" }
                        onDragStarted()
                        startTransfer(transferData)
                    }
                } finally {
                    onGestureCompleted()
                }
            }
        },
    )
}

/**
 * After a long press, wait until the pointer moves past [ViewConfiguration.touchSlop]
 * or is released. Returns `true` if drag threshold was exceeded.
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitDragPastSlop(
    longPress: PointerInputChange,
    viewConfiguration: ViewConfiguration,
): Boolean {
    val slopSquared = viewConfiguration.touchSlop * viewConfiguration.touchSlop
    val anchor = longPress.position
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == longPress.id } ?: return false
        if (change.changedToUp()) {
            change.consumeDownChange()
            return false
        }
        val distanceSquared = (change.position - anchor).let { it.x * it.x + it.y * it.y }
        if (distanceSquared > slopSquared) {
            change.consumeDownChange()
            return true
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.handleTapCandidate(
    down: PointerInputChange,
    viewConfiguration: ViewConfiguration,
    app: App,
    onTap: () -> Unit,
) {
    val up = waitForUpOrCancellation() ?: return
    val duration = up.uptimeMillis - down.uptimeMillis
    val movement = (up.position - down.position).getDistance()
    if (duration < viewConfiguration.longPressTimeoutMillis && movement < viewConfiguration.touchSlop) {
        up.consumeDownChange()
        logger.logDebug { "tap detected appId=${app.id}" }
        onTap()
    }
}
