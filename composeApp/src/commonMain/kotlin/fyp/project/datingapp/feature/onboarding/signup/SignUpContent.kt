package fyp.project.datingapp.feature.onboarding.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fyp.project.datingapp.ui.components.aura.AddChip
import fyp.project.datingapp.ui.components.aura.AuraChip
import fyp.project.datingapp.ui.components.aura.AuraLogo
import fyp.project.datingapp.ui.components.aura.AuraStepper
import fyp.project.datingapp.ui.components.aura.AuraTextField
import fyp.project.datingapp.ui.components.aura.AuraTopBar
import fyp.project.datingapp.ui.components.aura.AuraWordmark
import fyp.project.datingapp.ui.components.aura.AuroraBackground
import fyp.project.datingapp.ui.components.aura.PinDots
import fyp.project.datingapp.ui.components.aura.PrimaryButton
import fyp.project.datingapp.ui.components.aura.SecondaryButton
import fyp.project.datingapp.ui.theme.aura.AuraTheme

@Composable
fun SignUpContent(
    state: SignUpComponent.State,
    onCreateNewAccount: () -> Unit,
    onRestoreExistingAccount: () -> Unit,
    onSeedPhraseWrittenDown: () -> Unit,
    onSeedWordChanged: (Int, String) -> Unit,
    onConfirmSeedPhrase: () -> Unit,
    onPinDigitEntered: (Char) -> Unit,
    onPinBackspace: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onAgeChanged: (Int) -> Unit,
    onInterestsChanged: (List<String>) -> Unit,
    onCreateProfile: () -> Unit,
    onBack: () -> Unit,
) {
    when (state.step) {
        SignUpComponent.Step.Welcome -> WelcomeStep(state, onCreateNewAccount, onRestoreExistingAccount)
        SignUpComponent.Step.GeneratingIdentity -> LoadingStep("Generating your identity...")
        SignUpComponent.Step.ShowSeedPhrase -> ShowSeedPhraseStep(state, onSeedPhraseWrittenDown, onBack)
        SignUpComponent.Step.EnterSeedPhrase -> EnterSeedPhraseStep(state, onSeedWordChanged, onConfirmSeedPhrase, onBack)
        SignUpComponent.Step.SetPin -> SetPinStep(state, onPinDigitEntered, onPinBackspace, onBack)
        SignUpComponent.Step.CreateProfile -> CreateProfileStep(
            state, onDisplayNameChanged, onBioChanged, onAgeChanged, onInterestsChanged, onCreateProfile, onBack,
        )
    }
}

