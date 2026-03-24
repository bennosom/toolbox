package io.engst.launcher.ui.grid

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import io.engst.core.scopedLogger

private val logger = scopedLogger("SwipeGestureHandler")
private const val SWIPE_THRESHOLD_PX = 100f

/**
 * Detects vertical swipe gestures on the home screen background.
 * Swipe down → notifications panel. Swipe up → search placeholder.
 */
fun Modifier.homeScreenSwipeGestures(
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
): Modifier = pointerInput(Unit) {
    var cumulativeDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { cumulativeDrag = 0f },
        onVerticalDrag = { _, dragAmount -> cumulativeDrag += dragAmount },
        onDragEnd = {
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
        },
    )
}
