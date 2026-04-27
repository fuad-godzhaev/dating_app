package fyp.project.datingapp.feature.onboarding.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fyp.project.datingapp.ui.components.AnimatedLogo
import fyp.project.datingapp.ui.theme.Orange
import fyp.project.datingapp.ui.theme.Pink
import fyp.project.datingapp.ui.theme.TinderCloneComposeTheme

@Composable
fun SignInContent(
    state: SignInComponent.State,
    onDigitEntered: (Char) -> Unit,
    onBackspace: () -> Unit,
    onForgotPin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.horizontalGradient(listOf(Pink, Orange)))
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.0f))
        AnimatedLogo(
            modifier = Modifier.fillMaxWidth(.4f).padding(bottom = 8.dp),
            isAnimating = state.isVerifying
        )
        Column(
            modifier = Modifier.weight(1.0f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(SignInComponent.PIN_LENGTH) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < state.digitCount) Color.White
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                )
            }

            listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
            ).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        TextButton(
                            onClick = { onDigitEntered(digit) },
                            enabled = !state.isVerifying,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text(digit.toString(), color = Color.White, fontSize = 24.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.size(72.dp))
                TextButton(
                    onClick = { onDigitEntered('0') },
                    enabled = !state.isVerifying,
                    modifier = Modifier.size(72.dp)
                ) {
                    Text("0", color = Color.White, fontSize = 24.sp)
                }
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier.size(72.dp)
                ) {
                    Text("⌫", color = Color.White, fontSize = 20.sp)
                }
            }

            TextButton(onClick = onForgotPin) {
                Text(
                    if (state.suggestRecovery) "Restore account with recovery phrase"
                    else "Forgot PIN?",
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

@Preview
@Composable
fun SignInContentPreview() {
    TinderCloneComposeTheme {
        SignInContent(
            state = SignInComponent.State(),
            onDigitEntered = {},
            onBackspace = {},
            onForgotPin = {},
        )
    }
}