package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.theme.aura.AuraRadius
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraBrush

/**
 * Primary CTA: full-width aurora-gradient pill (AURA_DESIGN_SPEC §5.1). Disabled drops to a
 * raised-surface fill with tertiary text. [height]/[modifier] cover the smaller inline
 * variant (e.g. profile "Edit profile" 220x46).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.pill)
    val fill = if (enabled) auraBrush(horizontal = true) else SolidColor(colors.bgSurfaceRaised)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.text.button17,
            color = if (enabled) colors.textOnAccent else colors.textTertiary,
        )
    }
}

/** Secondary CTA: transparent outline pill (AURA_DESIGN_SPEC §5.2). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.pill)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .border(1.5.dp, colors.borderStrong, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AuraTheme.text.button17, color = colors.textPrimary)
    }
}
