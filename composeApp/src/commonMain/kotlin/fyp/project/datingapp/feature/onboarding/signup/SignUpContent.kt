package fyp.project.datingapp.feature.onboarding.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyp.project.datingapp.ui.components.AnimatedLogo
import fyp.project.datingapp.ui.theme.Orange
import fyp.project.datingapp.ui.theme.Pink
import fyp.project.datingapp.ui.theme.TinderCloneComposeTheme

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
        SignUpComponent.Step.GeneratingIdentity -> GeneratingStep()
        SignUpComponent.Step.ShowSeedPhrase -> ShowSeedPhraseStep(state, onSeedPhraseWrittenDown, onBack)
        SignUpComponent.Step.EnterSeedPhrase -> EnterSeedPhraseStep(state, onSeedWordChanged, onConfirmSeedPhrase, onBack)
        SignUpComponent.Step.SetPin -> SetPinStep(state, onPinDigitEntered, onPinBackspace, onBack)
        SignUpComponent.Step.CreateProfile -> CreateProfileStep(state, onDisplayNameChanged, onBioChanged, onAgeChanged, onInterestsChanged, onCreateProfile)
    }
}

@Composable
private fun GradientBackground(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.horizontalGradient(listOf(Pink, Orange)))
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun WelcomeStep(
    state: SignUpComponent.State,
    onCreateNewAccount: () -> Unit,
    onRestoreExistingAccount: () -> Unit,
) {
    GradientBackground {
        Spacer(modifier = Modifier.weight(1f))
        AnimatedLogo(modifier = Modifier.fillMaxWidth(.4f).padding(bottom = 8.dp), isAnimating = state.isLoading)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            if (state.error != null) {
                Text(
                    text = state.error,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)
                )
            }
            OutlinedButton(
                onClick = onCreateNewAccount,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Create New Account", modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRestoreExistingAccount,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Restore Existing Account", modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

@Composable
private fun GeneratingStep() {
    GradientBackground {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun ShowSeedPhraseStep(
    state: SignUpComponent.State,
    onSeedPhraseWrittenDown: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(24.dp)) {
            Text("Your Recovery Phrase", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Text("Write these 12 words down in order. You'll need them to restore your account.", modifier = Modifier.padding(bottom = 24.dp))
            val words = state.generatedSeedPhrase?.words ?: emptyList()
            Column(modifier = Modifier.weight(1f)) {
                words.chunked(2).forEachIndexed { rowIndex, rowWords ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        rowWords.forEachIndexed { colIndex, word ->
                            Text(
                                text = "${rowIndex * 2 + colIndex + 1}. $word",
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            Button(onClick = onSeedPhraseWrittenDown, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Text("I've Written It Down")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun EnterSeedPhraseStep(
    state: SignUpComponent.State,
    onSeedWordChanged: (Int, String) -> Unit,
    onConfirmSeedPhrase: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 24.dp)) {
            item {
                Text("Enter Recovery Phrase", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
            }
            item {
                repeat(12) { index ->
                    OutlinedTextField(
                        value = state.enteredSeedWords[index],
                        onValueChange = { onSeedWordChanged(index, it) },
                        label = { Text("Word ${index + 1}") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true
                    )
                }
            }
            item {
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = onConfirmSeedPhrase,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Confirm")
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
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
    GradientBackground {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (state.isConfirmingPin) "Confirm PIN" else "Set PIN",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape).background(
                            if (index < state.pinDigitCount) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            if (state.error != null) {
                Text(
                    text = state.error,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)
                )
            }
            listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9')).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { digit ->
                        TextButton(onClick = { onPinDigitEntered(digit) }, enabled = !state.isLoading, modifier = Modifier.size(72.dp)) {
                            Text(digit.toString(), color = Color.White, fontSize = 24.sp)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Spacer(modifier = Modifier.size(72.dp))
                TextButton(onClick = { onPinDigitEntered('0') }, enabled = !state.isLoading, modifier = Modifier.size(72.dp)) {
                    Text("0", color = Color.White, fontSize = 24.sp)
                }
                IconButton(onClick = onPinBackspace, modifier = Modifier.size(72.dp)) {
                    Text("⌫", color = Color.White, fontSize = 20.sp)
                }
            }
            TextButton(onClick = onBack) {
                Text("Back", color = Color.White)
            }
            Spacer(modifier = Modifier.height(44.dp))
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
) {
    var interestsText by remember(state.interests) { mutableStateOf(state.interests.joinToString(", ")) }
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 24.dp)) {
            item {
                Text("Create Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChanged,
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.bio,
                    onValueChange = onBioChanged,
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Age", modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (state.age > 18) onAgeChanged(state.age - 1) }) {
                        Text("−", fontSize = 20.sp)
                    }
                    Text(state.age.toString(), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { onAgeChanged(state.age + 1) }) {
                        Text("+", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = interestsText,
                    onValueChange = {
                        interestsText = it
                        onInterestsChanged(it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })
                    },
                    label = { Text("Interests (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = onCreateProfile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canCreateProfile && !state.isLoading
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Create Profile")
                }
                Spacer(modifier = Modifier.height(44.dp))
            }
        }
    }
}

@Preview
@Composable
fun SignUpContentWelcomePreview() {
    TinderCloneComposeTheme {
        SignUpContent(
            state = SignUpComponent.State(),
            onCreateNewAccount = {},
            onRestoreExistingAccount = {},
            onSeedPhraseWrittenDown = {},
            onSeedWordChanged = { _, _ -> },
            onConfirmSeedPhrase = {},
            onPinDigitEntered = {},
            onPinBackspace = {},
            onDisplayNameChanged = {},
            onBioChanged = {},
            onAgeChanged = {},
            onInterestsChanged = {},
            onCreateProfile = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
fun SignUpContentCreateProfilePreview() {
    TinderCloneComposeTheme {
        SignUpContent(
            state = SignUpComponent.State(step = SignUpComponent.Step.CreateProfile, displayName = "Alice", age = 23),
            onCreateNewAccount = {},
            onRestoreExistingAccount = {},
            onSeedPhraseWrittenDown = {},
            onSeedWordChanged = { _, _ -> },
            onConfirmSeedPhrase = {},
            onPinDigitEntered = {},
            onPinBackspace = {},
            onDisplayNameChanged = {},
            onBioChanged = {},
            onAgeChanged = {},
            onInterestsChanged = {},
            onCreateProfile = {},
            onBack = {},
        )
    }
}
