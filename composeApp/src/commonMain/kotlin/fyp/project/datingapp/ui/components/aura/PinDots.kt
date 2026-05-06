package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.theme.aura.AuraTheme

/**
 * Row of PIN dots (AURA_DESIGN_SPEC §5.14). [filled] dots are solid teal; the rest are an
 * outlined ring. The actual digits come from the platform keyboard via a BasicTextField;
 * this only reflects entry length.
 */
@Composable
fun PinDots(
    filled: Int,
    total: Int = 4,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val isFilled = i < filled
            Box(
                Modifier
                    .size(15.dp)
                    .then(
                        if (isFilled) {
                            Modifier.background(colors.accentTeal, CircleShape)
                        } else {
                            Modifier.background(Color.Transparent, CircleShape)
                                .border(1.5.dp, colors.borderStrong2, CircleShape)
                        },
                    ),
            )
        }
    }
}
