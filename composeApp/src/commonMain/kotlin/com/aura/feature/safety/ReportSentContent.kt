package com.aura.feature.safety

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.ui.components.aura.AuroraBackground
import com.aura.ui.components.aura.PrimaryButton
import com.aura.ui.theme.aura.AuraTheme

@Composable
fun ReportSentContent(component: ReportSentComponent) {
    val colors = AuraTheme.colors
    AuroraBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(colors.accentTeal),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(44.dp)) {
                    val s = this.size.minDimension
                    val st = s * 0.10f
                    drawLine(colors.accentOnTeal, Offset(s * 0.28f, s * 0.52f), Offset(s * 0.44f, s * 0.68f), st, StrokeCap.Round)
                    drawLine(colors.accentOnTeal, Offset(s * 0.44f, s * 0.68f), Offset(s * 0.74f, s * 0.34f), st, StrokeCap.Round)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Report received", style = AuraTheme.text.title26, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Thanks for helping keep aura safe. Our community reviews every report, and you can block anyone at any time.",
                style = AuraTheme.text.body16,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton("Done", onClick = component::onDone)
            Spacer(Modifier.height(16.dp))
        }
    }
}
