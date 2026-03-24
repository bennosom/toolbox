@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid.drag

import android.content.ClipData
import android.content.Intent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import io.engst.core.scopedLogger
import io.engst.launcher.model.App
import kotlinx.coroutines.withTimeoutOrNull

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
    onPressStarted: () -> Unit = {},
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
                onPressStarted()
                try {
                    val result = awaitLongPressOrSwipeOrTap(down, viewConfiguration)
                    when (result) {
                        is GestureResult.Tap -> {
                            logger.logDebug { "tap detected appId=${app.id}" }
                            onTap()
                            return@awaitEachGesture
                        }
                        is GestureResult.Swipe -> {
                            logger.logDebug { "swipe detected — ignoring appId=${app.id}" }
                            return@awaitEachGesture
                        }
                        is GestureResult.LongPress -> {
                            logger.logDebug { "long press detected appId=${app.id}" }
                            result.change.consumeDownChange()
                            onLongPress()
                        }
                    }

                    val dragDetected = awaitDragPastSlop(result.change, viewConfiguration)
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

internal sealed interface GestureResult {
    data class Tap(val change: PointerInputChange) : GestureResult
    data class LongPress(val change: PointerInputChange) : GestureResult
    object Swipe : GestureResult
}

/**
 * Waits for one of three outcomes after a pointer down:
 * - **Tap**: pointer lifted before long-press timeout and within touch-slop
 * - **Swipe**: pointer moved past touch-slop, or another handler consumed the gesture
 * - **LongPress**: pointer stayed within slop and unconsumed until the long-press timeout fired
 *
 * Uses [PointerEventPass.Final] so the pager's scroll handler processes events first on
 * [PointerEventPass.Main]. The additional distance check ensures the tile yields the gesture
 * *before* the pager starts scrolling, preventing a visible jump on the first drag frame.
 *
 * Long-press timing uses [withTimeoutOrNull] so it integrates with the coroutine dispatcher
 * clock, which the Compose test framework can advance via `mainClock.advanceTimeBy()`.
 */
internal suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitLongPressOrSwipeOrTap(
    down: PointerInputChange,
    viewConfiguration: ViewConfiguration,
    eventPass: PointerEventPass = PointerEventPass.Final,
): GestureResult {
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val slopSquared = viewConfiguration.touchSlop * viewConfiguration.touchSlop
    val anchor = down.position
    var lastChange = down
    val earlyResult = withTimeoutOrNull(longPressTimeout) {
        while (true) {
            val event = awaitPointerEvent(eventPass)
            val change = event.changes.firstOrNull { it.id == down.id }
                ?: return@withTimeoutOrNull GestureResult.Swipe
            lastChange = change
            if (change.isConsumed) return@withTimeoutOrNull GestureResult.Swipe
            val distanceSq = (change.position - anchor).let { it.x * it.x + it.y * it.y }
            if (distanceSq > slopSquared) return@withTimeoutOrNull GestureResult.Swipe
            if (change.changedToUp()) {
                change.consumeDownChange()
                return@withTimeoutOrNull GestureResult.Tap(change)
            }
        }
        @Suppress("UNREACHABLE_CODE") error("unreachable")
    }
    return earlyResult ?: GestureResult.LongPress(lastChange)
}

/**
 * After a long press, wait until the pointer moves past [ViewConfiguration.touchSlop]
 * or is released. Returns `true` if drag threshold was exceeded.
 */
internal suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitDragPastSlop(
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

