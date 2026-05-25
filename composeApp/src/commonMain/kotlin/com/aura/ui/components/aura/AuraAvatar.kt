package com.aura.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aura.ui.theme.aura.AuraTheme
import com.aura.ui.theme.aura.auraDiagonalBrush
import com.aura.ui.theme.aura.auraGlow

/**
 * Avatar (AURA_DESIGN_SPEC §5.6): an aurora-gradient circle. When [photoPath] is set it shows
 * the real photo (clipped to the circle); otherwise it falls back to [initial] over the gradient
 * placeholder. Optional violet-bright [ring] and soft [glow]. Sizes used: 36/40/54/64/84/104/120.
 */
@Composable
fun Avatar(
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = false,
    glow: Boolean = false,
    initial: String? = null,
    photoPath: String? = null,
    content: @Composable () -> Unit = {},
) {
    val colors = AuraTheme.colors
    val brush = auraDiagonalBrush(deepStart = true)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (glow) {
            Box(Modifier.fillMaxSize().auraGlow(colors.accentViolet, alpha = 0.45f, radiusFraction = 0.85f))
        }
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(brush)
                .then(if (ring) Modifier.border(2.dp, colors.accentVioletBright, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = "file://$photoPath",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else if (initial != null) {
                Text(initial, style = AuraTheme.text.title28, color = colors.textOnAccent)
            }
            content()
        }
    }
}

/** Teal verified badge with a drawn check (AURA_DESIGN_SPEC §5.7). */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    val colors = AuraTheme.colors
    Box(modifier.size(size).background(colors.stateVerified, CircleShape)) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val stroke = s * 0.10f
            drawLine(colors.accentOnTeal, Offset(s * 0.30f, s * 0.52f), Offset(s * 0.44f, s * 0.66f), stroke, StrokeCap.Round)
            drawLine(colors.accentOnTeal, Offset(s * 0.44f, s * 0.66f), Offset(s * 0.70f, s * 0.36f), stroke, StrokeCap.Round)
        }
    }
}
