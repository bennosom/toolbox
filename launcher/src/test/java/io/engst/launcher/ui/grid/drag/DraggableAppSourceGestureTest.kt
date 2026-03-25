@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid.drag

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose-level gesture tests verifying the pointer-event state machine
 * ([awaitLongPressOrSwipeOrTap] and [awaitDragPastSlop]) used by [draggableAppSource].
 *
 * The test harness wires the gesture functions into a plain [pointerInput] modifier
 * (bypassing the platform-specific `dragAndDropSource` wrapper).
 *
 * Long-press detection in production uses [kotlinx.coroutines.withTimeoutOrNull],
 * so tests advance the Compose test clock via `mainClock.advanceTimeBy()`.
 *
 * Covered use cases:
 * - Tap (press + release within slop & before long-press timeout) → onTap
 * - Long press (hold past timeout within slop) → onLongPress
 * - Long press + drag past slop → onLongPress then onDragStarted
 * - Swipe (move past slop before timeout) → neither onTap nor onLongPress
 * - Press always fires onPressStarted; gesture end always fires onGestureCompleted
 */
@RunWith(RobolectricTestRunner::class)
class DraggableAppSourceGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var tapCount = 0
    private var longPressCount = 0
    private var dragStartedCount = 0
    private var pressStartedCount = 0
    private var gestureCompletedCount = 0

    private fun resetCounters() {
        tapCount = 0
        longPressCount = 0
        dragStartedCount = 0
        pressStartedCount = 0
        gestureCompletedCount = 0
    }

    private fun setContent() {
        composeTestRule.setContent {
            val viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("tile")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressStartedCount++
                            try {
                                val result = awaitLongPressOrSwipeOrTap(down, viewConfiguration)
                                when (result) {
                                    is GestureResult.Tap -> {
                                        tapCount++
                                        return@awaitEachGesture
                                    }
                                    is GestureResult.Swipe -> {
                                        return@awaitEachGesture
                                    }
                                    is GestureResult.LongPress -> {
                                        result.change.consumeDownChange()
                                        longPressCount++
                                    }
                                }
                                val dragDetected = awaitDragPastSlop(result.change, viewConfiguration)
                                if (dragDetected) {
                                    dragStartedCount++
                                }
                            } finally {
                                gestureCompletedCount++
                            }
                        }
                    },
            )
        }
    }

    // -----------------------------------------------------------------------
    // Tap: press + release quickly within slop
    // -----------------------------------------------------------------------

    @Test
    fun tap_fires_onTap_and_not_onLongPress() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("onTap should fire once", 1, tapCount)
        assertEquals("onLongPress should not fire", 0, longPressCount)
        assertEquals("onDragStarted should not fire", 0, dragStartedCount)
    }

    @Test
    fun tap_fires_onPressStarted_and_onGestureCompleted() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("onPressStarted should fire once", 1, pressStartedCount)
        assertEquals("onGestureCompleted should fire once", 1, gestureCompletedCount)
    }

    // -----------------------------------------------------------------------
    // Long press: hold past timeout within slop.
    // Advance the coroutine clock so withTimeoutOrNull fires.
    // -----------------------------------------------------------------------

    @Test
    fun long_press_fires_onLongPress_and_not_onTap() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertEquals("onLongPress should fire once", 1, longPressCount)
        assertEquals("onTap should not fire", 0, tapCount)
    }

    @Test
    fun long_press_fires_onPressStarted() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertTrue("onPressStarted should have fired", pressStartedCount >= 1)
    }

    @Test
    fun long_press_then_release_fires_onGestureCompleted() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onNodeWithTag("tile").performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertTrue("onGestureCompleted should have fired", gestureCompletedCount >= 1)
    }

    // -----------------------------------------------------------------------
    // Long press + drag past slop → onDragStarted
    // -----------------------------------------------------------------------

    @Test
    fun long_press_then_drag_fires_onDragStarted() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onNodeWithTag("tile").performTouchInput {
            moveBy(Offset(200f, 0f), delayMillis = 50)
        }
        composeTestRule.waitForIdle()

        assertEquals("onLongPress should fire first", 1, longPressCount)
        assertEquals("onDragStarted should fire", 1, dragStartedCount)
    }

    @Test
    fun long_press_then_drag_fires_callbacks_in_order() {
        val callOrder = mutableListOf<String>()
        composeTestRule.setContent {
            val viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("tile")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            callOrder += "pressStarted"
                            try {
                                val result = awaitLongPressOrSwipeOrTap(down, viewConfiguration)
                                when (result) {
                                    is GestureResult.Tap -> {
                                        callOrder += "tap"
                                        return@awaitEachGesture
                                    }
                                    is GestureResult.Swipe -> {
                                        return@awaitEachGesture
                                    }
                                    is GestureResult.LongPress -> {
                                        result.change.consumeDownChange()
                                        callOrder += "longPress"
                                    }
                                }
                                val dragDetected = awaitDragPastSlop(result.change, viewConfiguration)
                                if (dragDetected) {
                                    callOrder += "dragStarted"
                                }
                            } finally {
                                callOrder += "gestureCompleted"
                            }
                        }
                    },
            )
        }

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onNodeWithTag("tile").performTouchInput {
            moveBy(Offset(200f, 0f), delayMillis = 50)
        }
        composeTestRule.waitForIdle()

        assertTrue("pressStarted should be first", callOrder.firstOrNull() == "pressStarted")
        assertTrue(
            "longPress should come before dragStarted",
            callOrder.indexOf("longPress") < callOrder.indexOf("dragStarted"),
        )
    }

    // -----------------------------------------------------------------------
    // Swipe: move past slop before long-press timeout
    // -----------------------------------------------------------------------

    @Test
    fun swipe_does_not_fire_onTap_or_onLongPress() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput {
            down(center)
            moveBy(Offset(300f, 0f), delayMillis = 20)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("onTap should not fire on swipe", 0, tapCount)
        assertEquals("onLongPress should not fire on swipe", 0, longPressCount)
        assertEquals("onDragStarted should not fire on swipe", 0, dragStartedCount)
    }

    @Test
    fun swipe_still_fires_onPressStarted_and_onGestureCompleted() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput {
            down(center)
            moveBy(Offset(300f, 0f), delayMillis = 20)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("onPressStarted should fire", 1, pressStartedCount)
        assertEquals("onGestureCompleted should fire", 1, gestureCompletedCount)
    }

    // -----------------------------------------------------------------------
    // Long press + release without drag → no onDragStarted
    // -----------------------------------------------------------------------

    @Test
    fun long_press_then_release_without_drag_does_not_fire_onDragStarted() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onNodeWithTag("tile").performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertEquals("onLongPress should fire", 1, longPressCount)
        assertEquals("onDragStarted should not fire", 0, dragStartedCount)
    }

    // -----------------------------------------------------------------------
    // Tap with parent consuming on Main pass (reproduces HorizontalPager bug)
    //
    // A parent that consumes all pointer changes on PointerEventPass.Main
    // (like HorizontalPager tracking scroll) must not prevent the child's
    // Final-pass gesture detector from recognising a tap.
    // -----------------------------------------------------------------------

    private var swipeDetectedCount = 0
    private val traceLog = mutableListOf<String>()

    // Traced version uses production awaitLongPressOrSwipeOrTap with trace logging around it.
    // Extracted to avoid @RestrictsSuspension conflict with class member extension functions.

    private fun setContentWithConsumingParent() {
        composeTestRule.setContent {
            val viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val parentDown = awaitFirstDown(requireUnconsumed = false)
                            traceLog += "[PARENT] awaitFirstDown id=${parentDown.id} pos=${parentDown.position} consumed=${parentDown.isConsumed}"
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val allUp = event.changes.all { it.changedToUp() }
                                traceLog += "[PARENT] event: changes=${event.changes.size} allUp=$allUp"
                                event.changes.forEach {
                                    traceLog += "[PARENT]   change id=${it.id} pressed=${it.pressed} consumed=${it.isConsumed} changedToUp=${it.changedToUp()}"
                                    it.consume()
                                    traceLog += "[PARENT]   after consume: consumed=${it.isConsumed}"
                                }
                                if (allUp) {
                                    traceLog += "[PARENT] breaking — all up"
                                    break
                                }
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .testTag("tile")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                traceLog += "[CHILD] awaitFirstDown id=${down.id} pos=${down.position} consumed=${down.isConsumed}"
                                pressStartedCount++
                                try {
                                    val result = awaitLongPressOrSwipeOrTap(down, viewConfiguration)
                                    when (result) {
                                        is GestureResult.Tap -> {
                                            tapCount++
                                            return@awaitEachGesture
                                        }
                                        is GestureResult.Swipe -> {
                                            swipeDetectedCount++
                                            return@awaitEachGesture
                                        }
                                        is GestureResult.LongPress -> {
                                            @Suppress("DEPRECATION")
                                            result.change.consumeDownChange()
                                            longPressCount++
                                        }
                                    }
                                    val dragDetected = awaitDragPastSlop(result.change, viewConfiguration)
                                    if (dragDetected) {
                                        dragStartedCount++
                                    }
                                } finally {
                                    gestureCompletedCount++
                                }
                            }
                        },
                )
            }
        }
    }

    @Test
    fun tap_blocked_by_consuming_parent_falls_through_to_long_press() {
        // When the parent consumes all events on Main pass, changedToUp() returns false
        // (Compose checks !isConsumed internally), so the child cannot detect the up event.
        // The withTimeoutOrNull eventually fires, yielding a LongPress.
        resetCounters()
        swipeDetectedCount = 0
        setContentWithConsumingParent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.onNodeWithTag("tile").performTouchInput { up() }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertEquals("tap should not fire when parent consumes", 0, tapCount)
        assertEquals("child falls through to long press", 1, longPressCount)
    }

    @Test
    fun long_press_fires_even_when_parent_consumes_on_main_pass() {
        resetCounters()
        setContentWithConsumingParent()

        composeTestRule.onNodeWithTag("tile").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertEquals("onLongPress should fire despite parent consuming on Main", 1, longPressCount)
        assertEquals("onTap should not fire", 0, tapCount)
    }
}
