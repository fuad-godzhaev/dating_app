package com.aura.ui.components.aura

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme
import com.aura.ui.theme.aura.auraBrush

/**
 * Primary CTA: full-width aurora-gradient pill (AURA_DESIGN_SPEC §5.1). Disabled drops to a
 * raised-surface fill with tertiary text. [height]/[modifier] cover the smaller inline
 * variant (e.g. profile "Edit profile" 220x46). Press scales + dims; keyboard focus shows a
 * teal-bright ring (accessibility, changelog §D3).
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, tween(120), label = "primaryScale")
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(fill)
            .then(if (focused) Modifier.border(2.dp, colors.accentTealBright, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (pressed && enabled) Box(Modifier.matchParentSize().background(colors.bgBase.copy(alpha = 0.12f)))
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, tween(120), label = "secondaryScale")
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .border(if (focused) 2.dp else 1.5.dp, if (focused) colors.accentTealBright else colors.borderStrong, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (pressed && enabled) Box(Modifier.matchParentSize().background(colors.textPrimary.copy(alpha = 0.08f)))
        Text(text = text, style = AuraTheme.text.button17, color = colors.textPrimary)
    }
}
