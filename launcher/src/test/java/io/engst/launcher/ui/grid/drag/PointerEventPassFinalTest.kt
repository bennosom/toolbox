package io.engst.launcher.ui.grid.drag

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that [awaitLongPressOrSwipeOrTap] detects a long press when using
 * [PointerEventPass.Main] explicitly. Exercises the `eventPass` parameter path.
 */
@RunWith(RobolectricTestRunner::class)
class PointerEventPassFinalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun Main_pass_detects_long_press() {
        val log = mutableListOf<String>()

        composeTestRule.setContent {
            val vc = LocalViewConfiguration.current
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("box")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            log += "down"
                            val result = awaitLongPressOrSwipeOrTap(down, vc, PointerEventPass.Main)
                            log += "result: $result"
                        }
                    },
            )
        }

        composeTestRule.onNodeWithTag("box").performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        assertTrue(
            "Should detect LongPress. Log:\n${log.joinToString("\n")}",
            log.any { it.contains("LongPress") },
        )
    }
}
