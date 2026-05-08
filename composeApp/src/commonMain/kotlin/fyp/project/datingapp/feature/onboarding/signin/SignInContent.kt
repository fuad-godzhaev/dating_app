package fyp.project.datingapp.feature.onboarding.signin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.components.aura.Avatar
import fyp.project.datingapp.ui.components.aura.AuraLogo
import fyp.project.datingapp.ui.components.aura.PinDots
import fyp.project.datingapp.ui.components.aura.AuroraBackground
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun SignInContent(
    state: SignInComponent.State,
    onDigitEntered: (Char) -> Unit,
    onBackspace: () -> Unit,
    onForgotPin: () -> Unit,
) {
    val colors = AuraTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    AuroraBackground {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            AuraLogo(ringSize = 44.dp, orbSize = 18.dp, ringStroke = 2.dp, glow = false)
            Spacer(Modifier.height(6.dp))
            Text("aura", style = AuraTheme.text.heading22, color = colors.textPrimary)

            Spacer(Modifier.weight(0.5f))

            Avatar(
                size = 84.dp,
                ring = true,
                glow = true,
                initial = state.displayName?.take(1)?.uppercase(),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                if (state.displayName != null) "Welcome back, ${state.displayName}" else "Welcome back",
                style = AuraTheme.text.heading22,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text("Enter your PIN to unlock", style = AuraTheme.text.body16, color = colors.textSecondary)

            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                PinDots(filled = state.digitCount)
                BasicTextField(
                    value = state.enteredDigits,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }.take(SignInComponent.PIN_LENGTH)
                        val old = state.enteredDigits
                        when {
                            filtered.length > old.length ->
                                for (i in old.length until filtered.length) onDigitEntered(filtered[i])
                            filtered.length < old.length ->
                                repeat(old.length - filtered.length) { onBackspace() }
                        }
                    },
                    enabled = !state.isVerifying,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = AuraTheme.text.body16.copy(color = Color.Transparent),
                    modifier = Modifier.focusRequester(focusRequester).size(1.dp).alpha(0f),
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
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
                modifier = Modifier
                    .clickable(onClick = onForgotPin)
                    .padding(8.dp),
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
