package fyp.project.datingapp.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.theme.aura.AuraTheme
import fyp.project.datingapp.ui.theme.aura.auraGlow
import fyp.project.datingapp.ui.theme.aura.tealButtonBrush

/**
 * Home / expanded-card action pair (AURA_DESIGN_SPEC §5.10): Pass (coral outline X) and
 * Like (teal gradient heart with glow). There is deliberately NO super-like.
 */
@Composable
fun ActionButtonsRow(
    onPass: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    size: Dp = 64.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PassButton(onPass, size = size)
        LikeButton(onLike, size = size)
    }
}

@Composable
fun PassButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val colors = AuraTheme.colors
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.bgSurface.copy(alpha = 0.65f))
            .border(1.5.dp, colors.statePass, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.38f)) {
            val s = this.size.minDimension
            val st = s * 0.16f
            drawLine(colors.statePass, Offset(s * 0.2f, s * 0.2f), Offset(s * 0.8f, s * 0.8f), st, StrokeCap.Round)
            drawLine(colors.statePass, Offset(s * 0.8f, s * 0.2f), Offset(s * 0.2f, s * 0.8f), st, StrokeCap.Round)
        }
    }
}

@Composable
fun LikeButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val colors = AuraTheme.colors
    Box(modifier.size(size).auraGlow(colors.accentTeal, alpha = 0.35f, radiusFraction = 0.75f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(tealButtonBrush())
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(size * 0.42f)) {
                val w = this.size.width
                val h = this.size.height
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.86f)
                    cubicTo(w * 0.04f, h * 0.55f, w * 0.18f, h * 0.10f, w * 0.5f, h * 0.32f)
                    cubicTo(w * 0.82f, h * 0.10f, w * 0.96f, h * 0.55f, w * 0.5f, h * 0.86f)
                    close()
                }
                drawPath(path, colors.accentOnTeal)
            }
        }
    }
}
