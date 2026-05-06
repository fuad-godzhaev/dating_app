package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import fyp.project.datingapp.ui.theme.aura.AuraTheme

/**
 * Reusable immersive background (AURA_DESIGN_SPEC §5.15): the base ink colour with a few
 * large, soft violet/teal washes near the top/hero. Place screen content inside.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AuraTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .drawBehind {
                fun wash(cx: Float, cy: Float, color: androidx.compose.ui.graphics.Color, alpha: Float, rFrac: Float) {
                    val center = Offset(size.width * cx, size.height * cy)
                    val r = size.minDimension * rFrac
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                            center = center,
                            radius = r,
                        ),
                        radius = r,
                        center = center,
                    )
                }
                wash(0.12f, 0.14f, colors.accentViolet, 0.45f, 0.85f)  // top-left violet
                wash(0.92f, 0.10f, colors.accentTeal, 0.38f, 0.70f)    // top-right teal
                wash(0.50f, 0.42f, colors.accentViolet, 0.18f, 0.75f)  // soft mid violet
            },
    ) {
        content()
    }
}
