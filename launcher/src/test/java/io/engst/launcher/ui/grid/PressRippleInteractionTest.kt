@file:Suppress("DEPRECATION")

package io.engst.launcher.ui.grid

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.grid.drag.draggableAppSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that pressing an app tile emits a [PressInteraction.Press] on the
 * [MutableInteractionSource] (which drives the ripple effect), and that releasing
 * emits [PressInteraction.Release].
 *
 * This mirrors the interaction wiring in AppTileContainer.
 */
@RunWith(RobolectricTestRunner::class)
class PressRippleInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testApp = App(
        id = "ripple-test",
        label = "RippleTest",
        icon = ColorDrawable(),
        componentName = ComponentName("pkg.ripple", "cls.ripple"),
        launchIntent = Intent(),
        shortcuts = emptyList(),
        versionName = "1.0",
        versionCode = 1L,
        minSdk = 31,
        targetSdk = 36,
        lastUpdatedTimeMillis = 0L,
        installedTimeMillis = 0L,
        isSystemApp = false,
    )

    private val interactions = MutableStateFlow<List<Any>>(emptyList())

    @Test
    fun press_emits_PressInteraction_Press() {
        val capturedInteractions = mutableListOf<Any>()
        composeTestRule.setContent {
            val density = LocalDensity.current
            val viewConfiguration = LocalViewConfiguration.current
            val interactionSource = remember { MutableInteractionSource() }
            val scope = rememberCoroutineScope()

            // Collect all interactions
            remember {
                scope.launch {
                    interactionSource.interactions.collect { interaction ->
                        capturedInteractions.add(interaction)
                        interactions.value = capturedInteractions.toList()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("tile")
                    .clip(MaterialTheme.shapes.small)
                    .indication(interactionSource, ripple())
                    .draggableAppSource(
                        app = testApp,
                        iconSizeDp = 60.dp,
                        density = density,
                        viewConfiguration = viewConfiguration,
                        onTap = {},
                        onLongPress = {},
                        onDragStarted = {},
                        onPressStarted = {
                            val press = PressInteraction.Press(
                                androidx.compose.ui.geometry.Offset.Zero,
                            )
                            scope.launch { interactionSource.emit(press) }
                        },
                        onGestureCompleted = {},
                    ),
            )
        }

        composeTestRule.onNodeWithTag("tile").performTouchInput {
            down(center)
        }
        composeTestRule.waitForIdle()

        // Wait briefly for async emission
        val result = runBlocking {
            withTimeoutOrNull(1000) {
                interactions.first { it.isNotEmpty() }
            }
        }

        assertNotNull("Interaction should have been emitted", result)
        assertTrue(
            "First interaction should be PressInteraction.Press",
            result!!.first() is PressInteraction.Press,
        )
    }

    @Test
    fun press_and_release_emits_PressInteraction_Release() {
        val capturedInteractions = mutableListOf<Any>()
        var pressInteraction: PressInteraction.Press? = null

        composeTestRule.setContent {
            val density = LocalDensity.current
            val viewConfiguration = LocalViewConfiguration.current
            val interactionSource = remember { MutableInteractionSource() }
            val scope = rememberCoroutineScope()

            remember {
                scope.launch {
                    interactionSource.interactions.collect { interaction ->
                        capturedInteractions.add(interaction)
                        interactions.value = capturedInteractions.toList()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("tile")
                    .clip(MaterialTheme.shapes.small)
                    .indication(interactionSource, ripple())
                    .draggableAppSource(
                        app = testApp,
                        iconSizeDp = 60.dp,
                        density = density,
                        viewConfiguration = viewConfiguration,
                        onTap = {},
                        onLongPress = {},
                        onDragStarted = {},
                        onPressStarted = {
                            val press = PressInteraction.Press(
                                androidx.compose.ui.geometry.Offset.Zero,
                            )
                            pressInteraction = press
                            scope.launch { interactionSource.emit(press) }
                        },
                        onGestureCompleted = {
                            pressInteraction?.let { press ->
                                scope.launch {
                                    interactionSource.emit(PressInteraction.Release(press))
                                }
                            }
                            pressInteraction = null
                        },
                    ),
            )
        }

        composeTestRule.onNodeWithTag("tile").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        val result = runBlocking {
            withTimeoutOrNull(1000) {
                interactions.first { list -> list.size >= 2 }
            }
        }

        assertNotNull("Both interactions should have been emitted", result)
        assertTrue(
            "First interaction should be Press",
            result!![0] is PressInteraction.Press,
        )
        assertTrue(
            "Second interaction should be Release",
            result[1] is PressInteraction.Release,
        )
    }
}
