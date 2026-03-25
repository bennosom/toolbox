package io.engst.launcher.ui.grid

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val PULSE_DURATION_MS = 800
private val INDICATOR_STROKE = 6.dp
private val INDICATOR_CORNER_RADIUS = 18.dp

/**
 * White pulsing rounded square shown on a cell while a drag shadow hovers over it.
 */
@Composable
fun DropIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dropPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dropPulseAlpha",
    )
    val scale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dropPulseScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .scale(scale)
                .border(
                    width = INDICATOR_STROKE,
                    color = Color.White,
                    shape = RoundedCornerShape(INDICATOR_CORNER_RADIUS),
                ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333, widthDp = 80, heightDp = 80)
@Composable
private fun DropIndicatorPreview() {
    DropIndicator(modifier = Modifier.size(80.dp))
}