@Composable
private fun WelcomeStep(
    state: SignUpComponent.State,
    onCreateNewAccount: () -> Unit,
    onRestoreExistingAccount: () -> Unit,
) {
    val colors = AuraTheme.colors
    AuroraBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            AuraLogo()
            Spacer(Modifier.height(32.dp))
            AuraWordmark()
            Spacer(Modifier.height(16.dp))
            Text("Connection, in your own light.", style = AuraTheme.text.body16, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            if (state.error != null) {
                Text(state.error, style = AuraTheme.text.caption13, color = colors.statePassStrong, modifier = Modifier.padding(bottom = 12.dp))
            }
            PrimaryButton("Create new account", onClick = onCreateNewAccount, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Restore existing account", onClick = onRestoreExistingAccount, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("Decentralized · No servers · Your keys", style = AuraTheme.text.caption13, color = colors.textTertiary)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LoadingStep(message: String) {
    val colors = AuraTheme.colors
    AuroraBackground {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AuraLogo()
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = colors.accentTeal)
            Spacer(Modifier.height(16.dp))
            Text(message, style = AuraTheme.text.body16, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ShowSeedPhraseStep(
    state: SignUpComponent.State,
    onWrittenDown: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AuraTheme.colors
    val words = state.generatedSeedPhrase?.words ?: emptyList()
    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(onBack = onBack, trailing = { Text("2 / 4", style = AuraTheme.text.label14, color = colors.textSecondary) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text("Your recovery phrase", style = AuraTheme.text.heading22, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Write these 12 words down and keep them safe. They restore your account.", style = AuraTheme.text.body16, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.bgSurface).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 4.dp, height = 20.dp).background(colors.statePass))
                    Spacer(Modifier.size(12.dp))
                    Text("Never share these words with anyone.", style = AuraTheme.text.label14, color = colors.statePassStrong)
                }
            }
            Spacer(Modifier.height(20.dp))
            for (rowStart in words.indices step 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0..1) {
                        val i = rowStart + col
                        if (i < words.size) WordChip(i + 1, words[i], Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(24.dp)) {
            PrimaryButton("I've written it down", onClick = onWrittenDown, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun WordChip(index: Int, word: String, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(colors.bgSurface).padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$index", style = AuraTheme.text.caption13, color = colors.textTertiary)
        Text(word, style = AuraTheme.text.body16, color = colors.textPrimary)
    }
}

@Composable
private fun EnterSeedPhraseStep(
    state: SignUpComponent.State,
    onSeedWordChanged: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AuraTheme.colors
    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Restore account", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text("Enter your 12-word recovery phrase", style = AuraTheme.text.body16, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            for (i in 0 until 12) {
                AuraTextField(
                    value = state.enteredSeedWords.getOrElse(i) { "" },
                    onValueChange = { onSeedWordChanged(i, it) },
                    placeholder = "word ${i + 1}",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.error != null) {
                Text(state.error, style = AuraTheme.text.caption13, color = colors.statePassStrong)
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(24.dp)) {
            PrimaryButton("Restore", onClick = onConfirm, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SetPinStep(
    state: SignUpComponent.State,
    onPinDigitEntered: (Char) -> Unit,
    onPinBackspace: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AuraTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val entered = if (state.isConfirmingPin) state.pinConfirm else state.pin

    AuroraBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuraTopBar(onBack = onBack)
            Spacer(Modifier.weight(1f))
            Text(if (state.isConfirmingPin) "Confirm your PIN" else "Set your PIN", style = AuraTheme.text.heading22, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("This PIN unlocks your identity on this device.", style = AuraTheme.text.caption13, color = colors.textTertiary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                PinDots(filled = state.pinDigitCount)
                BasicTextField(
                    value = entered,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }.take(4)
                        when {
                            filtered.length > entered.length -> for (i in entered.length until filtered.length) onPinDigitEntered(filtered[i])
                            filtered.length < entered.length -> repeat(entered.length - filtered.length) { onPinBackspace() }
                        }
                    },
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = AuraTheme.text.body16.copy(color = Color.Transparent),
                    modifier = Modifier.focusRequester(focusRequester).size(1.dp).alpha(0f),
                )
            }
            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
                Text(state.error, style = AuraTheme.text.caption13, color = colors.statePassStrong, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CreateProfileStep(
    state: SignUpComponent.State,
    onDisplayNameChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onAgeChanged: (Int) -> Unit,
    onInterestsChanged: (List<String>) -> Unit,
    onCreateProfile: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AuraTheme.colors
    Column(Modifier.fillMaxSize().background(colors.bgBase).systemBarsPadding()) {
        AuraTopBar(title = "Create your profile", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(20.dp)).background(colors.bgSurface),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Add photos", style = AuraTheme.text.label14, color = colors.accentVioletBright)
            }
            AuraTextField(state.displayName, onDisplayNameChanged, label = "Display name", placeholder = "Your name")
            AuraTextField(state.bio, onBioChanged, label = "Bio", placeholder = "A little about you", singleLine = false, minHeight = 96.dp)
            AuraStepper("Age", state.age, onDecrement = { onAgeChanged((state.age - 1).coerceAtLeast(18)) }, onIncrement = { onAgeChanged(state.age + 1) })
            InterestsEditor(state.interests, onInterestsChanged)
            if (state.error != null) {
                Text(state.error, style = AuraTheme.text.caption13, color = colors.statePassStrong)
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.padding(24.dp)) {
            PrimaryButton("Create profile", onClick = onCreateProfile, enabled = state.canCreateProfile && !state.isLoading, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InterestsEditor(interests: List<String>, onChange: (List<String>) -> Unit) {
    val colors = AuraTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Interests", style = AuraTheme.text.label14, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            interests.forEach { interest ->
                AuraChip(interest, onRemove = { onChange(interests - interest) })
            }
            AddChip(onClick = { onChange(interests + "new") })
        }
    }
}
