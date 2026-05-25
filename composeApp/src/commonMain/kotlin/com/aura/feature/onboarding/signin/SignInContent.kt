package com.aura.feature.onboarding.signin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aura.ui.components.aura.Avatar
import com.aura.ui.components.aura.AuraLogo
import com.aura.ui.components.aura.AuroraBackground
import com.aura.ui.components.aura.PinEntry
import com.aura.ui.theme.aura.AuraTheme

@Composable
fun SignInContent(
    state: SignInComponent.State,
    onDigitEntered: (Char) -> Unit,
    onBackspace: () -> Unit,
    onForgotPin: () -> Unit,
) {
    val colors = AuraTheme.colors
    AuroraBackground {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top-anchored with fixed spacing (no vertical centering) so the PIN row sits in the
            // upper-middle, clear of even a tall keyboard, and lines up with the Set-PIN screen.
            Spacer(Modifier.height(24.dp))
            AuraLogo(ringSize = 44.dp, orbSize = 18.dp, ringStroke = 2.dp, glow = false)
            Spacer(Modifier.height(6.dp))
            Text("aura", style = AuraTheme.text.heading22, color = colors.textPrimary)

            Spacer(Modifier.height(36.dp))

            Avatar(
                size = 84.dp,
                ring = true,
                glow = true,
                initial = state.displayName?.take(1)?.uppercase(),
                photoPath = state.photoPath,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                if (state.displayName != null) "Welcome back, ${state.displayName}" else "Welcome back",
                style = AuraTheme.text.heading22,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text("Enter your PIN to unlock", style = AuraTheme.text.body16, color = colors.textSecondary)

            Spacer(Modifier.height(20.dp))
            PinEntry(
                value = state.enteredDigits,
                onDigit = onDigitEntered,
                onBackspace = onBackspace,
                length = SignInComponent.PIN_LENGTH,
                enabled = !state.isVerifying,
            )

            if (state.error != null) {
                Text(
                    state.error!!,
                    style = AuraTheme.text.caption13,
                    color = colors.statePassStrong,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                if (state.suggestRecovery) "Restore account with recovery phrase" else "Use recovery phrase instead",
                style = AuraTheme.text.label14,
                color = colors.accentTealBright,
                modifier = Modifier.clickable(onClick = onForgotPin).padding(8.dp),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
fun SignInContentPreview() {
    AuraTheme {
        SignInContent(
            state = SignInComponent.State(displayName = "Eva"),
            onDigitEntered = {},
            onBackspace = {},
            onForgotPin = {},
        )
    }
}
