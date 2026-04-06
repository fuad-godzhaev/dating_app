package fyp.project.datingapp.feature.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.components.withLinearGradient

/**
 * Circular icon button with a two-colour gradient border and matching
 * gradient-tinted icon. Used for the ❌ / ❤ buttons under the swipe
 * stack. When disabled the border and icon fade in lockstep —
 * `IconButton(enabled = false)` handles the icon via `LocalContentColor`;
 * we fade the border brush manually.
 */
@Composable
fun RoundGradientButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    color1: Color,
    color2: Color,
    enabled: Boolean = true,
) {
    val borderAlpha = if (enabled) 1f else 0.38f
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(color1.copy(alpha = borderAlpha), color2.copy(alpha = borderAlpha)),
                    ),
                    shape = CircleShape,
                )
                .padding(12.dp)
                .size(32.dp)
                .withLinearGradient(color1, color2),
        )
    }
}
