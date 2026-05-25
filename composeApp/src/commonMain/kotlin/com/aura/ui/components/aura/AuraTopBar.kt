package com.aura.ui.components.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.aura.AuraTheme

/**
 * Top navigation bar (AURA_DESIGN_SPEC §4): optional back chevron (left), optional centered
 * title, optional trailing slot (e.g. settings gear / likes pill).
 */
@Composable
fun AuraTopBar(
    modifier: Modifier = Modifier.fillMaxWidth(),
    title: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(modifier.height(56.dp).padding(horizontal = 24.dp)) {
        if (onBack != null) {
            BackChevron(Modifier.align(Alignment.CenterStart).size(20.dp).clickable(onClick = onBack))
        }
        if (title != null) {
            Text(
                title,
                style = AuraTheme.text.button17,
                color = AuraTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

/** Left-pointing back chevron (AURA_DESIGN_SPEC §4). */
@Composable
fun BackChevron(
    modifier: Modifier = Modifier,
    color: Color = AuraTheme.colors.textPrimary,
) {
    Canvas(modifier.size(20.dp)) {
        val s = this.size.minDimension
        val st = s * 0.11f
        drawLine(color, Offset(s * 0.62f, s * 0.28f), Offset(s * 0.38f, s * 0.50f), st, StrokeCap.Round)
        drawLine(color, Offset(s * 0.38f, s * 0.50f), Offset(s * 0.62f, s * 0.72f), st, StrokeCap.Round)
    }
}
