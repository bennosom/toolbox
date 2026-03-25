package io.engst.launcher.ui.grid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import io.engst.core.scopedLogger

private val logger = scopedLogger("SwipeGestureHandler")
private const val SWIPE_THRESHOLD_PX = 100f

/**
 * Detects vertical swipe gestures on the home screen background.
 * Swipe down → notifications panel. Swipe up → search placeholder.
 *
 * Uses [PointerEventPass.Final] so child composables (app tiles) can consume
 * events first, preventing this handler from stealing drags meant for tile reordering.
 */
fun Modifier.homeScreenSwipeGestures(
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        var cumulativeDrag = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.isConsumed) break
            if (change.changedToUp()) {
                when {
                    cumulativeDrag > SWIPE_THRESHOLD_PX -> {
                        logger.logDebug { "swipe down detected — opening notifications" }
                        onSwipeDown()
                    }
                    cumulativeDrag < -SWIPE_THRESHOLD_PX -> {
                        logger.logDebug { "swipe up detected — search placeholder" }
                        onSwipeUp()
                    }
                }
                break
            }
            cumulativeDrag += change.positionChange().y
        }
    }
}
