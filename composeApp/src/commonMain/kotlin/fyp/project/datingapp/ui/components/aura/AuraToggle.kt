package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.theme.aura.AuraRadius
import fyp.project.datingapp.ui.theme.aura.AuraTheme

/** On/off toggle (AURA_DESIGN_SPEC §5.9). On = teal track + right knob; off = raised + left knob. */
@Composable
fun AuraToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.full)
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(colors.accentTeal)
                else Modifier.background(colors.bgSurfaceRaised).border(1.dp, colors.borderStrong, shape),
            )
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .background(if (checked) colors.accentOnTeal else colors.textSecondary, CircleShape),
        )
    }
}
