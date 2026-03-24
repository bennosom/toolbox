package io.engst.launcher.ui.grid

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose-level gesture tests for the background long-press that opens the grid settings menu.
 * Mirrors the `detectTapGestures(onLongPress = ...)` usage in [AppGrid].
 */
@RunWith(RobolectricTestRunner::class)
class BackgroundLongPressGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var longPressPosition: Offset? = null
    private var longPressCount = 0

    private fun resetState() {
        longPressPosition = null
        longPressCount = 0
    }

    private fun setContent() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("background")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { position ->
                                longPressPosition = position
                                longPressCount++
                            },
                        )
                    },
            )
        }
    }

    @Test
    fun long_press_on_background_fires_callback_with_position() {
        resetState()
        setContent()

        composeTestRule.onNodeWithTag("background").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertEquals("long press should fire once", 1, longPressCount)
        assertNotNull("position should be reported", longPressPosition)
    }

    @Test
    fun short_tap_on_background_does_not_fire_long_press() {
        resetState()
        setContent()

        composeTestRule.onNodeWithTag("background").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("long press should not fire on short tap", 0, longPressCount)
        assertNull("position should remain null", longPressPosition)
    }

    @Test
    fun moving_past_slop_cancels_long_press() {
        resetState()
        setContent()

        composeTestRule.onNodeWithTag("background").performTouchInput {
            down(center)
            moveBy(Offset(300f, 0f), delayMillis = 50)
        }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertEquals("long press should not fire when pointer moves past slop", 0, longPressCount)
    }
}