package com.aura.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aura.ui.components.aura.AuraTopBar
import com.aura.ui.theme.aura.AuraRadius
import com.aura.ui.theme.aura.AuraTheme

@Composable
fun RecoveryPhraseContent(component: RecoveryPhraseComponent) {
    val colors = AuraTheme.colors
    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Recovery phrase", onBack = component::onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Your recovery phrase is your account.",
                style = AuraTheme.text.heading22,
                color = colors.textPrimary,
            )
            Text(
                "The 12-word phrase was shown once, when you created your account. For your security it is " +
                    "derived one way and is not stored on this device, so it cannot be displayed again here.",
                style = AuraTheme.text.body16,
                color = colors.textSecondary,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AuraRadius.md))
                    .background(colors.bgSurface)
                    .border(1.dp, colors.borderHairline, RoundedCornerShape(AuraRadius.md))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Keep it safe", style = AuraTheme.text.label14, color = colors.textPrimary)
                Text(
                    "If you wrote it down, store it offline and never share it - anyone with the phrase can " +
                        "restore your identity. If you have lost it, you can keep using aura on this device, but " +
                        "you will not be able to recover this identity on a new device.",
                    style = AuraTheme.text.caption13,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
