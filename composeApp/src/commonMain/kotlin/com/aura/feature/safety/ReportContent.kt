package com.aura.feature.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aura.ui.components.aura.AuraTextField
import com.aura.ui.components.aura.AuraToggle
import com.aura.ui.components.aura.AuraTopBar
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme

private val REPORT_REASONS = listOf(
    "Inappropriate or explicit photos",
    "Harassment or bullying",
    "Spam or scam",
    "Fake profile or impersonation",
    "Underage user",
    "Something else",
)

@Composable
fun ReportContent(component: ReportComponent) {
    val colors = AuraTheme.colors
    var selected by remember { mutableStateOf<Int?>(null) }
    var details by remember { mutableStateOf("") }
    var alsoBlock by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Report ${component.targetName}", onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Privacy reassurance card with a teal left accent bar.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(AuraRadius.md))
                    .background(colors.bgSurface)
                    .border(1.dp, colors.borderHairline, RoundedCornerShape(AuraRadius.md)),
            ) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(colors.accentTeal))
                Text(
                    "Your report is private. ${component.targetName} won't be notified.",
                    style = AuraTheme.text.caption13,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(14.dp),
                )
            }

            Text("Why are you reporting?", style = AuraTheme.text.label14, color = colors.textSecondary)
            Column {
                REPORT_REASONS.forEachIndexed { i, reason ->
                    ReasonRow(reason, selected == i) { selected = i }
                }
            }

            AuraTextField(
                value = details,
                onValueChange = { details = it },
                label = "Add details (optional)",
                placeholder = "What happened?",
                singleLine = false,
                minHeight = 96.dp,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Also block ${component.targetName}", style = AuraTheme.text.body16, color = colors.textPrimary)
                    Text("You won't see each other again", style = AuraTheme.text.caption13, color = colors.textSecondary)
                }
                AuraToggle(alsoBlock, { alsoBlock = it })
            }

            CoralSubmitButton("Submit report", enabled = selected != null, onClick = component::onSubmit)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReasonRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(vertical = 11.dp)
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.accentTeal else colors.borderStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(colors.accentTeal, CircleShape))
        }
        Text(
            text,
            style = AuraTheme.text.body16,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CoralSubmitButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(AuraRadius.pill)
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) colors.statePassStrong else colors.bgSurfaceRaised)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AuraTheme.text.button17,
            color = if (enabled) colors.textOnAccent else colors.textTertiary,
        )
    }
}
