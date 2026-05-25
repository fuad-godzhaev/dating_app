package com.aura.ui.components.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraTheme

/** Age stepper (AURA_DESIGN_SPEC §5.5): label, value, and two circular -/+ buttons. */
@Composable
fun AuraStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AuraTheme.text.body16, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StepButton("-", onDecrement)
            Text(value.toString(), style = AuraTheme.text.body16, color = colors.textPrimary)
            StepButton("+", onIncrement)
        }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .background(colors.bgSurfaceRaised, CircleShape)
            .border(1.dp, colors.borderHairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = AuraTheme.text.body16, color = colors.accentVioletBright)
    }
}
