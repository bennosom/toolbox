package io.engst.launcher.ui.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose-level gesture tests for [homeScreenSwipeGestures]:
 * - Vertical swipe down → onSwipeDown
 * - Vertical swipe up → onSwipeUp
 * - Small drag below threshold → neither callback fires
 */
@RunWith(RobolectricTestRunner::class)
class SwipeGestureHandlerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var swipeDownCount = 0
    private var swipeUpCount = 0

    private fun resetCounters() {
        swipeDownCount = 0
        swipeUpCount = 0
    }

    private fun setContent() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("surface")
                    .homeScreenSwipeGestures(
                        onSwipeDown = { swipeDownCount++ },
                        onSwipeUp = { swipeUpCount++ },
                    ),
            )
        }
    }

    @Test
    fun swipe_down_fires_onSwipeDown() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("surface").performTouchInput {
            swipeDown()
        }
        composeTestRule.waitForIdle()

        assertEquals("onSwipeDown should fire once", 1, swipeDownCount)
        assertEquals("onSwipeUp should not fire", 0, swipeUpCount)
    }

    @Test
    fun swipe_up_fires_onSwipeUp() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("surface").performTouchInput {
            swipeUp()
        }
        composeTestRule.waitForIdle()

        assertEquals("onSwipeUp should fire once", 1, swipeUpCount)
        assertEquals("onSwipeDown should not fire", 0, swipeDownCount)
    }

    @Test
    fun small_vertical_drag_below_threshold_fires_neither_callback() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("surface").performTouchInput {
            down(center)
            // Move only ~30px — well below the 100px threshold
            moveBy(androidx.compose.ui.geometry.Offset(0f, 30f), delayMillis = 50)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("onSwipeDown should not fire", 0, swipeDownCount)
        assertEquals("onSwipeUp should not fire", 0, swipeUpCount)
    }

    @Test
    fun horizontal_swipe_fires_neither_callback() {
        resetCounters()
        setContent()

        composeTestRule.onNodeWithTag("surface").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(300f, 0f), delayMillis = 100)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals("onSwipeDown should not fire on horizontal swipe", 0, swipeDownCount)
        assertEquals("onSwipeUp should not fire on horizontal swipe", 0, swipeUpCount)
    }
}
