package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.theme.aura.AuraRadius
import fyp.project.datingapp.ui.theme.aura.AuraTheme

/** Grouped settings/info card (AURA_DESIGN_SPEC §5.8): rows on a surface card. */
@Composable
fun AuraCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.md)
    Column(
        modifier = modifier.clip(shape).background(colors.bgSurface).border(1.dp, colors.borderHairline, shape),
        content = content,
    )
}

/** A row inside an [AuraCard] (AURA_DESIGN_SPEC §5.8). */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    subtitle: String? = null,
    titleColor: Color = AuraTheme.colors.textPrimary,
    titleStyle: TextStyle = AuraTheme.text.body16,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { Chevron() },
    onClick: (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = titleStyle, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = AuraTheme.text.caption13, color = colors.textSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}

/** Hairline divider between rows, inset to match the row padding. */
@Composable
fun HairlineDivider(inset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(1.dp)
            .background(AuraTheme.colors.borderHairline),
    )
}

/** Right-pointing chevron (AURA_DESIGN_SPEC §5.8 trailing). */
@Composable
fun Chevron(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = AuraTheme.colors.textTertiary,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val st = s * 0.12f
        drawLine(color, Offset(s * 0.40f, s * 0.28f), Offset(s * 0.64f, s * 0.50f), st, StrokeCap.Round)
        drawLine(color, Offset(s * 0.64f, s * 0.50f), Offset(s * 0.40f, s * 0.72f), st, StrokeCap.Round)
    }
}
