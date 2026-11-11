package de.rezeptbuch.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.rezeptbuch.app.ui.theme.LocalDarkTheme

/** Leichte Rückmeldung beim Antippen: das Element federt minimal zusammen. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier =
    composed {
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "pressScale",
        )
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

/** Weicher, bläulicher Schatten wie im Entwurf; im dunklen Design dezenter. */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 10.dp): Modifier = composed {
    val dark = LocalDarkTheme.current
    val color = if (dark) Color.Black.copy(alpha = 0.5f) else Color(0xFF1E3A8A).copy(alpha = 0.10f)
    shadow(elevation = if (dark) elevation / 2 else elevation, shape = shape, ambientColor = color, spotColor = color)
}
