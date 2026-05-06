package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraGlow
import fyp.project.datingapp.ui.theme.aura.auraBrush

/**
 * Aura logo mark (AURA_DESIGN_SPEC §6.1): an aurora-gradient orb inside an aurora-gradient
 * ring, with an optional blurred violet halo behind.
 */
@Composable
fun AuraLogo(
    modifier: Modifier = Modifier,
    ringSize: Dp = 104.dp,
    orbSize: Dp = 44.dp,
    ringStroke: Dp = 3.dp,
    glow: Boolean = true,
) {
    val brush = auraBrush(horizontal = true)
    Box(
        modifier = modifier.size(ringSize).then(
            if (glow) Modifier.auraGlow(AuraTheme.colors.accentViolet, alpha = 0.4f, radiusFraction = 0.7f) else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(ringSize).border(ringStroke, brush, CircleShape))
        Box(Modifier.size(orbSize).background(brush, CircleShape))
    }
}

/** The lowercase "aura" wordmark (AURA_DESIGN_SPEC §3.4 / §6.1). */
@Composable
fun AuraWordmark(modifier: Modifier = Modifier) {
    Text(
        text = "aura",
        style = AuraTheme.text.wordmark,
        color = AuraTheme.colors.textPrimary,
        modifier = modifier,
    )
}
