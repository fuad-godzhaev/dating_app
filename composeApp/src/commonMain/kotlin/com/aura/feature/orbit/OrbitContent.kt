package com.aura.feature.orbit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.aura.ui.components.aura.AuraTopBar
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme
import com.aura.ui.theme.aura.auraDiagonalBrush

@Composable
fun OrbitContent(component: OrbitComponent) {
    val state by component.state.subscribeAsState()
    val colors = AuraTheme.colors

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Orbit", onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            val count = state.cards.size
            Text(
                if (count == 0) "No one yet - keep swiping" else "$count ${if (count == 1) "person likes" else "people like"} you - like back to match",
                style = AuraTheme.text.body16,
                color = colors.textSecondary,
            )
            Spacer(Modifier.size(16.dp))
            state.cards.chunked(2).forEach { rowCards ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowCards.forEach { card ->
                        Box(Modifier.weight(1f)) {
                            OrbitCard(card) { component.onLikeBack(card.did, card.name) }
                        }
                    }
                    if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun OrbitCard(card: OrbitComponent.Card, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(163f / 220f)
            .clip(RoundedCornerShape(AuraRadius.md))
            .background(auraDiagonalBrush(deepStart = true))
            .clickable(onClick = onClick),
    ) {
        // Bottom scrim for legibility.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bgBase.copy(alpha = 0.92f)))),
        )
        // Teal "liked you" heart badge.
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp).clip(CircleShape).background(colors.accentTeal),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) {
                val w = size.width
                val h = size.height
                val heart = Path().apply {
                    moveTo(w * 0.5f, h * 0.86f)
                    cubicTo(w * 0.02f, h * 0.54f, w * 0.16f, h * 0.06f, w * 0.5f, h * 0.34f)
                    cubicTo(w * 0.84f, h * 0.06f, w * 0.98f, h * 0.54f, w * 0.5f, h * 0.86f)
                    close()
                }
                drawPath(heart, colors.accentOnTeal)
            }
        }
        // Name / age.
        Row(
            Modifier.align(Alignment.BottomStart).padding(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(card.name ?: "Someone nearby", style = AuraTheme.text.body16, color = colors.textOnImage)
            if (card.age != null) {
                Text(", ${card.age}", style = AuraTheme.text.body16, color = colors.textOnImage)
            }
        }
    }
}
