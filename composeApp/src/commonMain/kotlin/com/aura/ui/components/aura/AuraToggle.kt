package com.aura.ui.components.aura

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme

/**
 * On/off toggle (AURA_DESIGN_SPEC §5.9). On = teal track + right knob; off = raised + left knob.
 * The knob slides and the track colour cross-fades when toggled.
 */
@Composable
fun AuraToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.full)
    // Travel = track width 46 - padding 3*2 - knob 20 = 20dp.
    val knobOffset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(180), label = "toggleKnob")
    val trackColor by animateColorAsState(if (checked) colors.accentTeal else colors.bgSurfaceRaised, tween(180), label = "toggleTrack")
    val knobColor by animateColorAsState(if (checked) colors.accentOnTeal else colors.textSecondary, tween(180), label = "toggleKnobColor")
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(shape)
            .background(trackColor)
            .then(if (checked) Modifier else Modifier.border(1.dp, colors.borderStrong, shape))
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(20.dp)
                .background(knobColor, CircleShape),
        )
    }
}
