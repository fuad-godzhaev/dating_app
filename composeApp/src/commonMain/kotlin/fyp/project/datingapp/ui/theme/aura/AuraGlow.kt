package fyp.project.datingapp.ui.theme.aura

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Aurora glows (AURA_DESIGN_SPEC §3.3): large, soft accent washes behind content. Rather
 * than a real `Modifier.blur` (which would blur the element's own content and has uneven
 * platform behaviour), we draw a radial gradient that fades to transparent - visually a
 * soft glow, cheap, and identical on Android + iOS/Skia.
 *
 * @param color the accent colour (violet/teal).
 * @param alpha centre opacity (spec uses 0.3-0.55 for washes, ~0.35 for tight glows).
 * @param radiusFraction glow radius as a fraction of the element's max dimension.
 * @param center glow centre as a fraction of size (0.5,0.5 = middle).
 */
fun Modifier.auraGlow(
    color: Color,
    alpha: Float = 0.4f,
    radiusFraction: Float = 0.9f,
    center: Offset = Offset(0.5f, 0.5f),
): Modifier = this.drawBehind {
    val c = Offset(size.width * center.x, size.height * center.y)
    val r = size.maxDimension * radiusFraction
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
}
