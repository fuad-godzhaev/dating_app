package fyp.project.datingapp.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Tints the content of [this] with a linear gradient between [color1] and
 * [color2]. Works on any composable — the content is drawn first, then
 * over-painted with the gradient using [BlendMode.SrcAtop] so only the
 * opaque pixels of the content get tinted.
 *
 * The `graphicsLayer(alpha = 0.99f)` line forces an offscreen buffer so
 * the src-atop blend has a surface to operate on; without it, blend modes
 * that need the destination alpha silently misbehave on some backends.
 *
 * Ported from the old prototype's
 * `com.apiguave.core_ui.modifiers.Modifier.withLinearGradient`.
 */
fun Modifier.withLinearGradient(color1: Color, color2: Color): Modifier =
    this
        .graphicsLayer(alpha = 0.99f)
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.linearGradient(listOf(color1, color2)),
                    blendMode = BlendMode.SrcAtop,
                )
            }
        }
