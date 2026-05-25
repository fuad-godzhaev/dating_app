package com.aura.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme

/**
 * Tag / chip (AURA_DESIGN_SPEC §5.3). Default = raised surface + strong border + violet
 * label. [overPhoto] = translucent white over imagery. [onRemove] adds the edit-profile
 * remove "x". [onClick] makes it tappable.
 */
@Composable
fun AuraChip(
    text: String,
    modifier: Modifier = Modifier,
    overPhoto: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.full)
    val bg = if (overPhoto) colors.textOnImage.copy(alpha = 0.16f) else colors.bgSurfaceRaised
    val labelColor = if (overPhoto) colors.textOnImage else colors.accentVioletBright
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(if (!overPhoto) Modifier.border(1.dp, colors.borderStrong, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = AuraTheme.text.caption13, color = labelColor)
        if (onRemove != null) {
            Canvas(Modifier.size(12.dp).clickable(onClick = onRemove)) {
                val s = size.minDimension
                val st = s * 0.14f
                drawLine(labelColor, Offset(s * 0.25f, s * 0.25f), Offset(s * 0.75f, s * 0.75f), st, StrokeCap.Round)
                drawLine(labelColor, Offset(s * 0.75f, s * 0.25f), Offset(s * 0.25f, s * 0.75f), st, StrokeCap.Round)
            }
        }
    }
}

/** Dashed "+ add" chip (AURA_DESIGN_SPEC §5.3 add variant). */
@Composable
fun AddChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "+ add",
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AuraRadius.full))
            .drawBehind {
                drawRoundRect(
                    color = colors.borderStrong2,
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = AuraTheme.text.caption13, color = colors.textTertiary)
    }
}
